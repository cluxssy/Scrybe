package com.example.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpActivity extends AppCompatActivity {

    private EditText otpEditText;
    private Button verifyButton;
    private ApiService apiService;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        apiService = RetrofitClient.getApiService();
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        otpEditText = findViewById(R.id.otpEditText);
        verifyButton = findViewById(R.id.verifyButton);

        verifyButton.setOnClickListener(v -> verifyOtp());
    }

    private void verifyOtp() {
        String otp = otpEditText.getText().toString().trim();

        if (otp.isEmpty() || userEmail == null) {
            Toast.makeText(this, "Please enter the OTP.", Toast.LENGTH_SHORT).show();
            return;
        }

        OtpVerify otpVerify = new OtpVerify(userEmail, otp);
        apiService.verifyOtp(otpVerify).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpActivity.this, "Verification successful! You can now log in.", Toast.LENGTH_LONG).show();
                    finish(); // Close the OTP activity and return to the login screen
                } else {
                    Toast.makeText(OtpActivity.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(OtpActivity.this, "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}