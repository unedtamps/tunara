package com.example.camerafunction;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoHistoryManager {
    private static final String PREFS_NAME = "PhotoHistoryPrefs";
    private static final String KEY_PHOTO_URIS = "photo_uris";

    public static void addPhotoUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriSet = new HashSet<>(prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>()));
        uriSet.add(uri.toString());
        prefs.edit().putStringSet(KEY_PHOTO_URIS, uriSet).apply();
    }

    public static List<PhotoItem> getPhotoHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriStrings = prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>());
        List<PhotoItem> photoItems = new ArrayList<>();

        String[] projection = {MediaStore.Images.Media.DATE_TAKEN};

        for (String uriString : uriStrings) {
            Uri uri = Uri.parse(uriString);
            long timestamp = 0;
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                    timestamp = cursor.getLong(dateColumn);
                }
            } catch (Exception e) {
                timestamp = System.currentTimeMillis(); // Fallback to now
            }
            photoItems.add(new PhotoItem(uri, timestamp));
        }

        Collections.sort(photoItems, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        return photoItems;
    }
}
