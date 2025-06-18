package com.example.camerafunction;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PhotoHistoryManager {
    private static final String PREFS_NAME = "PhotoHistoryPrefs";
    private static final String KEY_PHOTO_URIS = "photo_uris";
    private static final String INFERRED_IMAGES_DIR = "inferred_images"; // Direktori untuk menyimpan gambar hasil inferensi

    /**
     * Menyimpan Bitmap ke penyimpanan internal aplikasi dan mengembalikan Uri-nya.
     * @param context Context aplikasi.
     * @param bitmap Bitmap yang akan disimpan.
     * @return Uri dari file yang baru disimpan, atau null jika gagal.
     */
    public static Uri saveInferredImageAndGetUri(Context context, Bitmap bitmap) {
        File directory = new File(context.getFilesDir(), INFERRED_IMAGES_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "INFERRED_" + timeStamp + ".jpg";
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            // Mengembalikan Uri dari file yang baru dibuat, bukan dari MediaStore
            return Uri.fromFile(file);
        } catch (IOException e) {
            Log.e("PhotoHistoryManager", "Error saving inferred image", e);
            return null;
        }
    }

    /**
     * Menambahkan Uri foto ke dalam daftar riwayat di SharedPreferences.
     * @param context Context aplikasi.
     * @param uri Uri yang akan ditambahkan.
     */
    public static void addPhotoUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriSet = new HashSet<>(prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>()));
        uriSet.add(uri.toString());
        prefs.edit().putStringSet(KEY_PHOTO_URIS, uriSet).apply();
    }

    /**
     * Mengambil daftar riwayat foto. Sekarang dapat menangani Uri dari galeri dan file internal.
     * @param context Context aplikasi.
     * @return Daftar PhotoItem yang sudah diurutkan berdasarkan tanggal.
     */
    public static List<PhotoItem> getPhotoHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> uriStrings = prefs.getStringSet(KEY_PHOTO_URIS, new HashSet<>());
        List<PhotoItem> photoItems = new ArrayList<>();

        for (String uriString : uriStrings) {
            Uri uri = Uri.parse(uriString);
            long timestamp = 0;

            String scheme = uri.getScheme();
            if ("content".equals(scheme)) {
                // Menangani content Uri dari galeri atau kamera
                String[] projection = {MediaStore.Images.Media.DATE_TAKEN};
                try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                        timestamp = cursor.getLong(dateColumn);
                    }
                } catch (Exception e) {
                    timestamp = System.currentTimeMillis(); // Fallback
                }
            } else if ("file".equals(scheme)) {
                // Menangani file Uri dari gambar yang kita simpan
                File file = new File(uri.getPath());
                if (file.exists()) {
                    timestamp = file.lastModified();
                }
            }
            if (timestamp == 0) timestamp = System.currentTimeMillis(); // Fallback terakhir

            photoItems.add(new PhotoItem(uri, timestamp));
        }

        // --- MODIFIKASI ADA DI SINI ---
        // Mengurutkan list berdasarkan timestamp secara descending (dari yang terbesar ke terkecil).
        // Ini akan menempatkan foto terbaru di paling atas.
        Collections.sort(photoItems, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        return photoItems;
    }
}
