package com.example.app;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;

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

    @GET("api/users/me")
    Call<UserRead> readUsersMe();

    class AiNameUpdate {
        @SerializedName("ai_name")
        String ai_name;
        public AiNameUpdate(String name) { this.ai_name = name; }
    }

    @PUT("api/users/me/ai_name")
    Call<UserRead> updateAiName(@Body AiNameUpdate aiNameUpdate);

    @GET("api/users/me/stats")
    Call<ProfileStats> getUserStats();
}