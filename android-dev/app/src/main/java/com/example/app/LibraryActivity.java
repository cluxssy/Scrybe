package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StoryAdapter storyAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        recyclerView = findViewById(R.id.storiesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        storyAdapter = new StoryAdapter(this, new ArrayList<>(), story -> {
            fetchStoryDetailsAndOpenViewer(story.id);
        });
        recyclerView.setAdapter(storyAdapter);

        FloatingActionButton fab = findViewById(R.id.fab_new_story);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, GenreSelectionActivity.class);
            startActivity(intent);
        });

//        fetchStories();
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_library) {
            // Do nothing, already here
        } else if (itemId == R.id.nav_profile) {
            startActivity(new Intent(LibraryActivity.this, ProfileActivity.class));
        } else if (itemId == R.id.nav_settings) {
            startActivity(new Intent(LibraryActivity.this, SettingsActivity.class)); // Launch the new activity
        } else if (itemId == R.id.nav_theme) {
            Toast.makeText(this, "Theme options are in Settings.", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void fetchStories() {
        Call<List<Story>> call = apiService.readStories();

        call.enqueue(new Callback<List<Story>>() {
            @Override
            public void onResponse(Call<List<Story>> call, Response<List<Story>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    storyAdapter.setStories(response.body());
                } else {
                    Toast.makeText(LibraryActivity.this, "Failed to load stories.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Story>> call, Throwable t) {
                Log.e("LibraryActivity", "Error fetching stories", t);
                Toast.makeText(LibraryActivity.this, "Network error. Could not fetch stories.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchStoryDetailsAndOpenViewer(int storyId) {
        Intent intent = new Intent(LibraryActivity.this, StoryViewerActivity.class);
        intent.putExtra("STORY_ID", storyId);
        startActivity(intent);
    }

    private void logoutUser() {
        sessionManager.clearAuthToken();
        Intent intent = new Intent(LibraryActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Fetch stories every time the activity resumes
        fetchStories();
    }
}