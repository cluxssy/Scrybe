package com.example.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameTextView, emailTextView, storiesCreatedTextView, totalWordsTextView, mostCommonGenreTextView;
    private EditText aiNameEditText;
    private Button saveAiNameButton;

    private ApiService apiService;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        apiService = RetrofitClient.getApiService();//this
        prefs = getSharedPreferences("ScrybePrefs", MODE_PRIVATE);

        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        aiNameEditText = findViewById(R.id.aiNameEditText);
        saveAiNameButton = findViewById(R.id.saveAiNameButton);
        storiesCreatedTextView = findViewById(R.id.storiesCreatedTextView);
        totalWordsTextView = findViewById(R.id.totalWordsTextView);
        mostCommonGenreTextView = findViewById(R.id.mostCommonGenreTextView);

        loadProfileData();
        loadStatsData();

        aiNameEditText.setText(prefs.getString("aiName", "Orion"));

        saveAiNameButton.setOnClickListener(v -> {
            String newAiName = aiNameEditText.getText().toString().trim();
            if (!newAiName.isEmpty()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("aiName", newAiName);
                editor.apply();
                Toast.makeText(this, "AI name updated!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProfileData() {
        apiService.readUsersMe().enqueue(new Callback<UserRead>() {
            @Override
            public void onResponse(@NonNull Call<UserRead> call, @NonNull Response<UserRead> response) {
                if (response.isSuccessful() && response.body() != null) {
                    usernameTextView.setText(response.body().username);
                    emailTextView.setText(response.body().email);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserRead> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatsData() {
        apiService.getUserStats().enqueue(new Callback<ProfileStats>() {
            @Override
            public void onResponse(@NonNull Call<ProfileStats> call, @NonNull Response<ProfileStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    storiesCreatedTextView.setText("Stories Created: " + response.body().stories_created);
                    totalWordsTextView.setText("Total Words Written: " + response.body().total_words);
                    mostCommonGenreTextView.setText("Favorite Genre: " + response.body().most_common_genre);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileStats> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Failed to load stats.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}