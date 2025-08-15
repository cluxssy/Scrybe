package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StoryAdapter storyAdapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        apiService = RetrofitClient.getApiService();
        recyclerView = findViewById(R.id.storiesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list and the click listener
        storyAdapter = new StoryAdapter(this, new ArrayList<>(), story -> {
            fetchStoryDetailsAndOpenViewer(story.id);
        });
        recyclerView.setAdapter(storyAdapter);

        fetchStories();
    }

    private void fetchStories() {
        Call<List<Story>> call = apiService.readStories();

        call.enqueue(new Callback<List<Story>>() {
            @Override
            public void onResponse(Call<List<Story>> call, Response<List<Story>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // This is the corrected line: Use the adapter's own method to update its data
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
}