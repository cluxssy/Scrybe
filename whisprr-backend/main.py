import os
import google.generativeai as genai
from fastapi import FastAPI
from pydantic import BaseModel
from dotenv import load_dotenv
from fastapi import Depends, HTTPException
from sqlmodel import Session, SQLModel, create_engine, select
import models # This import the models.py file 

# INITIALIZATION
load_dotenv()

genai.configure(api_key=os.getenv("GEMINI_API_KEY")) 

model = genai.GenerativeModel("gemini-1.5-flash")
app = FastAPI()

# Define the location of our database file
DATABASE_URL = "sqlite:///database.db"

# Create the engine: our main connection point to the database
# echo=True will print all the SQL statements it executes, which is great for debugging
engine = create_engine(DATABASE_URL, echo=True)


def create_db_and_tables():
    SQLModel.metadata.create_all(engine)

# This is a special FastAPI event that runs this function once when the server starts
@app.on_event("startup")
def on_startup():
    create_db_and_tables()

# This is the most important new function. It handles database sessions.
# Think of a session as a temporary conversation with the database.
# FastAPI's `Depends` will run this function for every request that needs it.
def get_session():
    with Session(engine) as session:
        yield session
# What does get_session do?
# The yield keyword is special. It provides the session to the endpoint, lets the endpoint do all of its database work,
# and then automatically closes the connection properly when it's done. It's very efficient and safe.

# DATA MODELS 
class StoryRequest(BaseModel):
    ai_name: str = "Orion"  
    genre: str
    story_context: str      
    user_input: str        

class StoryResponse(BaseModel): 
    ai_response: str        


# API ENDPOINT 
@app.post("/api/continue_story", response_model=StoryResponse)
async def continue_story(request: StoryRequest): 
    prompt = f"""You are {request.ai_name}, a master storyteller specializing in the {request.genre} genre.
    The story so far is:
    "{request.story_context}"

    The user has just added this idea: "{request.user_input}"

    Your task is to seamlessly continue the story, incorporating the user's idea.
    Write the next one or two paragraphs. Your response must be only the new story text, nothing else.
    """

    try:
        response = model.generate_content(prompt)
        return StoryResponse(ai_response=response.text)
    
    except Exception as e:
        
        print(f"An error occurred with the Gemini API: {e}")
        return StoryResponse(ai_response=f"An error occurred: Could not get a response from the AI.")
    

# A Pydantic model to define the structure for creating a full story
class FullStoryCreate(BaseModel):
    title: str
    genre: str
    ai_name: str
    chapters: list[models.Chapter] # A list of chapter objects

# Replace the old create_story function with this one in main.py

@app.post("/api/stories", response_model=models.Story)
def create_story(story_data: FullStoryCreate, session: Session = Depends(get_session)):
    """
    Creates a new story and its chapters in the database.
    """
    
    # 1. Create the main Story object directly from the request data
    story_to_create = models.Story(
        title=story_data.title,
        genre=story_data.genre,
        ai_name=story_data.ai_name
    )

    # 2. Create the Chapter objects and link them to the parent story
    for chapter_from_request in story_data.chapters:
        new_chapter = models.Chapter(
            chapter_number=chapter_from_request.chapter_number,
            title=chapter_from_request.title,
            content=chapter_from_request.content,
            story=story_to_create  # This creates the link!
        )
        session.add(new_chapter)

    # Note: We don't need to add story_to_create to the session separately.
    # SQLModel is smart enough to add it when it sees the chapters are linked to it.
    
    # 3. Commit everything to the database to save it permanently
    session.commit()
    
    # 4. Refresh the story object to load the newly created chapters and get its ID
    session.refresh(story_to_create)
    
    # 5. Return the newly created story
    return story_to_create

@app.get("/api/stories", response_model=list[models.Story])
def read_stories(session: Session = Depends(get_session)):
    """
    Returns a list of all stories in the database.
    """
    stories = session.exec(select(models.Story)).all()
    return stories

# Place this in main.py with the other endpoints

@app.get("/api/stories/{story_id}", response_model=models.Story)
def read_story(story_id: int, session: Session = Depends(get_session)):
    """
    Returns a single story by its ID, including all its chapters.
    """
    # .get() is the simplest way to find an object by its primary key (id)
    story = session.get(models.Story, story_id)
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")
    return story


