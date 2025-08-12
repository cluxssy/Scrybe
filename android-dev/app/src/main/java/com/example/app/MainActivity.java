package com.example.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private String aiName;
    private TextView storyTextView;
    private EditText userInputEditText;
    private Button recordButton;
    private Button saveButton;
    private Button libraryButton;

    // Permissions
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    // Audio Recording
    private MediaRecorder recorder = null;
    private String fileName = null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) {
            Toast.makeText(this, "Microphone permission is required for voice input.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        aiName = getIntent().getStringExtra("AI_NAME");
        if (aiName == null) {
            aiName = "Orion";
        }
        Toast.makeText(this, "Your co-author is " + aiName, Toast.LENGTH_SHORT).show();

        storyTextView = findViewById(R.id.storyTextView);
        userInputEditText = findViewById(R.id.userInputEditText);
        recordButton = findViewById(R.id.sendButton); // Assuming ID is still sendButton in XML
        saveButton = findViewById(R.id.saveButton);
        libraryButton = findViewById(R.id.libraryButton);

        // --- NEW: Replaced OnClickListener with OnTouchListener ---
        recordButton.setOnTouchListener((v, event) -> {
            if (permissionToRecordAccepted) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRecording();
                        recordButton.setText("Recording...");
                        break;
                    case MotionEvent.ACTION_UP:
                        stopRecording();
                        recordButton.setText("Send");
                        break;
                }
            } else {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            }
            return true;
        });

        // --- Note: The original sendButton OnClickListener is now gone ---

        saveButton.setOnClickListener(v -> saveStory());
        libraryButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LibraryActivity.class)));
    }

    private void startRecording() {
        fileName = getExternalCacheDir().getAbsolutePath() + "/whisprr_audio.mp3";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("MainActivity", "startRecording prepare() failed", e);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            Toast.makeText(this, "Transcribing...", Toast.LENGTH_SHORT).show();
            transcribeAudioFile(fileName);
        }
    }

    private void transcribeAudioFile(String filePath) {
        File audioFile = new File(filePath);
        RequestBody audioRequestBody = RequestBody.create(MediaType.parse("audio/*"), audioFile);
        HuggingFaceApiService hfApiService = HuggingFaceRetrofitClient.getApiService();

        // IMPORTANT: Replace with your Hugging Face Token
        String authToken = "Bearer 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";

        Call<WhisperResponse> call = hfApiService.transcribeAudio(authToken, audioRequestBody);
        call.enqueue(new Callback<WhisperResponse>() {
            @Override
            public void onResponse(@NonNull Call<WhisperResponse> call, @NonNull Response<WhisperResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String transcribedText = response.body().text;
                    Log.d("MainActivity", "Hugging Face Transcription: " + transcribedText);
                    // Use the transcribed text as input for our story AI
                    sendStoryContinuationRequest(transcribedText);
                } else {
                    Log.e("MainActivity", "HF API Error: " + response.code());
                    Toast.makeText(MainActivity.this, "Transcription failed. Check logs.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<WhisperResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "HF API Failure", t);
                Toast.makeText(MainActivity.this, "Transcription network failure.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // MODIFIED to be reusable
    private void sendStoryContinuationRequest(String userInput) {
        String currentStory = storyTextView.getText().toString();
        // The original logic for typing is now disabled, but we can keep the method
        // in case we want a toggle between voice/text later.
        if (userInput == null || userInput.trim().isEmpty()) {
            Toast.makeText(this, "Input was empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        StoryRequest request = new StoryRequest(aiName, "mystery", currentStory, userInput);
        ApiService apiService = RetrofitClient.getApiService();
        Call<StoryResponse> call = apiService.continueStory(request);
        call.enqueue(new Callback<StoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoryResponse> call, @NonNull Response<StoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = response.body().aiResponse;
                    runOnUiThread(() -> storyTextView.append("\n\n" + aiResponse));
                } else {
                    Log.e("MainActivity", "Story API Error: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<StoryResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Story API Failure", t);
            }
        });
    }

    // Add this entire new method to your MainActivity class
    private void saveStory() {
        String fullStoryText = storyTextView.getText().toString();

        if (fullStoryText.isEmpty() || fullStoryText.equals("The story begins...")) {
            Toast.makeText(this, "Cannot save an empty story.", Toast.LENGTH_SHORT).show();
            return;
        }

        // For now, we will treat the entire story as a single chapter.
        Chapter chapter1 = new Chapter(1, "Chapter 1", fullStoryText);
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(chapter1);

        // Create the main request object that our backend expects
        FullStoryCreate storyToSave = new FullStoryCreate(
                "My New Story", // We can make the title dynamic later
                "Mystery",      // We can make the genre dynamic later
                aiName,         // Use the AI name passed from the WelcomeActivity
                chapters
        );

        // Get the ApiService and make the call to the /api/stories endpoint
        ApiService apiService = RetrofitClient.getApiService();
        Call<Story> call = apiService.createStory(storyToSave);

        call.enqueue(new Callback<Story>() {
            @Override
            public void onResponse(Call<Story> call, Response<Story> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String successMessage = "Story '" + response.body().title + "' saved successfully!";
                    Toast.makeText(MainActivity.this, successMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error saving story: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Story> call, Throwable t) {
                Log.e("MainActivity", "Save Story Failed", t);
                Toast.makeText(MainActivity.this, "Network failure while saving story.", Toast.LENGTH_SHORT).show();
            }

        });

    }
}