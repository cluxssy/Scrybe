package com.example.app;

// In FullStoryCreate.java
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FullStoryCreate {

    @SerializedName("title")
    String title;

    @SerializedName("genre")
    String genre;

    @SerializedName("ai_name")
    String ai_name;

    @SerializedName("chapters")
    List<Chapter> chapters;

    public FullStoryCreate(String title, String genre, String ai_name, List<Chapter> chapters) {
        this.title = title;
        this.genre = genre;
        this.ai_name = ai_name;
        this.chapters = chapters;
    }
}
