package com.example.app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewerActivity extends AppCompatActivity {

    ImageView coverImageView;
    TextView titleTextView;
    TextView storyContentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        coverImageView = findViewById(R.id.coverImageView);
        titleTextView = findViewById(R.id.titleTextView);
        storyContentTextView = findViewById(R.id.storyContentTextView);

        int storyId = getIntent().getIntExtra("STORY_ID", -1);
        if (storyId != -1) {
            ApiService apiService = RetrofitClient.getApiService();
            apiService.getStoryDetails(storyId).enqueue(new Callback<Story>() {
                @Override
                public void onResponse(Call<Story> call, Response<Story> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Story story = response.body();
                        titleTextView.setText(story.title);

                        if (story.cover_image_url != null && !story.cover_image_url.isEmpty()) {
                            Picasso.get().load(story.cover_image_url).into(coverImageView);
                        }

                        StringBuilder chaptersText = new StringBuilder();
                        if (story.chapters != null) {
                            for (Chapter chapter : story.chapters) {
                                chaptersText.append("Chapter ")
                                        .append(chapter.chapter_number)
                                        .append(": ")
                                        .append(chapter.title)
                                        .append("\n\n")
                                        .append(chapter.content)
                                        .append("\n\n");
                            }
                        } else {
                            chaptersText.append("No chapters available.");
                        }
                        storyContentTextView.setText(chaptersText.toString());
                    } else {
                        storyContentTextView.setText("Failed to load story details.");
                    }
                }

                @Override
                public void onFailure(Call<Story> call, Throwable t) {
                    storyContentTextView.setText("Error: " + t.getMessage());
                }
            });
        }
    }
}