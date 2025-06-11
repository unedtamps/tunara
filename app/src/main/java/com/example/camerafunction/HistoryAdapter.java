package com.example.camerafunction;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<PhotoItem> photoItems;
    private final OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Uri photoUri);
    }

    public HistoryAdapter(Context context, List<PhotoItem> photoItems, OnPhotoClickListener listener) {
        this.context = context;
        this.photoItems = photoItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the new card layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_card, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoItem item = photoItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return photoItems.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView dateTextView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.historyImageView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        public void bind(final PhotoItem item, final OnPhotoClickListener listener) {
            // Load image using Glide
            Glide.with(itemView.getContext())
                    .load(item.getUri())
                    .into(imageView);

            // Format and set the date
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            dateTextView.setText(sdf.format(new Date(item.getTimestamp())));

            // Set click listener
            itemView.setOnClickListener(v -> listener.onPhotoClick(item.getUri()));
        }
    }
}
