package com.example.app;

import com.google.gson.annotations.SerializedName;

public class UserRead {
    @SerializedName("username")
    public String username;

    @SerializedName("email")
    public String email;
}