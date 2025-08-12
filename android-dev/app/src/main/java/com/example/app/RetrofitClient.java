package com.example.app;

import java.util.concurrent.TimeUnit; // <-- Add this import

import okhttp3.OkHttpClient; // <-- Add this import
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.0.2.2:8000/";

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {

            // Create a new OkHttpClient with a longer timeout
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // Time to establish a connection
                    .readTimeout(30, TimeUnit.SECONDS)    // Time to wait for data
                    .writeTimeout(30, TimeUnit.SECONDS)   // Time to send data
                    .build();

            // Build Retrofit using our custom OkHttpClient
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // <-- Attach the custom client
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}