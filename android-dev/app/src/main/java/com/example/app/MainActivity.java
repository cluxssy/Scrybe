package com.example.app;

import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.os.Environment;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import java.io.InputStream;
import java.net.URL;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private String aiName;
    private String genre;

    // --- UI elements for the canvas and chat ---
    private TextView storyTextView;
    private RecyclerView chatRecyclerView;
    private EditText userInputEditText;
    private Button sendButton;
    private ImageButton recordButton;

    // --- Data sources for the two panels ---
    private SpannableStringBuilder storyBuilder;
    private List<StoryElement> chatElements = new ArrayList<>();
    private StoryCanvasAdapter chatAdapter;

    // Permissions & Audio
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private MediaRecorder recorder = null;
    private String fileName = null;
    private SessionManager sessionManager;

    private Handler typewriterHandler = new Handler(Looper.getMainLooper());



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

        sessionManager = new SessionManager(getApplicationContext());

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        aiName = getIntent().getStringExtra("AI_NAME");
        genre = getIntent().getStringExtra("GENRE");
        if (aiName == null) aiName = "Orion";
        if (genre == null) genre = "Mystery";

        // Find UI Views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        storyTextView = findViewById(R.id.storyTextView);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        userInputEditText = findViewById(R.id.userInputEditText);
        sendButton = findViewById(R.id.sendButton);
        recordButton = findViewById(R.id.recordButton);

        // Initialize Story Canvas
        storyBuilder = new SpannableStringBuilder();
        appendStoryParagraph("The " + genre + " story begins...\n\n");

        // Initialize Chat Log
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new StoryCanvasAdapter(chatElements);
        chatRecyclerView.setAdapter(chatAdapter);
        addMessageToChat("Hello! I'm " + aiName + ". How should we begin our " + genre + " story?", StoryElement.TYPE_AI);

        // Set up input listeners
        sendButton.setOnClickListener(v -> handleTextSend());
        recordButton.setOnTouchListener((v, event) -> {
            if (permissionToRecordAccepted) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        stopRecording();
                        break;
                }
            } else {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            }
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save) {
            saveStory();
            return true;
        } else if (itemId == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        sessionManager.clearAuthToken();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // Flags to clear the back stack and prevent user from returning to MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void handleTextSend() {
        String inputText = userInputEditText.getText().toString().trim();
        if (!inputText.isEmpty()) {
            addMessageToChat(inputText, StoryElement.TYPE_USER);
            sendStoryContinuationRequest(inputText);
            userInputEditText.setText("");
        }
    }

    // --- Helper methods for UI updates ---
    private void appendStoryParagraph(String text) {
        storyBuilder.append(text);
        storyTextView.setText(storyBuilder);
    }

    private void replaceStoryText(String newFullStory) {
        storyBuilder.clear();
        storyBuilder.append(newFullStory);
        storyTextView.setText(storyBuilder);
    }

    private void addMessageToChat(String text, int type) {
        chatElements.add(new StoryElement(text, type));
        chatAdapter.notifyItemInserted(chatElements.size() - 1);
        chatRecyclerView.scrollToPosition(chatElements.size() - 1);
    }

    private void addChapterTitle(String title) {
        // Add to story canvas
        storyBuilder.append("\n\n--- ").append(title).append(" ---\n\n");
        storyTextView.setText(storyBuilder);

        // Add to chat log
        addMessageToChat(title, StoryElement.TYPE_CHAPTER);
    }
    // --- Story Generation Logic ---
    private void sendStoryContinuationRequest(String userInput) {
        String currentStory = storyBuilder.toString();
        StoryRequest request = new StoryRequest(aiName, genre, currentStory, userInput);
        ApiService apiService = RetrofitClient.getApiService();
        Call<StoryResponse> call = apiService.continueStory(request);
        call.enqueue(new Callback<StoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoryResponse> call, @NonNull Response<StoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StoryResponse storyResponse = response.body();
                    runOnUiThread(() -> {
                        // Check for a new chapter title FIRST
                        if (storyResponse.newChapterTitle != null && !storyResponse.newChapterTitle.isEmpty()) {
                            addChapterTitle(storyResponse.newChapterTitle);
                        }

                        // Handle the AI's action
                        if ("REPLACE".equals(storyResponse.action)) {
                            replaceStoryText(storyResponse.storyText);
                        } else if ("APPEND".equals(storyResponse.action)) {
                            animateTypewriter(storyResponse.storyText);
                        }
                        // If the action is "REFUSE" or "CHAT", we do nothing to the story canvas.

                        // Always add the AI's conversational message to the chat
                        addMessageToChat(storyResponse.chatResponse, StoryElement.TYPE_AI);
                    });
                } else {
                    Log.e("MainActivity", "Story API Error: " + response.code());
                    Toast.makeText(MainActivity.this, "AI failed to respond.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<StoryResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Story API Failure", t);
                Toast.makeText(MainActivity.this, "Network error. AI unreachable.", Toast.LENGTH_SHORT).show();
                runOnUiThread(() -> {
                    addMessageToChat("This is a test response from the AI.", StoryElement.TYPE_AI);
                    animateTypewriter("This is a test paragraph to see the typewriter effect working.");
                });
            }
        });
    }

    private void animateTypewriter(final String text) {
        storyBuilder.append("\n\n");
        final int[] index = {0};
        typewriterHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    storyBuilder.append(text.charAt(index[0]));
                    storyTextView.setText(storyBuilder);
                    index[0]++;
                    typewriterHandler.postDelayed(this, 5);
                }
            }
        }, 20);
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
            }
        }
    }

    private void transcribeAudioFile(String filePath) {
        File audioFile = new File(filePath);
        String authToken = "Bearer " + BuildConfig.HUGGING_FACE_API_KEY;

        if (BuildConfig.HUGGING_FACE_API_KEY == null || BuildConfig.HUGGING_FACE_API_KEY.isEmpty()) {
            Toast.makeText(this, "Hugging Face API Key not found.", Toast.LENGTH_LONG).show();
            return;
        }

        RequestBody audioRequestBody = RequestBody.create(MediaType.parse("audio/mp3"), audioFile);
        HuggingFaceApiService hfApiService = HuggingFaceRetrofitClient.getApiService();

        Call<WhisperResponse> call = hfApiService.transcribeAudio(authToken, audioRequestBody);
        call.enqueue(new Callback<WhisperResponse>() {
            @Override
            public void onResponse(@NonNull Call<WhisperResponse> call, @NonNull Response<WhisperResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String transcribedText = response.body().text;
                    runOnUiThread(() -> {
                        addMessageToChat(transcribedText, StoryElement.TYPE_USER);
                        sendStoryContinuationRequest(transcribedText);
                    });
                } else {
                    try {
                        Log.e("MainActivity", "HF API Error: " + response.code() + " - " + response.errorBody().string());
                    } catch (IOException e) {
                        Log.e("MainActivity", "HF API Error: " + response.code());
                    }
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

    // --- Story Saving Logic ---
    private void saveStory() {
        String fullStoryText = storyBuilder.toString();

        if (fullStoryText.trim().equals("The " + genre + " story begins...")) {
            Toast.makeText(this, "Cannot save an empty story.", Toast.LENGTH_SHORT).show();
            return;
        }

        Chapter chapter1 = new Chapter(1, "Chapter 1", fullStoryText);
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(chapter1);

        FullStoryCreate storyToSave = new FullStoryCreate(
                "My New " + genre + " Story",
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
                    Story savedStory = response.body();
                    Toast.makeText(MainActivity.this, "Story saved! Generating cover...", Toast.LENGTH_SHORT).show();
                    triggerCoverGeneration(savedStory.id);
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
    private void triggerCoverGeneration(int storyId) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<Story> call = apiService.generateCover(storyId);

        call.enqueue(new Callback<Story>() {
            @Override
            public void onResponse(Call<Story> call, Response<Story> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this, "Cover generated successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Log.e("MainActivity", "Cover Gen Error: " + response.message());
                    Toast.makeText(MainActivity.this, "Could not generate cover.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Story> call, Throwable t) {
                Log.e("MainActivity", "Cover Gen Failed", t);
                Toast.makeText(MainActivity.this, "Network failure while generating cover.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void exportStoryAsPdf() {
        // Allow network operations on the main thread for image downloading
        // Note: In a production app, this should be done in a background thread.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String fullStoryText = storyBuilder.toString();
        if (fullStoryText.trim().equals("The " + genre + " story begins...")) {
            Toast.makeText(this, "Cannot export an empty story.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "ScrybeStory_" + timeStamp + ".pdf";
            File pdfDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }

            File pdfFile = new File(pdfDir, fileName);

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // --- Add Cover Image ---
            // This part needs the story object, which we don't have here.
            // For now, we will skip adding the cover image directly to the PDF from this screen.
            // A more advanced implementation would pass the full story object to this activity.

            // --- Add Title and Chapters with Formatting ---
            Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 22, Font.BOLD);
            Font chapterFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            Font bodyFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);

            document.add(new Paragraph("My New " + genre + " Story", titleFont));

            // Split the story into chapters based on our delimiter
            String[] chapters = fullStoryText.split("\n\n--- ");

            for (int i = 0; i < chapters.length; i++) {
                String chapterContent = chapters[i];
                if (i > 0) { // The first element is the beginning of the story
                    String[] chapterParts = chapterContent.split(" ---\n\n");
                    String chapterTitle = chapterParts[0];
                    document.add(new Paragraph("\n\n" + chapterTitle, chapterFont));
                    if (chapterParts.length > 1) {
                        document.add(new Paragraph(chapterParts[1], bodyFont));
                    }
                } else {
                    document.add(new Paragraph(chapterContent, bodyFont));
                }
            }

            document.close();

            Uri pdfUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Story PDF"));

        } catch (Exception e) {
            Log.e("MainActivity", "Error creating or sharing PDF", e);
            Toast.makeText(this, "Could not create or share PDF.", Toast.LENGTH_SHORT).show();
        }
    }

}