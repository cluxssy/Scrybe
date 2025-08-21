package com.example.app;

import com.google.gson.annotations.SerializedName;

public class GoogleToken {
    @SerializedName("token")
    String token;

    public GoogleToken(String token) {
        this.token = token;
    }
}