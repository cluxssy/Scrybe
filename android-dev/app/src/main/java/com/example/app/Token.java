package com.example.app;

import com.google.gson.annotations.SerializedName;

public class Token {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("token_type")
    public String tokenType;
}