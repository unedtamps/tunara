package com.example.camerafunction;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// Helper class to manage saving and loading photo URIs
public class PhotoHistoryManager {
    private static final String PREFS_NAME = "PhotoHistoryPrefs";
    private static final String KEY_PHOTO_URIS = "photo_uris";

    public static void addPhotoUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriSet = new HashSet<>(prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>()));
        uriSet.add(uri.toString());
        prefs.edit().putStringSet(KEY_PHOTO_URIS, uriSet).apply();
    }

    public static Set<Uri> getPhotoUris(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriStrings = prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>());
        return uriStrings.stream().map(Uri::parse).collect(Collectors.toSet());
    }
}
