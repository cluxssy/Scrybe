package com.example.app;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("username")
    public String username;

    @SerializedName("email")
    public String email;

    @SerializedName("password")
    public String password;

    @SerializedName("password2")
    public String password2;

    public User(String username, String email, String password, String password2) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.password2 = password2;
    }
}