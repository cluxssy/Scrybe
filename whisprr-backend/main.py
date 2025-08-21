# cluxssy/scrybe/Scrybe-c94d3f327bfcadc6ab5122064c5ce42612e537fa/whisprr-backend/main.py
import os
import google.generativeai as genai
from fastapi import FastAPI, Depends, HTTPException, Request, status, BackgroundTasks
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from dotenv import load_dotenv
from sqlmodel import Session, SQLModel, create_engine, select
import models
import json
import requests
from typing import Optional, List
from sqlalchemy.orm import selectinload
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta
import random
from fastapi_mail import FastMail, MessageSchema, ConnectionConfig
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests

# --- INITIALIZATION ---
load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

text_model = genai.GenerativeModel("gemini-1.5-flash-latest")

HF_API_URL = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0"
HF_API_KEY = os.getenv("HUGGING_FACE_API_KEY")
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")

app = FastAPI()

# --- NEW MODEL FOR GOOGLE TOKEN ---
class GoogleToken(BaseModel):
    token: str

# --- EMAIL CONFIG ---
conf = ConnectionConfig(
    MAIL_USERNAME=os.getenv("MAIL_USERNAME"),
    MAIL_PASSWORD=os.getenv("MAIL_PASSWORD"),
    MAIL_FROM=os.getenv("MAIL_FROM"),
    MAIL_PORT=587,
    MAIL_SERVER=os.getenv("MAIL_SERVER"),
    MAIL_STARTTLS=True,
    MAIL_SSL_TLS=False,
)

fm = FastMail(conf)

async def send_otp_email(email: str, otp: str):
    message = MessageSchema(
        subject="Your Scrybe OTP Code",
        recipients=[email],
        body=f"Your OTP code is {otp}. It is valid for 10 minutes.",
        subtype="html"
    )
    await fm.send_message(message)

# --- SECURITY ---
SECRET_KEY = "a_very_secret_key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

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

async def get_current_user(token: str = Depends(oauth2_scheme), session: Session = Depends(get_session)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
        token_data = models.TokenData(username=username)
    except JWTError:
        raise credentials_exception
    user = session.exec(select(models.User).where(models.User.username == token_data.username)).first()
    if user is None:
        raise credentials_exception
    return user

# --- AUTHENTICATION ---
@app.post("/api/signup")
async def signup(user: models.UserCreate, background_tasks: BackgroundTasks, session: Session = Depends(get_session)):
    if session.exec(select(models.User).where(models.User.username == user.username)).first():
        raise HTTPException(status_code=400, detail="Username already registered")
    if session.exec(select(models.User).where(models.User.email == user.email)).first():
        raise HTTPException(status_code=400, detail="Email already registered")

    hashed_password = get_password_hash(user.password)
    otp = str(random.randint(100000, 999999))
    otp_expires = datetime.utcnow() + timedelta(minutes=10)
    
    db_user = models.User(
        username=user.username,
        email=user.email,
        hashed_password=hashed_password,
        otp=otp,
        otp_expires_at=otp_expires
    )
    session.add(db_user)
    session.commit()

    background_tasks.add_task(send_otp_email, user.email, otp)
    
    return {"message": "Signup successful. OTP sent to email."}

@app.post("/api/verify-otp")
def verify_otp(request: models.OtpVerify, session: Session = Depends(get_session)):
    user = session.exec(select(models.User).where(models.User.email == request.email)).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if user.otp != request.otp or user.otp_expires_at < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Invalid or expired OTP")
    
    user.is_verified = True
    user.otp = None
    user.otp_expires_at = None
    session.add(user)
    session.commit()
    
    return {"message": "Email verified successfully."}

@app.post("/api/login", response_model=models.Token)
async def login(form_data: OAuth2PasswordRequestForm = Depends(), session: Session = Depends(get_session)):
    user = session.exec(select(models.User).where(models.User.username == form_data.username)).first()
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.is_verified:
        raise HTTPException(status_code=400, detail="Email not verified")
        
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/api/login/google", response_model=models.Token)
async def login_google(google_token: GoogleToken, session: Session = Depends(get_session)):
    try:
        idinfo = id_token.verify_oauth2_token(google_token.token, google_requests.Request(), GOOGLE_CLIENT_ID)
        email = idinfo['email']
        username = idinfo.get('name', email)

    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid Google token")

    user = session.exec(select(models.User).where(models.User.email == email)).first()

    if not user:
        dummy_hash = get_password_hash(f"google_auth_{random.randint(10000, 99999)}")
        user = models.User(
            username=username,
            email=email,
            hashed_password=dummy_hash,
            is_verified=True
        )
        session.add(user)
        session.commit()
        session.refresh(user)

    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

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
    chapters: List[models.ChapterCreate]

# --- STORY GENERATION ---
@app.post("/api/continue_story", response_model=StoryResponse)
async def continue_story(request: StoryRequest):
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
        print(f"Raw AI Response was: {response.text}")
        return StoryResponse(
            action="CHAT",
            story_text="",
            chat_response="I'm having a little trouble thinking right now. Could you rephrase that?"
        )

# --- CREATE STORY ---
@app.post("/api/stories", response_model=models.StoryRead)
def create_story(story_data: FullStoryCreate, session: Session = Depends(get_session), current_user: models.User = Depends(get_current_user)):
    story_to_create = models.Story(
        title=story_data.title,
        genre=story_data.genre,
        ai_name=story_data.ai_name,
        user_id=current_user.id
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
def read_stories(session: Session = Depends(get_session), current_user: models.User = Depends(get_current_user)):
    stories = session.exec(
        select(models.Story).where(models.Story.user_id == current_user.id).options(selectinload(models.Story.chapters))
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