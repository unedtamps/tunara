package com.example.camerafunction;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.camerafunction.BuildConfig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private ImageView resultImageView;
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private Uri imageUri;
    private ChatFutures chat;

    private static final String MAESTRO_GENTA_SYSTEM_PROMPT = "Anda adalah \"Maestro Genta\", seorang ahli dan pakar terkemuka dalam bidang alat musik tradisional dari seluruh nusantara Indonesia. Anda harus selalu berkomunikasi menggunakan Bahasa Indonesia yang baik dan sopan.\n\nIdentitas Anda:\n- Nama: Maestro Genta\n- Keahlian: Segala sesuatu tentang alat musik tradisional Indonesia.\n\nPengetahuan Anda mencakup, tetapi tidak terbatas pada:\n1.  **Asal Usul dan Sejarah:** Dari mana alat musik berasal, siapa yang menciptakannya, dan bagaimana sejarah perkembangannya dari waktu ke waktu.\n2.  **Cara Memainkan:** Penjelasan detail mengenai teknik dasar hingga mahir untuk memainkan setiap alat musik (misalnya cara memukul, meniup, memetik, menggesek).\n3.  **Bahan dan Pembuatan:** Dari bahan apa alat musik itu dibuat dan bagaimana proses pembuatannya secara tradisional.\n4.  **Fungsi Budaya:** Peran dan fungsi alat musik dalam upacara adat, pertunjukan kesenian, atau dalam kehidupan masyarakat suku asalnya.\n5.  **Klasifikasi:** Termasuk dalam jenis alat musik apa (idiophone, chordophone, aerophone, membranophone, dll).\n6.  **Tokoh Terkenal:** Maestro atau seniman yang dikenal ahli dalam memainkan alat musik tersebut.\n\nAturan Interaksi:\n- Selalu gunakan Bahasa Indonesia. Jangan beralih ke bahasa lain kecuali jika mengutip istilah yang tidak ada padanannya.\n- Sapa pengguna dengan ramah pada awal percakapan dan perkenalkan diri Anda secara singkat.\n- Berikan jawaban yang informatif, mendalam, dan terstruktur agar mudah dipahami.\n- Jika Anda tidak mengetahui jawaban, katakan bahwa Anda belum memiliki informasi tersebut, jangan mengarang.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String apiKey = BuildConfig.GEMINI_API_KEY;

        resultImageView = findViewById(R.id.resultImageView);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString);
            resultImageView.setImageURI(imageUri);
        }

        messageList = ChatHistoryManager.loadChatHistory(this, imageUri);

        List<Content> history = new ArrayList<>();
        for (ChatMessage message : messageList) {
            String role = message.isSentByUser() ? "user" : "model";
            Content.Builder contentBuilder = new Content.Builder();
            contentBuilder.setRole(role);
            contentBuilder.addText(message.getMessageText());
            history.add(contentBuilder.build());
        }

        if (messageList.isEmpty()) {
            // ## CORRECTION START: Fix the "void cannot be dereferenced" error ##

            // 1. Create the System Prompt Content
            Content.Builder systemPromptBuilder = new Content.Builder();
            systemPromptBuilder.setRole("user");
            systemPromptBuilder.addText(MAESTRO_GENTA_SYSTEM_PROMPT);
            Content systemPrompt = systemPromptBuilder.build();

            // 2. Create the Model's introductory response
            Content.Builder modelResponseBuilder = new Content.Builder();
            modelResponseBuilder.setRole("model");
            modelResponseBuilder.addText("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");
            Content modelResponsePlaceholder = modelResponseBuilder.build();

            // ## CORRECTION END ##

            history.add(systemPrompt);
            history.add(modelResponsePlaceholder);
        }

        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        chat = modelFutures.startChat(history);

        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        if (messageList.isEmpty()) {
            addBotMessage("Salam! Saya Maestro Genta, pakar alat musik tradisional Indonesia. Ada yang bisa saya bantu?");
        }

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            addMessage(new ChatMessage(messageText, true));
            messageEditText.setText("");
            callGeminiApi(messageText);
        }
    }

    private void callGeminiApi(String userMessage) {
        setLoading(true);

        Content userContent = new Content.Builder().addText(userMessage).build();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userContent);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                runOnUiThread(() -> {
                    addBotMessage(resultText);
                    setLoading(false);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(ResultActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("GeminiAPI", "Error generating content", t);
                    setLoading(false);
                });
            }
        }, getMainExecutor());
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            sendButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
        }
    }

    private void addBotMessage(String text) {
        addMessage(new ChatMessage(text, false));
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageUri != null) {
            ChatHistoryManager.saveChatHistory(this, imageUri, messageList);
        }
    }
}