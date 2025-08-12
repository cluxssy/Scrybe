package com.example.app;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface HuggingFaceApiService {
    @Headers("Content-Type: audio/flac")
    @POST("models/openai/whisper-large-v3")
    Call<WhisperResponse> transcribeAudio(
            @Header("Authorization") String authorization,
            @Body RequestBody audioData
    );
}