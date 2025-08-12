package com.example.app; // Make sure this matches your package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    EditText aiNameEditText;
    Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    // Create an Intent to open MainActivity
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    // Pass the AI's name to the next activity
                    intent.putExtra("AI_NAME", aiName);
                    startActivity(intent);
                    // Optional: finish() this activity so the user can't go back to it
                    finish();
                }
            }
        });
    }
}