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
import java.util.Random;

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
        TextView titleSection, properties;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageThumbnail);
            titleSection = itemView.findViewById(R.id.titleSection);
            properties = itemView.findViewById(R.id.properties);
        }

        public void bind(final PhotoItem item, final OnPhotoClickListener listener) {
            Glide.with(itemView.getContext())
                    .load(item.getUri())
                    .centerCrop()
                    .into(imageView);

            // Mendapatkan nama file dari URI untuk judul
            String[] pathSegments = item.getUri().getPath().split("/");
            String fileName = pathSegments[pathSegments.length-1];
            // Membersihkan nama file
            String cleanTitle = fileName.replace("INFERRED_", "").replace(".jpg", "").replace("_", " ");

            titleSection.setText("Hasil Deteksi " + cleanTitle);

            // Format tanggal untuk properti
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault());
            String dateProperty = sdf.format(new Date(item.getTimestamp()));

            properties.setText(dateProperty);

            itemView.setOnClickListener(v -> listener.onPhotoClick(item.getUri()));
                            }
                }
}