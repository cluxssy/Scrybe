package com.example.app;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable; // <-- Import Serializable

public class Chapter implements Serializable { // <-- Implement Serializable

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

    public Chapter(int chapterNumber, String title, String content) {
        this.chapter_number = chapterNumber;
        this.title = title;
        this.content = content;
    }
}