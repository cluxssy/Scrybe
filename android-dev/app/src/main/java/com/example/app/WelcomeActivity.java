package com.example.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private EditText aiNameEditText;
    private Button continueButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("ScrybePrefs", MODE_PRIVATE);
        boolean hasNamedAi = prefs.getBoolean("hasNamedAi", false);

        if (hasNamedAi) {
            // If AI has been named, skip this activity
            launchGenreSelection();
            return;
        }

        setContentView(R.layout.activity_welcome);

        aiNameEditText = findViewById(R.id.aiNameEditText);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aiName = aiNameEditText.getText().toString().trim();

                if (aiName.isEmpty()) {
                    Toast.makeText(WelcomeActivity.this, "Please enter a name for your AI.", Toast.LENGTH_SHORT).show();
                } else {
                    // Save the AI name and the flag
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("aiName", aiName);
                    editor.putBoolean("hasNamedAi", true);
                    editor.apply();

                    launchGenreSelection();
                }
            }
        });
    }

    private void launchGenreSelection() {
        Intent intent = new Intent(WelcomeActivity.this, GenreSelectionActivity.class);
        startActivity(intent);
        finish(); // Prevent user from returning to this screen
    }
}