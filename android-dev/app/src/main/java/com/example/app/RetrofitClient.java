package com.example.app;

import android.content.Context;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.0.118:8000/";

    private static Retrofit retrofit = null;
    private static SessionManager sessionManager;

    // A static method to initialize the SessionManager
    public static void initialize(Context context) {
        sessionManager = new SessionManager(context);
    }

    public static ApiService getApiService() {
        if (retrofit == null) {

            // Interceptor to add the auth token to headers
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    String token = sessionManager.fetchAuthToken();

                    if (token != null && !token.isEmpty()) {
                        Request.Builder builder = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + token);
                        Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(originalRequest);
                }
            };

            // Create a new OkHttpClient with the interceptor and a longer timeout
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor) // Add the interceptor here
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Build Retrofit using our custom OkHttpClient
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}