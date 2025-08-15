import os
import google.generativeai as genai
from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from dotenv import load_dotenv
from sqlmodel import Session, SQLModel, create_engine, select
import models
import json
import requests
from typing import Optional, List
from sqlalchemy.orm import selectinload

# --- INITIALIZATION ---
load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

text_model = genai.GenerativeModel("gemini-1.5-flash-latest")

HF_API_URL = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0"
HF_API_KEY = os.getenv("HUGGING_FACE_API_KEY")

app = FastAPI()

# --- STATIC FILES ---
COVERS_DIR = "covers"
os.makedirs(COVERS_DIR, exist_ok=True)
app.mount("/covers", StaticFiles(directory=COVERS_DIR), name="covers")

# --- DATABASE ---
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

# --- REQUEST MODELS ---
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
    chapters: List[models.ChapterCreate]  # NEW: use Pydantic model for chapters

# --- STORY GENERATION ---
@app.post("/api/continue_story", response_model=StoryResponse)
async def continue_story(request: StoryRequest):
    """
    Analyzes user input to continue, edit, refuse, chat, or start a new chapter.
    """
    # This new prompt is more direct and less likely to confuse the AI.
    prompt = f"""
    You are a backend AI that MUST ONLY return a JSON object. Do not return any other text, explanation, or markdown.

    You are a creative partner named {request.ai_name}.
    Your task is to analyze the user's instruction based on the story so far and choose one of five actions:

    1.  **APPEND**: For a regular creative continuation.
    2.  **REPLACE**: If the user gives an editing command like "change the character's name" or "rewrite that last part."
    3.  **CHAPTER**: If the story reaches a natural break (climax, setting change, time jump).
    4.  **CHAT**: If the user is just talking to you ("hello", "what's next?").
    5.  **REFUSE**: If the user asks for harmful or explicit content.

    STORY SO FAR:
    ---
    {request.story_context}
    ---
    USER'S INSTRUCTION:
    ---
    {request.user_input}
    ---

    Based on your analysis, generate a valid JSON response with the following structure.

    -   For APPEND, REPLACE, or CHAPTER, the JSON must contain "action", "story_text", and "chat_response". For CHAPTER, also include "new_chapter_title".
    -   For CHAT or REFUSE, the JSON must contain "action" and "chat_response". "story_text" should be an empty string.

    Your response must be a single, valid JSON object and nothing else.
    """
    try:
        response = text_model.generate_content(prompt)
        cleaned_response_text = (response.text or "").strip().removeprefix("```json").removesuffix("```")
        
        response_data = json.loads(cleaned_response_text)
        
        return StoryResponse(
            action=response_data.get("action", "CHAT"),
            story_text=response_data.get("story_text", ""),
            chat_response=response_data.get("chat_response", "I seem to be at a loss for words. Could you try again?"),
            new_chapter_title=response_data.get("new_chapter_title")
        )
    except (json.JSONDecodeError, Exception) as e:
        print(f"An error occurred decoding the AI's JSON response: {e}")
        print(f"Raw AI Response was: {response.text}") # Added for better debugging
        return StoryResponse(
            action="CHAT",
            story_text="",
            chat_response="I'm having a little trouble thinking right now. Could you rephrase that?"
        )

# --- CREATE STORY ---
@app.post("/api/stories", response_model=models.StoryRead)
def create_story(story_data: FullStoryCreate, session: Session = Depends(get_session)):
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

# --- GET ALL STORIES (with chapters) ---
@app.get("/api/stories", response_model=List[models.StoryRead])
def read_stories(session: Session = Depends(get_session)):
    stories = session.exec(
        select(models.Story).options(selectinload(models.Story.chapters))
    ).all()
    return stories

# --- GET SINGLE STORY (with chapters) ---
@app.get("/api/stories/{story_id}", response_model=models.StoryRead)
def read_story(story_id: int, session: Session = Depends(get_session)):
    story = session.exec(
        select(models.Story)
        .where(models.Story.id == story_id)
        .options(selectinload(models.Story.chapters))
    ).first()
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")
    return story

# --- GENERATE COVER IMAGE ---
@app.post("/api/stories/{story_id}/generate_cover", response_model=models.StoryRead)
def generate_cover(story_id: int, request: Request, session: Session = Depends(get_session)):
    require_hf_key()

    story = session.exec(
        select(models.Story).options(selectinload(models.Story.chapters))
        .where(models.Story.id == story_id)
    ).first()
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    if not story.chapters:
        raise HTTPException(status_code=400, detail="Story has no chapters to summarize for cover generation")

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

    image_prompt = f"A professional book cover for a {story.genre} novel titled '{story.title}'. {visual_summary}"

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

    if hf_resp.status_code != 200:
        try:
            err_json = hf_resp.json()
            err_text = err_json.get("error") or hf_resp.text
        except Exception:
            err_text = hf_resp.text
        raise HTTPException(status_code=hf_resp.status_code, detail=f"Hugging Face API error: {err_text}")

    if not hf_resp.headers.get("content-type", "").startswith("image/"):
        raise HTTPException(status_code=500, detail="Unexpected non-image response from Hugging Face")

    filename = f"cover_{story_id}.png"
    filepath = os.path.join(COVERS_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(hf_resp.content)

    story.cover_image_url = str(request.base_url) + f"covers/{filename}"
    session.add(story)
    session.commit()
    session.refresh(story)
    return story