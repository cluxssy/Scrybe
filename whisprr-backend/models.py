from typing import List, Optional
from pydantic import BaseModel
from sqlmodel import Field, Relationship, SQLModel


class Story(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str = Field(index=True, default="Untitled Story")
    genre: str
    ai_name: str
    cover_image_url: Optional[str] = None # We'll fill this in later

    # This creates the one-to-many relationship: One Story has many Chapters
    chapters: List["Chapter"] = Relationship(back_populates="story")


class Chapter(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    chapter_number: int
    title: str = Field(default="Untitled Chapter")
    content: str 

    # This links a chapter back to its parent story
    story_id: Optional[int] = Field(default=None, foreign_key="story.id")
    story: Optional[Story] = Relationship(back_populates="chapters")


# Pydantic schemas for reading data
class ChapterRead(BaseModel):
    id: int
    chapter_number: int
    title: str
    content: str

    class Config:
        from_attributes = True

class ChapterCreate(BaseModel):
    chapter_number: int
    title: str
    content: str

    class Config:
        from_attributes = True


class StoryRead(BaseModel):
    id: int
    title: str
    genre: str
    ai_name: str
    cover_image_url: Optional[str]
    chapters: List[ChapterRead] = []

    class Config:
        from_attributes = True

class StoryCreate(BaseModel):
    title: str
    genre: str
    ai_name: str
    chapters: List[ChapterCreate]

    class Config:
        from_attributes = True