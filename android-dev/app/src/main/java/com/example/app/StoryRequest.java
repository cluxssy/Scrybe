package com.example.app;
import com.google.gson.annotations.SerializedName;

public class StoryRequest {
    @SerializedName("ai_name")
    String aiName;

    @SerializedName("genre")
    String genre;

    @SerializedName("story_context")
    String storyContext;

    @SerializedName("user_input")
    String userInput;

    public StoryRequest(String aiName, String genre, String storyContext, String userInput) {
        this.aiName = aiName;
        this.genre = genre;
        this.storyContext = storyContext;
        this.userInput = userInput;
    }
}