package com.example.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewerActivity extends AppCompatActivity {

    private ImageView coverImageView;
    private TextView titleTextView;
    private TextView storyContentTextView;
    private Story currentStory; // Declared as a class member

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

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
                        currentStory = response.body();
                        titleTextView.setText(currentStory.title);

                        if (currentStory.cover_image_url != null && !currentStory.cover_image_url.isEmpty()) {
                            Picasso.get().load(currentStory.cover_image_url).into(coverImageView);
                        }

                        StringBuilder chaptersText = new StringBuilder();
                        if (currentStory.chapters != null) {
                            for (Chapter chapter : currentStory.chapters) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.story_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_export_pdf) {
            if (currentStory != null) {
                exportStoryAsPdf();
            } else {
                Toast.makeText(this, "Story data not loaded yet.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportStoryAsPdf() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String fullStoryText = storyContentTextView.getText().toString();

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

            Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 22, Font.BOLD);
            Font bodyFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);

            document.add(new Paragraph(currentStory.title, titleFont));
            document.add(new Paragraph(fullStoryText, bodyFont));

            document.close();

            Uri pdfUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Story PDF"));

        } catch (Exception e) {
            Log.e("StoryViewerActivity", "Error creating or sharing PDF", e);
            Toast.makeText(this, "Could not create or share PDF.", Toast.LENGTH_SHORT).show();
        }
    }
}