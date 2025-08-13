package com.example.app;

public class StoryElement {
    // Define constants for the different types of views
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_CHAPTER = 2;

    public String text;
    public int type;

    public StoryElement(String text, int type) {
        this.text = text;
        this.type = type;
    }
}