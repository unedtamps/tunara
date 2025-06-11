package com.example.camerafunction;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.PhotoViewHolder> {

    private Context context;
    private List<Uri> photoUris;

    public interface OnPhotoClickListener {
        void onPhotoClick(Uri photoUri);
    }

    private final OnPhotoClickListener listener;

    public HistoryAdapter(Context context, List<Uri> photoUris, OnPhotoClickListener listener) {
        this.context = context;
        this.photoUris = photoUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);
        Glide.with(context)
                .load(uri)
                .centerCrop()
                .into(holder.imageView);
        holder.bind(uri, listener);
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.historyImageView);
        }

        public void bind(final Uri photoUri, final OnPhotoClickListener listener) {
            itemView.setOnClickListener(v -> listener.onPhotoClick(photoUri));
        }
    }
}
