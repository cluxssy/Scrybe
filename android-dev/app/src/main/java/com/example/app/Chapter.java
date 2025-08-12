package com.example.app;

// In Chapter.java
import com.google.gson.annotations.SerializedName;

public class Chapter {

    @SerializedName("id")
    public int id;

    @SerializedName("chapter_number")
    public int chapter_number;

    @SerializedName("title")
    public String title;

    @SerializedName("content")
    public String content;

    @SerializedName("story_id")
    public int story_id;

    // This constructor is used when creating a new story to send to the backend
    public Chapter(int chapterNumber, String title, String content) {
        this.chapter_number = chapterNumber;
        this.title = title;
        this.content = content;
    }
}
