import os
import google.generativeai as genai
from fastapi import FastAPI, Depends, HTTPException, Request, status
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from dotenv import load_dotenv
from sqlmodel import Session, SQLModel, create_engine, select
import models
import json
import requests
from typing import Optional


# --- INITIALIZATION ---
load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

# --- UPDATED: Use the specific models you requested ---
text_model = genai.GenerativeModel("gemini-1.5-flash-latest")

HF_API_URL = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0"
HF_API_KEY = os.getenv("HUGGING_FACE_API_KEY")

app = FastAPI()

# --- STATIC FILES (Cover images) ---
COVERS_DIR = "covers"
os.makedirs(COVERS_DIR, exist_ok=True)
app.mount("/covers", StaticFiles(directory=COVERS_DIR), name="covers")

# --- DATABASE SETUP ---
DATABASE_URL = "sqlite:///database.db"
engine = create_engine(DATABASE_URL, echo=True)

def create_db_and_tables():
    SQLModel.metadata.create_all(engine)

@app.on_event("startup")
def on_startup():
    create_db_and_tables()

def get_session():
    with Session(engine) as session:
        yield session

# --- CONFIG VALIDATION ---

def require_hf_key():
    if not HF_API_KEY or not HF_API_KEY.strip():
        raise HTTPException(status_code=500, detail="Hugging Face API key not found. Set HUGGING_FACE_API_KEY in .env")

@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "has_gemini_key": bool(os.getenv("GEMINI_API_KEY")),
        "has_hf_key": bool(HF_API_KEY and HF_API_KEY.strip()),
        "hf_model": HF_API_URL.split("/models/")[-1],
    }

# --- DATA MODELS ---
class StoryRequest(BaseModel):
    ai_name: str = "Orion"
    genre: str
    story_context: str
    user_input: str

class StoryResponse(BaseModel):
    action: str
    story_text: str
    chat_response: str
    new_chapter_title: Optional[str] = None

class FullStoryCreate(BaseModel):
    title: str
    genre: str
    ai_name: str
    chapters: list[models.Chapter]

# --- API ENDPOINTS ---
@app.post("/api/continue_story", response_model=StoryResponse)
async def continue_story(request: StoryRequest):
    """
    Analyzes user input to continue, edit, refuse, chat, or start a new chapter.
    """
    prompt = f"""
    You are {request.ai_name}, a master storyteller and creative partner.
    You have five tasks: Writing, Editing, Refusing, Conversing, and Chaptering.

    Here is the story so far:
    ---
    {request.story_context}
    ---
    Here is the user's latest instruction:
    ---
    {request.user_input}
    ---

    Analyze the user's instruction and the story's progression.

    1.  If the story has reached a natural break point (a climax, a change in setting, or a significant time jump), start a new chapter.
        - Your response MUST be a JSON object with the following structure:
        {{
          "action": "APPEND",
          "story_text": "The first paragraph(s) of the new chapter.",
          "chat_response": "A brief, conversational reply...",
          "new_chapter_title": "A fitting title for the NEW chapter."
        }}

    2.  If it's a regular CREATIVE CONTINUATION... (Your response MUST be a JSON object with "action": "APPEND" and no new_chapter_title...)

    3.  If it's an EDITING COMMAND... (Your response MUST be a JSON object with "action": "REPLACE" and no new_chapter_title...)

    4.  If the instruction asks for harmful/explicit content... (Your response MUST be a JSON object with "action": "REFUSE", "story_text": "", and no new_chapter_title...)

    5.  If the instruction is PURELY CONVERSATIONAL... (Your response MUST be a JSON object with "action": "CHAT", "story_text": "", and no new_chapter_title...)

    Your JSON response must be valid and contain nothing else.
    """
    try:
        response = text_model.generate_content(prompt)
        cleaned_response_text = (response.text or "").strip()
        for fence in ("```json", "```JSON", "```", "`"):
            cleaned_response_text = cleaned_response_text.replace(fence, "")
        response_data = json.loads(cleaned_response_text)
        return StoryResponse(
            action=response_data.get("action", "APPEND"),
            story_text=response_data.get("story_text", ""),
            chat_response=response_data.get("chat_response", "Let's try that again."),
            new_chapter_title=response_data.get("new_chapter_title")
        )
    except (json.JSONDecodeError, Exception) as e:
        print(f"An error occurred: {e}")
        return StoryResponse(
            action="CHAT",
            story_text="",
            chat_response="I'm having a little trouble thinking right now. Could you rephrase that?"
        )

@app.post("/api/stories", response_model=models.Story)
def create_story(story_data: FullStoryCreate, session: Session = Depends(get_session)):
    """
    Creates a new story and its chapters in the database.
    """
    story_to_create = models.Story(
        title=story_data.title,
        genre=story_data.genre,
        ai_name=story_data.ai_name
    )
    for chapter_from_request in story_data.chapters:
        new_chapter = models.Chapter(
            chapter_number=chapter_from_request.chapter_number,
            title=chapter_from_request.title,
            content=chapter_from_request.content,
            story=story_to_create
        )
        session.add(new_chapter)
    session.commit()
    session.refresh(story_to_create)
    return story_to_create

@app.get("/api/stories", response_model=list[models.Story])
def read_stories(session: Session = Depends(get_session)):
    """
    Returns a list of all stories in the database.
    """
    stories = session.exec(select(models.Story)).all()
    return stories

@app.get("/api/stories/{story_id}", response_model=models.Story)
def read_story(story_id: int, session: Session = Depends(get_session)):
    """
    Returns a single story by its ID, including all its chapters.
    """
    story = session.get(models.Story, story_id)
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")
    return story

@app.post("/api/stories/{story_id}/generate_cover", response_model=models.Story)
def generate_cover(story_id: int, request: Request, session: Session = Depends(get_session)):
    """
    Summarizes a story and generates a book cover image for it using the Hugging Face Inference API (Stable Diffusion XL).
    - Saves the image under ./covers and serves it at /covers/{filename}
    - Returns the updated Story with cover_image_url populated
    """
    require_hf_key()

    story = session.get(models.Story, story_id)
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    # Ensure the story has chapters/content
    if not getattr(story, "chapters", None) or len(story.chapters) == 0:
        raise HTTPException(status_code=400, detail="Story has no chapters to summarize for cover generation")

    # 1) Build a concise, visual summary for the image prompt using all chapters
    try:
        full_text = "\n\n".join([c.content for c in story.chapters])
        summary_prompt = (
            "Summarize the following story in ONE vivid, visually descriptive sentence optimized for an AI image generator. "
            "Avoid character names unless essential; focus on mood, setting, and standout imagery. Story: " + full_text[:6000]
        )
        summary_response = text_model.generate_content(summary_prompt)
        visual_summary = (summary_response.text or "").strip().replace("```", "")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gemini summarization failed: {e}")

    # 2) Create the final image generation prompt
    image_prompt = f"A professional book cover for a {story.genre} novel titled '{story.title}'. {visual_summary}"

    # 3) Call the Hugging Face Inference API
    headers = {
        "Authorization": f"Bearer {HF_API_KEY}",
        "Accept": "image/png",
    }
    payload = {"inputs": image_prompt}

    try:
        hf_resp = requests.post(HF_API_URL, headers=headers, json=payload, timeout=120)
    except requests.Timeout:
        raise HTTPException(status_code=504, detail="Hugging Face API timeout while generating cover image")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Hugging Face request failed: {e}")

    # 4) Handle HF response
    if hf_resp.status_code != 200:
        # Try to parse error body for clarity
        err_text = hf_resp.text
        try:
            err_json = hf_resp.json()
            err_text = err_json.get("error") or err_text
        except Exception:
            pass

        if hf_resp.status_code in (401, 403):
            raise HTTPException(status_code=401, detail=f"Invalid or unauthorized Hugging Face key: {err_text}")
        if hf_resp.status_code == 503:
            raise HTTPException(status_code=503, detail=f"Model is loading or unavailable on Hugging Face: {err_text}")
        raise HTTPException(status_code=hf_resp.status_code, detail=f"Hugging Face API error: {err_text}")

    content_type = hf_resp.headers.get("content-type", "")
    if not content_type.startswith("image/"):
        # Sometimes HF returns JSON even with 200; guard against saving non-image
        try:
            as_json = hf_resp.json()
            raise HTTPException(status_code=500, detail=f"Unexpected non-image response from Hugging Face: {as_json}")
        except Exception:
            raise HTTPException(status_code=500, detail="Unexpected non-image response from Hugging Face")

    # 5) Save image to ./covers and build a public URL via mounted static path
    image_bytes = hf_resp.content
    filename = f"cover_{story_id}.png"
    filepath = os.path.join(COVERS_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(image_bytes)

    # Build absolute URL: base_url already ends with '/'
    image_url = str(request.base_url) + f"covers/{filename}"

    # 6) Persist to DB and return updated Story
    story.cover_image_url = image_url
    session.add(story)
    session.commit()
    session.refresh(story)
    return story