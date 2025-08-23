package com.example.app;

import com.google.gson.annotations.SerializedName;

public class ProfileStats {
    @SerializedName("stories_created")
    public int stories_created;

    @SerializedName("total_words")
    public int total_words;

    @SerializedName("most_common_genre")
    public String most_common_genre;
}