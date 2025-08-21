package com.example.app;

import com.google.gson.annotations.SerializedName;

public class OtpVerify {
    @SerializedName("email")
    String email;

    @SerializedName("otp")
    String otp;

    public OtpVerify(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}