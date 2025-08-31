
from typing import List, Optional
from pydantic import BaseModel, EmailStr, validator
from sqlmodel import Field, Relationship, SQLModel
import datetime


class User(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    username: str = Field(index=True, unique=True)
    email: str = Field(unique=True)
    hashed_password: str
    is_verified: bool = Field(default=False)
    otp: Optional[str] = None
    otp_expires_at: Optional[datetime.datetime] = None
    ai_name: Optional[str] = None
    
    stories: List["Story"] = Relationship(back_populates="user")


class UserCreate(BaseModel):
    username: str
    email: EmailStr
    password: str
    password2: str

    @validator('password2')
    def passwords_match(cls, v, values, **kwargs):
        if 'password' in values and v != values['password']:
            raise ValueError('passwords do not match')
        return v

class OtpVerify(BaseModel):
    email: EmailStr
    otp: str

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: Optional[str] = None


class Story(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str = Field(index=True, default="Untitled Story")
    genre: str
    ai_name: str
    cover_image_url: Optional[str] = None

    chapters: List["Chapter"] = Relationship(back_populates="story")
    
    user_id: Optional[int] = Field(default=None, foreign_key="user.id")
    user: Optional[User] = Relationship(back_populates="stories")


class Chapter(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    chapter_number: int
    title: str = Field(default="Untitled Chapter")
    content: str 

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

class UserRead(BaseModel):
    username: str
    email: str
    ai_name: Optional[str] = None

class ProfileStats(BaseModel):
    stories_created: int
    total_words: int
    most_common_genre: Optional[str] = "N/A"
