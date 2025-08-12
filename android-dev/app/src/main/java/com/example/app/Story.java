package com.example.app;

// In Story.java
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Story {

    @SerializedName("id")
    public int id;

    @SerializedName("title")
    public String title;

    @SerializedName("genre")
    public String genre;

    @SerializedName("ai_name")
    public String ai_name;

    @SerializedName("cover_image_url")
    public String cover_image_url;

    @SerializedName("chapters")
    public List<Chapter> chapters;
}
