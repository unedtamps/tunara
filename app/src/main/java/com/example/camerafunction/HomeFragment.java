package com.example.camerafunction;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private List<PhotoItem> photoItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        // Use a LinearLayoutManager for a vertical list of cards
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Set up the adapter with the listener
        historyAdapter = new HistoryAdapter(requireContext(), photoItems, photoUri -> {
            Intent intent = new Intent(requireActivity(), ResultActivity.class);
            intent.putExtra("image_uri", photoUri.toString());
            startActivity(intent);
        });

        historyRecyclerView.setAdapter(historyAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list when the fragment becomes visible again
        loadPhotoHistory();
    }

    private void loadPhotoHistory() {
        // Clear the old list and load the new one
        photoItems.clear();
        photoItems.addAll(PhotoHistoryManager.getPhotoHistory(requireContext()));
        // Notify the adapter that the data has changed
        historyAdapter.notifyDataSetChanged();
    }
}
