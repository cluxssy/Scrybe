package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    private EditText aiNameEditText;
    private Button continueButton;
    private ApiService apiService; // Service to communicate with the backend

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize the Retrofit service
        apiService = RetrofitClient.getApiService();

        aiNameEditText = findViewById(R.id.aiNameEditText);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            String aiName = aiNameEditText.getText().toString().trim();

            if (aiName.isEmpty()) {
                Toast.makeText(WelcomeActivity.this, "Please enter a name for your AI.", Toast.LENGTH_SHORT).show();
            } else {
                // Save the name to the backend instead of local storage
                saveAiNameToBackend(aiName);
            }
        });
    }

    /**
     * Sends the chosen AI name to the backend to be saved with the user's profile.
     * @param name The name the user has chosen for the AI.
     */
    private void saveAiNameToBackend(String name) {
        // Create the request body for the API call
        ApiService.AiNameUpdate update = new ApiService.AiNameUpdate(name);

        apiService.updateAiName(update).enqueue(new Callback<UserRead>() {
            @Override
            public void onResponse(@NonNull Call<UserRead> call, @NonNull Response<UserRead> response) {
                if (response.isSuccessful()) {
                    // If the name was saved successfully, proceed to the main library screen
                    Toast.makeText(WelcomeActivity.this, "AI name saved!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WelcomeActivity.this, LibraryActivity.class);
                    startActivity(intent);
                    finish(); // Close this activity so the user cannot navigate back
                } else {
                    Toast.makeText(WelcomeActivity.this, "Failed to save the name. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserRead> call, @NonNull Throwable t) {
                Toast.makeText(WelcomeActivity.this, "A network error occurred.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}