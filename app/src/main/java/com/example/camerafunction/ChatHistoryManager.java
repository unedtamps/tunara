package com.example.camerafunction;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {

    private static final String TAG = "ChatHistoryManager";
    private static final String CHAT_HISTORY_DIR = "chat_history";

    // Generates a unique, safe filename from a URI
    private static String getFileNameForUri(Uri imageUri) {
        if (imageUri == null || imageUri.getLastPathSegment() == null) {
            return "chat_" + System.currentTimeMillis();
        }
        return "chat_" + imageUri.getLastPathSegment().replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static void saveChatHistory(Context context, Uri imageUri, List<ChatMessage> messages) {
        File directory = new File(context.getFilesDir(), CHAT_HISTORY_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, getFileNameForUri(imageUri));
        JSONArray jsonArray = new JSONArray();
        for (ChatMessage message : messages) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("text", message.getMessageText());
                jsonObject.put("isSentByUser", message.isSentByUser());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON for message", e);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonArray.toString().getBytes());
        } catch (Exception e) {
            Log.e(TAG, "Error saving chat history", e);
        }
    }

    public static List<ChatMessage> loadChatHistory(Context context, Uri imageUri) {
        List<ChatMessage> messages = new ArrayList<>();
        File directory = new File(context.getFilesDir(), CHAT_HISTORY_DIR);
        File file = new File(directory, getFileNameForUri(imageUri));

        if (!file.exists()) {
            return messages; // No history yet, return empty list
        }

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(isr)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String text = jsonObject.getString("text");
                boolean isSentByUser = jsonObject.getBoolean("isSentByUser");
                messages.add(new ChatMessage(text, isSentByUser));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading chat history", e);
        }

        return messages;
    }
}
