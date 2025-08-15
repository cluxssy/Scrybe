package com.example.app;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path; // Import Path

public interface ApiService {
    @POST("api/continue_story")
    Call<StoryResponse> continueStory(@Body StoryRequest request);

    @POST("api/stories")
    Call<Story> createStory(@Body FullStoryCreate story);

    @GET("api/stories")
    Call<List<Story>> readStories();

    // NEW: Add the endpoint for generating a cover
    @POST("api/stories/{story_id}/generate_cover")
    Call<Story> generateCover(@Path("story_id") int storyId);
}