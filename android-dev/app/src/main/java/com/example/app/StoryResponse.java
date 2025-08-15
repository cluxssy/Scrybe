package com.example.app;

import com.google.gson.annotations.SerializedName;

public class StoryResponse {
    @SerializedName("action")
    public String action;

    @SerializedName("story_text")
    public String storyText;

    @SerializedName("chat_response")
    public String chatResponse;

    // NEW: Add the optional chapter title field
    @SerializedName("new_chapter_title")
    public String newChapterTitle;
}