package com.example.app;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/signup")
    Call<Void> signup(@Body User user);

    @POST("api/verify-otp")
    Call<Void> verifyOtp(@Body OtpVerify otpVerify);

    @FormUrlEncoded
    @POST("api/login")
    Call<Token> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @POST("api/continue_story")
    Call<StoryResponse> continueStory(@Body StoryRequest request);

    @POST("api/stories")
    Call<Story> createStory(@Body FullStoryCreate story);

    @GET("api/stories")
    Call<List<Story>> readStories();

    // NEW: Add the endpoint for fetching a single story
    @GET("api/stories/{story_id}")
    Call<Story> getStoryDetails(@Path("story_id") int storyId);

    @POST("api/stories/{story_id}/generate_cover")
    Call<Story> generateCover(@Path("story_id") int storyId);

    @POST("api/login/google")
    Call<Token> loginWithGoogle(@Body GoogleToken googleToken);
}