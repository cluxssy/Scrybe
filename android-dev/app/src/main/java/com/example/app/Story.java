package com.example.app;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable; // <-- Import Serializable
import java.util.List;

public class Story implements Serializable { // <-- Implement Serializable

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