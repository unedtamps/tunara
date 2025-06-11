package com.example.camerafunction;

import android.net.Uri;

// Data class to hold both the Uri and the timestamp of a photo
public class PhotoItem {
    private final Uri uri;
    private final long timestamp;

    public PhotoItem(Uri uri, long timestamp) {
        this.uri = uri;
        this.timestamp = timestamp;
    }

    public Uri getUri() {
        return uri;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
