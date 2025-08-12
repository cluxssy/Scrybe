package com.example.app;

// In StoryAdapter.java

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private Context context;
    private List<Story> storyList;

    // Constructor
    public StoryAdapter(Context context) {
        this.context = context;
        this.storyList = new ArrayList<>(); // Start with an empty list
    }

    // This method is called when a new view holder is needed.
    // It inflates our list_item_story.xml layout for a single row.
    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_story, parent, false);
        return new StoryViewHolder(view);
    }

    // This method is called to display the data for a specific item in the list.
    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story currentStory = storyList.get(position);
        holder.titleTextView.setText(currentStory.title);
        holder.genreTextView.setText(currentStory.genre);
    }

    // This method returns the total number of items in our list.
    @Override
    public int getItemCount() {
        return storyList.size();
    }

    // A helper method to update the adapter's data and refresh the list
    public void setStories(List<Story> stories) {
        this.storyList = stories;
        notifyDataSetChanged(); // This tells the RecyclerView to redraw itself
    }


    // The ViewHolder class holds the UI elements for a single list item.
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView genreTextView;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.storyTitleTextView);
            genreTextView = itemView.findViewById(R.id.storyGenreTextView);
        }
    }
}