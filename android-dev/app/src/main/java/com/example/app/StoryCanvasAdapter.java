package com.example.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StoryCanvasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<StoryElement> storyElements;

    public StoryCanvasAdapter(List<StoryElement> storyElements) {
        this.storyElements = storyElements;
    }

    @Override
    public int getItemViewType(int position) {
        return storyElements.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case StoryElement.TYPE_USER:
                View userView = inflater.inflate(R.layout.item_story_user, parent, false);
                return new UserViewHolder(userView);
            case StoryElement.TYPE_AI:
                View aiView = inflater.inflate(R.layout.item_story_ai, parent, false);
                return new AiViewHolder(aiView);
            case StoryElement.TYPE_CHAPTER:
                View chapterView = inflater.inflate(R.layout.item_story_chapter, parent, false);
                return new ChapterViewHolder(chapterView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StoryElement element = storyElements.get(position);
        switch (holder.getItemViewType()) {
            case StoryElement.TYPE_USER:
                ((UserViewHolder) holder).userTextView.setText(element.text);
                break;
            case StoryElement.TYPE_AI:
                ((AiViewHolder) holder).aiTextView.setText(element.text);
                break;
            case StoryElement.TYPE_CHAPTER:
                ((ChapterViewHolder) holder).chapterTextView.setText(element.text);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return storyElements.size();
    }

    // ViewHolder for User messages
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userTextView;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userTextView = itemView.findViewById(R.id.userTextView);
        }
    }

    // ViewHolder for AI messages
    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView aiTextView;
        public AiViewHolder(@NonNull View itemView) {
            super(itemView);
            aiTextView = itemView.findViewById(R.id.aiTextView);
        }
    }

    // ViewHolder for Chapter titles
    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTextView;
        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTextView = itemView.findViewById(R.id.chapterTextView);
        }
    }
}