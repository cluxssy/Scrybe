package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final Context context;
    private List<Story> storyList;
    private final OnStoryClickListener listener;

    // Interface for click events
    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public StoryAdapter(Context context, List<Story> storyList, OnStoryClickListener listener) {
        this.context = context;
        this.storyList = storyList;
        this.listener = listener;
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
        holder.bind(currentStory, listener);
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    public void setStories(List<Story> stories) {
        this.storyList = stories;
        notifyDataSetChanged();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView genreTextView;
        ImageView coverImageView;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.storyTitleTextView);
            genreTextView = itemView.findViewById(R.id.storyGenreTextView);
            coverImageView = itemView.findViewById(R.id.storyCoverImageView);
        }

        public void bind(final Story story, final OnStoryClickListener listener) {
            titleTextView.setText(story.title);
            genreTextView.setText(story.genre);

            if (story.cover_image_url != null && !story.cover_image_url.isEmpty()) {
                Picasso.get()
                        .load(story.cover_image_url)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(coverImageView);
            } else {
                coverImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            itemView.setOnClickListener(v -> listener.onStoryClick(story));
        }
    }
}