package com.example.camerafunction;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.camerafunction.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    private OkHttpClient okHttpClient;
    private Gson gson;
    private ExecutorService executorService = Executors.newSingleThreadExecutor(); // For background tasks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String apiKey = BuildConfig.GEMINI_API_KEY; // Gemini API Key
        String roboflowApiKey = BuildConfig.RF_API;
        String roboflowModelUrl = "https://classify.roboflow.com/ppbalatmusiktrad-a5usu/3";

        okHttpClient = new OkHttpClient();
        gson = new Gson();

        resultImageView = findViewById(R.id.resultImageView);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString);
            resultImageView.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "No image to display.", Toast.LENGTH_SHORT).show();
            setLoading(false);
        }

        // Load chat history first
        messageList = ChatHistoryManager.loadChatHistory(this, imageUri);

        // Initialize chat adapter and layout manager
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Only perform image detection and initialize Gemini chat with detection results if it's a new chat (history is empty)
        if (messageList.isEmpty()) {
            if (imageUri != null) { // Only detect if an image is available
                detectObjectsWithRoboflow(imageUri, roboflowModelUrl, roboflowApiKey);
            } else {
                // If no image and no history, just initialize Gemini without detection context
                initializeGeminiChat(null);
            }
        } else {
            // If history exists, just initialize Gemini chat with the loaded history
            initializeGeminiChatFromHistory();
        }

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void detectObjectsWithRoboflow(Uri imageUri, String modelUrl, String apiKey) {
        setLoading(true); // Show progress bar during detection

        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to open image.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        initializeGeminiChat(null); // Initialize Gemini even if detection fails
                    });
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                // Compress to JPEG. Adjust quality as needed (e.g., 80 for good balance)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                RequestBody body = RequestBody.create(base64Image, mediaType);

                Request request = new Request.Builder()
                        .url(modelUrl + "?api_key=" + apiKey)
                        .post(body)
                        .build();

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ResultActivity.this, "Roboflow API Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("RoboflowAPI", "Error calling Roboflow API", e);
                            setLoading(false);
                            initializeGeminiChat(null); // Initialize Gemini even if detection fails
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            RoboflowDetectionResponse detectionResponse = gson.fromJson(responseBody, RoboflowDetectionResponse.class);

                            runOnUiThread(() -> {
                                setLoading(false);
                                processDetectionResults(detectionResponse, bitmap);
                                initializeGeminiChat(detectionResponse); // Initialize Gemini with detection results
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(ResultActivity.this, "Roboflow API Error: " + response.code() + " - " + response.message(), Toast.LENGTH_LONG).show();
                                Log.e("RoboflowAPI", "Roboflow API returned error: " + response.code() + " - " + response.message());
                                setLoading(false);
                                initializeGeminiChat(null); // Initialize Gemini even if detection fails
                            });
                        }
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Image processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ImageProcessing", "Error processing image for Roboflow", e);
                    setLoading(false);
                    initializeGeminiChat(null); // Initialize Gemini even if detection fails
                });
            }
        });
    }

    private void processDetectionResults(RoboflowDetectionResponse detectionResponse, Bitmap originalBitmap) {
        if (detectionResponse != null && detectionResponse.getPredictions() != null && !detectionResponse.getPredictions().isEmpty()) {
            // Draw bounding boxes on the image
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();

            // Mengatur properti untuk Bounding Box
            paint.setColor(Color.YELLOW); // Ganti warna menjadi KUNING
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8); // Mungkin Anda ingin membuat garis lebih tebal agar lebih menonjol

            // Mengatur properti untuk Teks (nama kelas)
            paint.setTextSize(40); // Perbesar ukuran teks agar lebih jelas
            paint.setColor(Color.BLACK); // Ganti warna teks menjadi HITAM agar kontras dengan kuning
            paint.setFakeBoldText(true); // Membuat teks menjadi tebal
            paint.setShadowLayer(5f, 0f, 0f, Color.WHITE); // Tambahkan efek bayangan putih agar teks lebih menonjol

            for (Prediction prediction : detectionResponse.getPredictions()) {
                float x = (float) prediction.getX();
                float y = (float) prediction.getY();
                float width = (float) prediction.getWidth();
                float height = (float) prediction.getHeight();
                String className = prediction.getClassName();

                // Calculate bounding box coordinates (Roboflow returns center_x, center_y, width, height)
                float left = x - (width / 2);
                float top = y - (height / 2);
                float right = x + (width / 2);
                float bottom = y + (height / 2);

                // Gambar Bounding Box (gunakan paint yang sudah diatur untuk box)
                // Penting: atur lagi warna untuk garis box sebelum menggambar box jika Anda mengatur warna teks di atas
                paint.setColor(Color.YELLOW); // Setel ulang warna ke kuning untuk garis box
                canvas.drawRect(left, top, right, bottom, paint);

                // Gambar Teks (gunakan paint yang sudah diatur untuk teks)
                paint.setColor(Color.BLACK); // Setel ulang warna ke hitam untuk teks
                canvas.drawText(className, left + 10, top + 40, paint); // Sesuaikan posisi teks agar tidak terlalu dekat dengan box
            }
            resultImageView.setImageBitmap(mutableBitmap);
        } else {
            // If no detections or detection failed, just display the original image
            resultImageView.setImageURI(imageUri);
            Toast.makeText(this, "No musical instruments detected.", Toast.LENGTH_SHORT).show();
        }
    }
    private void initializeGeminiChatFromHistory() {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        List<Content> history = new ArrayList<>();
        Content.Builder systemPromptBuilder = new Content.Builder();
        systemPromptBuilder.setRole("user"); // System prompts are typically given as a user turn
        systemPromptBuilder.addText(MAESTRO_GENTA_SYSTEM_PROMPT); // Call addText
        Content systemPromptContent = systemPromptBuilder.build(); // Then call build
        history.add(systemPromptContent);

        Content.Builder initialGreetingBuilder = new Content.Builder(); // Declare a new builder
        initialGreetingBuilder.setRole("model");
        initialGreetingBuilder.addText("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");
        Content initialGreetingContent = initialGreetingBuilder.build(); // Then call build
        history.add(initialGreetingContent);


        for (ChatMessage message : messageList) {
            if (message.getMessageText().startsWith("Sistem:") ||
                    message.getMessageText().equals("Terima kasih atas informasinya. Saya siap menjawab pertanyaan Anda mengenai alat musik yang terdeteksi!") ||
                    message.getMessageText().equals("Baik, meskipun tidak ada alat musik yang terdeteksi, saya tetap siap membantu Anda dengan informasi seputar alat musik tradisional Indonesia lainnya. Silakan bertanya!")) {
                continue; // Skip these messages from being added to Gemini's actual history
            }

            String role = message.isSentByUser() ? "user" : "model";
            Content.Builder contentBuilder = new Content.Builder();
            contentBuilder.setRole(role);
            contentBuilder.addText(message.getMessageText());
            history.add(contentBuilder.build());
        }

        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        chat = modelFutures.startChat(history);
    }


    private void initializeGeminiChat(RoboflowDetectionResponse detectionResponse) {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        List<Content> history = new ArrayList<>();

        // Add Maestro Genta System Prompt (user role, NOT displayed to user)
        Content.Builder systemPromptBuilder = new Content.Builder();
        systemPromptBuilder.setRole("user");
        systemPromptBuilder.addText(MAESTRO_GENTA_SYSTEM_PROMPT);
        history.add(systemPromptBuilder.build());

        // Add Maestro Genta's initial greeting (model role, WILL be displayed to user)
        Content.Builder modelGreetingBuilder = new Content.Builder();
        modelGreetingBuilder.setRole("model");
        modelGreetingBuilder.addText("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");
        history.add(modelGreetingBuilder.build());
        // Also add this to the UI's messageList
        addBotMessage("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");


        // Add detection results as a "context" message from the user role
        // This is sent to the Gemini model for context but NOT explicitly displayed as a user message in chat.
        if (detectionResponse != null && detectionResponse.getPredictions() != null && !detectionResponse.getPredictions().isEmpty()) {
            // Extract unique class names
            List<String> detectedInstruments = detectionResponse.getPredictions().stream()
                    .map(Prediction::getClassName)
                    .distinct() // Get only unique instrument names
                    .collect(Collectors.toList());

            String detectionMessageForGemini;
            if (detectedInstruments.size() == 1) {
                detectionMessageForGemini = "Saya telah mendeteksi sebuah alat musik gamelan **" + detectedInstruments.get(0) + "** pada gambar.";
            } else {
                detectionMessageForGemini = "Pada gambar, saya mendeteksi beberapa alat musik gamelan seperti: " +
                        String.join(", ", detectedInstruments) + ".";
            }
            addBotMessage("Sistem mendeteksi: " + detectionMessageForGemini); // Teks ini akan terlihat oleh user

            Content.Builder detectionContextBuilder = new Content.Builder();
            detectionContextBuilder.setRole("user");
            detectionContextBuilder.addText("Berikut adalah hasil deteksi gambar: " + detectionMessageForGemini);
            history.add(detectionContextBuilder.build());

            // Add the model's acknowledgement (model role, WILL be displayed to user)
            Content.Builder modelAcknowledgementBuilder = new Content.Builder();
            modelAcknowledgementBuilder.setRole("model");
            modelAcknowledgementBuilder.addText("Terima kasih atas informasinya. Saya siap menjawab pertanyaan Anda mengenai alat musik yang terdeteksi!");
            history.add(modelAcknowledgementBuilder.build());
            // Also add this to the UI's messageList
            addBotMessage("Terima kasih atas informasinya. Saya siap menjawab pertanyaan Anda mengenai alat musik yang terdeteksi!");

        } else {
            // If no instruments were detected, add context to Gemini and display a bot message
            Content.Builder noDetectionMessageBuilder = new Content.Builder();
            noDetectionMessageBuilder.setRole("user");
            noDetectionMessageBuilder.addText("Tidak ada alat musik tradisional Indonesia yang terdeteksi pada gambar.");
            history.add(noDetectionMessageBuilder.build());

            Content.Builder modelNoDetectionAcknowledgementBuilder = new Content.Builder();
            modelNoDetectionAcknowledgementBuilder.setRole("model");
            modelNoDetectionAcknowledgementBuilder.addText("Baik, meskipun tidak ada alat musik yang terdeteksi, saya tetap siap membantu Anda dengan informasi seputar alat musik tradisional Indonesia lainnya. Silakan bertanya!");
            history.add(modelNoDetectionAcknowledgementBuilder.build());
            // Also add this to the UI's messageList
            addBotMessage("Baik, meskipun tidak ada alat musik yang terdeteksi, saya tetap siap membantu Anda dengan informasi seputar alat musik tradisional Indonesia lainnya. Silakan bertanya!");
        }

        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        chat = modelFutures.startChat(history);
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
            messageEditText.setEnabled(false); // Disable input while loading
        } else {
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
            messageEditText.setEnabled(true); // Enable input after loading
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Shutdown the thread pool
    }
}