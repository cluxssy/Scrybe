package com.example.app;

// In LibraryActivity.java

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    // 1. Declare our UI elements and the adapter
    private RecyclerView recyclerView;
    private StoryAdapter storyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // 2. Find the RecyclerView from our layout
        recyclerView = findViewById(R.id.storiesRecyclerView);

        // 3. Set up the RecyclerView
        // A LayoutManager is required to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Create an instance of our adapter and attach it to the RecyclerView
        storyAdapter = new StoryAdapter(this);
        recyclerView.setAdapter(storyAdapter);

        // 4. Fetch the stories from our backend
        fetchStories();
    }

    private void fetchStories() {
        // Get the ApiService from our RetrofitClient
        ApiService apiService = RetrofitClient.getApiService();
        // Make the call to the GET /api/stories endpoint
        Call<List<Story>> call = apiService.readStories();

        call.enqueue(new Callback<List<Story>>() {
            @Override
            public void onResponse(Call<List<Story>> call, Response<List<Story>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // If successful, pass the list of stories to our adapter
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
}