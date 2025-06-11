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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3)); // 3 columns in grid

        List<Uri> photoUris = new ArrayList<>(PhotoHistoryManager.getPhotoUris(requireContext()));

        historyAdapter = new HistoryAdapter(requireContext(), photoUris, photoUri -> {
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
        List<Uri> photoUris = new ArrayList<>(PhotoHistoryManager.getPhotoUris(requireContext()));
        historyAdapter = new HistoryAdapter(requireContext(), photoUris, photoUri -> {
            Intent intent = new Intent(requireActivity(), ResultActivity.class);
            intent.putExtra("image_uri", photoUri.toString());
            startActivity(intent);
        });
        historyRecyclerView.setAdapter(historyAdapter);
    }
}
