package com.example.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso; // Import Picasso
import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private Context context;
    private List<Story> storyList;

    public StoryAdapter(Context context) {
        this.context = context;
        this.storyList = new ArrayList<>();
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story currentStory = storyList.get(position);
        holder.titleTextView.setText(currentStory.title);
        holder.genreTextView.setText(currentStory.genre);

        // --- NEW: Use Picasso to load the cover image ---
        if (currentStory.cover_image_url != null && !currentStory.cover_image_url.isEmpty()) {
            Picasso.get()
                    .load(currentStory.cover_image_url)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Show placeholder while loading
                    .error(android.R.drawable.ic_menu_report_image) // Show error image if it fails
                    .into(holder.coverImageView);
        } else {
            // Set a default placeholder if there is no URL
            holder.coverImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    public void setStories(List<Story> stories) {
        this.storyList = stories;
        notifyDataSetChanged();
    }

    // UPDATED: The ViewHolder now includes the ImageView
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView genreTextView;
        ImageView coverImageView; // Add the ImageView

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.storyTitleTextView);
            genreTextView = itemView.findViewById(R.id.storyGenreTextView);
            coverImageView = itemView.findViewById(R.id.storyCoverImageView); // Find the ImageView
        }
    }
}