package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GenreSelectionActivity extends AppCompatActivity {

    private String aiName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        // Get the AI name passed from WelcomeActivity
        aiName = getIntent().getStringExtra("AI_NAME");

        Button mysteryButton = findViewById(R.id.mysteryButton);
        Button horrorButton = findViewById(R.id.horrorButton);
        Button scifiButton = findViewById(R.id.scifiButton);
        Button fantasyButton = findViewById(R.id.fantasyButton);
        Button romanceButton = findViewById(R.id.romanceButton);

        View.OnClickListener listener = v -> {
            Button b = (Button) v;
            String genre = b.getText().toString();
            launchMainActivity(genre);
        };

        mysteryButton.setOnClickListener(listener);
        horrorButton.setOnClickListener(listener);
        scifiButton.setOnClickListener(listener);
        fantasyButton.setOnClickListener(listener);
        romanceButton.setOnClickListener(listener);
    }

    private void launchMainActivity(String genre) {
        Intent intent = new Intent(GenreSelectionActivity.this, MainActivity.class);
        intent.putExtra("AI_NAME", aiName);
        intent.putExtra("GENRE", genre);
        startActivity(intent);
        // Finish this activity so the user can't go back to it
        finish();
    }
}