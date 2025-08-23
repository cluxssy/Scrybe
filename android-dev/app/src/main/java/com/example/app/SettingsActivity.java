package com.example.app;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat themeSwitch;
    private SharedPreferences prefs;
    public static final String THEME_KEY = "theme_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Enable the back button in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        prefs = getSharedPreferences("ScrybePrefs", MODE_PRIVATE);

        TextView changePasswordTextView = findViewById(R.id.changePasswordTextView);
        TextView deleteAccountTextView = findViewById(R.id.deleteAccountTextView);
        themeSwitch = findViewById(R.id.themeSwitch);

        // Set the initial state of the switch based on the current theme
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        themeSwitch.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                prefs.edit().putString(THEME_KEY, "dark").apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                prefs.edit().putString(THEME_KEY, "light").apply();
            }
            // No need to recreate the activity, this will apply the theme on the next app start.
            // For an instant change, more complex logic is needed, which we can address later if you wish.
        });

        changePasswordTextView.setOnClickListener(v -> {
            Toast.makeText(this, "Change password screen coming soon!", Toast.LENGTH_SHORT).show();
        });

        deleteAccountTextView.setOnClickListener(v -> {
            Toast.makeText(this, "Account deletion coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    // This method handles clicks on toolbar items, including the back button
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // This is the ID for the back button
            finish(); // Closes the current activity and returns to the previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}