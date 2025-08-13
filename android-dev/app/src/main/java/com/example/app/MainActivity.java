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
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.app.BuildConfig;

public class MainActivity extends AppCompatActivity {

    private String aiName;
    private String genre;

    // --- UI elements for the canvas ---
    private RecyclerView storyRecyclerView;
    private StoryCanvasAdapter storyCanvasAdapter;
    private List<StoryElement> storyElements;
    private EditText userInputEditText;
    private Button sendButton;
    private ImageButton recordButton;
    // Note: save and library buttons can be re-added to a Toolbar later

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

        // --- Get data from previous activities ---
        aiName = getIntent().getStringExtra("AI_NAME");
        genre = getIntent().getStringExtra("GENRE");
        if (aiName == null) aiName = "Orion";
        if (genre == null) genre = "Mystery";

        // --- Find UI Views ---
        userInputEditText = findViewById(R.id.userInputEditText);
        sendButton = findViewById(R.id.sendButton);
        recordButton = findViewById(R.id.recordButton);

        // --- Setup the RecyclerView ---
        storyRecyclerView = findViewById(R.id.storyRecyclerView);
        storyElements = new ArrayList<>();
        storyCanvasAdapter = new StoryCanvasAdapter(storyElements);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        storyRecyclerView.setLayoutManager(layoutManager);
        storyRecyclerView.setAdapter(storyCanvasAdapter);

        // --- Set initial AI prompt ---
        addAiResponse("The " + genre + " story begins...");

        // --- Set up input listeners ---
        sendButton.setOnClickListener(v -> handleTextSend());

        recordButton.setOnTouchListener((v, event) -> {
            if (permissionToRecordAccepted) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true); // Show button pressed state
                        startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false); // Reset button state
                        stopRecording();
                        break;
                }
            } else {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            }
            return true;
        });
    }

    private void handleTextSend() {
        String inputText = userInputEditText.getText().toString().trim();
        if (!inputText.isEmpty()) {
            addUserInput(inputText);
            sendStoryContinuationRequest(inputText);
            userInputEditText.setText(""); // Clear the input field
        }
    }

    // --- Helper methods to add elements to the canvas ---
    private void addUserInput(String text) {
        storyElements.add(new StoryElement(text, StoryElement.TYPE_USER));
        storyCanvasAdapter.notifyItemInserted(storyElements.size() - 1);
        storyRecyclerView.scrollToPosition(storyElements.size() - 1);
    }

    private void addAiResponse(String text) {
        storyElements.add(new StoryElement(text, StoryElement.TYPE_AI));
        storyCanvasAdapter.notifyItemInserted(storyElements.size() - 1);
        storyRecyclerView.scrollToPosition(storyElements.size() - 1);
    }

    private void addChapter(String title) {
        storyElements.add(new StoryElement(title, StoryElement.TYPE_CHAPTER));
        storyCanvasAdapter.notifyItemInserted(storyElements.size() - 1);
        storyRecyclerView.scrollToPosition(storyElements.size() - 1);
    }

    // --- Helper method to get the full story text ---
    private String getStoryContext() {
        // Use Java 8 Stream API to join the text from all elements
        return storyElements.stream()
                .map(element -> element.text)
                .collect(Collectors.joining("\n\n"));
    }


    // --- Audio Recording & Transcription ---

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
            try {
                recorder.stop();
                recorder.release();
                recorder = null;
                Toast.makeText(this, "Transcribing...", Toast.LENGTH_SHORT).show();
                transcribeAudioFile(fileName);
            } catch (RuntimeException e) {
                Log.e("MainActivity", "stopRecording failed", e);
                // Handle case where recording is stopped too quickly
            }
        }
    }

    private void transcribeAudioFile(String filePath) {
        File audioFile = new File(filePath);
        // Ensure you have a secure way to store your API key
        String authToken = "Bearer " + BuildConfig.HUGGING_FACE_API_KEY;

        if (BuildConfig.HUGGING_FACE_API_KEY == null || BuildConfig.HUGGING_FACE_API_KEY.isEmpty()) {
            Toast.makeText(this, "Hugging Face API Key not found.", Toast.LENGTH_LONG).show();
            return;
        }

        RequestBody audioRequestBody = RequestBody.create(MediaType.parse("audio/*"), audioFile);
        HuggingFaceApiService hfApiService = HuggingFaceRetrofitClient.getApiService();

        Call<WhisperResponse> call = hfApiService.transcribeAudio(authToken, audioRequestBody);
        call.enqueue(new Callback<WhisperResponse>() {
            @Override
            public void onResponse(@NonNull Call<WhisperResponse> call, @NonNull Response<WhisperResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String transcribedText = response.body().text;
                    Log.d("MainActivity", "Hugging Face Transcription: " + transcribedText);
                    runOnUiThread(() -> {
                        addUserInput(transcribedText);
                        sendStoryContinuationRequest(transcribedText);
                    });
                } else {
                    Log.e("MainActivity", "HF API Error: " + response.code() + " - " + response.message());
                    Toast.makeText(MainActivity.this, "Transcription failed.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<WhisperResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "HF API Failure", t);
                Toast.makeText(MainActivity.this, "Transcription network failure.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Story Generation & Saving ---

    private void sendStoryContinuationRequest(String userInput) {
        String currentStory = getStoryContext(); // Use the new helper method

        StoryRequest request = new StoryRequest(aiName, genre, currentStory, userInput);
        ApiService apiService = RetrofitClient.getApiService();
        Call<StoryResponse> call = apiService.continueStory(request);
        call.enqueue(new Callback<StoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoryResponse> call, @NonNull Response<StoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = response.body().aiResponse;
                    runOnUiThread(() -> addAiResponse(aiResponse));
                } else {
                    Log.e("MainActivity", "Story API Error: " + response.code());
                    Toast.makeText(MainActivity.this, "AI failed to respond.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<StoryResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Story API Failure", t);
                Toast.makeText(MainActivity.this, "Network error. AI unreachable.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStory() {
        String fullStoryText = getStoryContext(); // Use the new helper method

        if (storyElements.size() <= 1) { // Only contains the initial prompt
            Toast.makeText(this, "Cannot save an empty story.", Toast.LENGTH_SHORT).show();
            return;
        }

        Chapter chapter1 = new Chapter(1, "Chapter 1", fullStoryText);
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(chapter1);

        FullStoryCreate storyToSave = new FullStoryCreate(
                "My New Story", // TODO: Let user input a title before saving
                genre,
                aiName,
                chapters
        );

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