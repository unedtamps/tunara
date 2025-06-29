package com.example.camerafunction;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html; // Import ditambahkan untuk memformat teks
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;
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

    private PhotoView resultImageView;
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
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String apiKey = BuildConfig.GEMINI_API_KEY;
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

        boolean isFromHistory = imageUri != null && "file".equals(imageUri.getScheme());

        if (isFromHistory) {
            messageList = ChatHistoryManager.loadChatHistory(this, imageUri);
            initializeGeminiChatFromHistory();
        } else if (imageUri != null) {
            messageList = new ArrayList<>();
            detectObjectsWithRoboflow(imageUri, roboflowModelUrl, roboflowApiKey);
        } else {
            messageList = new ArrayList<>();
            initializeGeminiChat(null);
        }

        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void detectObjectsWithRoboflow(Uri imageUri, String modelUrl, String apiKey) {
        setLoading(true);

        executorService.execute(() -> {
            try {
                Bitmap bitmap = getCorrectlyOrientedBitmap(imageUri);

                if (bitmap == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        initializeGeminiChat(null);
                    });
                    return;
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
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
                            setLoading(false);
                            processDetectionResults(null, bitmap);
                            initializeGeminiChat(null);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();
                        if (response.isSuccessful()) {
                            RoboflowDetectionResponse detectionResponse = gson.fromJson(responseBody, RoboflowDetectionResponse.class);
                            runOnUiThread(() -> {
                                setLoading(false);
                                processDetectionResults(detectionResponse, bitmap);
                                initializeGeminiChat(detectionResponse);
                            });
                        } else {
                            runOnUiThread(() -> {
                                setLoading(false);
                                processDetectionResults(null, bitmap);
                                initializeGeminiChat(null);
                            });
                        }
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    processDetectionResults(null, null);
                    initializeGeminiChat(null);
                });
            }
        });
    }

    private Bitmap getCorrectlyOrientedBitmap(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        if (inputStream == null) return null;

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        InputStream exifInputStream = getContentResolver().openInputStream(imageUri);
        ExifInterface exifInterface = new ExifInterface(exifInputStream);
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        exifInputStream.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void processDetectionResults(RoboflowDetectionResponse detectionResponse, Bitmap originalBitmap) {
        if (originalBitmap == null) {
            showDetectionDialog(null);
            return;
        }

        List<String> detectedInstruments = new ArrayList<>();

        if (detectionResponse != null && detectionResponse.getPredictions() != null && !detectionResponse.getPredictions().isEmpty()) {

            detectedInstruments = detectionResponse.getPredictions().stream()
                    .map(Prediction::getClassName)
                    .distinct()
                    .collect(Collectors.toList());

            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.YELLOW);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(12);
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(40);
            textPaint.setFakeBoldText(true);
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.YELLOW);
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setAlpha(180);

            for (Prediction prediction : detectionResponse.getPredictions()) {
                float x = (float) prediction.getX();
                float y = (float) prediction.getY();
                float width = (float) prediction.getWidth();
                float height = (float) prediction.getHeight();
                String className = prediction.getClassName();
                float left = x - (width / 2);
                float top = y - (height / 2);
                float right = x + (width / 2);
                float bottom = y + (height / 2);
                canvas.drawRect(left, top, right, bottom, boxPaint);
                Rect textBounds = new Rect();
                textPaint.getTextBounds(className, 0, className.length(), textBounds);
                float padding = 8;
                float backgroundLeft = left;
                float backgroundTop = Math.max(0, top - textBounds.height() - (padding * 2));
                float backgroundRight = backgroundLeft + textPaint.measureText(className) + (padding * 2);
                float backgroundBottom = top;
                if (backgroundRight > originalBitmap.getWidth()) {
                    backgroundRight = originalBitmap.getWidth();
                    backgroundLeft = backgroundRight - (textPaint.measureText(className) + (padding * 2));
                }
                canvas.drawRect(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom, backgroundPaint);
                canvas.drawText(className, backgroundLeft + padding, top - padding, textPaint);
            }

            resultImageView.setImageBitmap(mutableBitmap);

            Uri newSavedUri = PhotoHistoryManager.saveInferredImageAndGetUri(this, mutableBitmap);

            if (newSavedUri != null) {
                PhotoHistoryManager.addPhotoUri(this, newSavedUri);
                this.imageUri = newSavedUri;
            } else {
                Toast.makeText(this, "Gagal menyimpan gambar hasil olahan.", Toast.LENGTH_SHORT).show();
            }

        } else {
            resultImageView.setImageBitmap(originalBitmap);
            Log.d("DetectionResult", "Tidak ada deteksi, gambar tidak disimpan ke riwayat.");
        }

        showDetectionDialog(detectedInstruments);
    }

    private void showDetectionDialog(List<String> detectedInstruments) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_detection_result, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        ImageView dialogIcon = dialogView.findViewById(R.id.dialog_icon);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button okButton = dialogView.findViewById(R.id.dialog_button_ok);

        if (detectedInstruments != null && !detectedInstruments.isEmpty()) {
            dialogIcon.setImageResource(R.drawable.ic_success_checkmark);
            dialogTitle.setText("Deteksi Berhasil");

            // --- MODIFIKASI DIMULAI DI SINI ---
            String instruments = String.join(", ", detectedInstruments);
            String messageText = "Alat musik yang berhasil dikenali:<br><br><b>" + instruments + "</b>";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                dialogMessage.setText(Html.fromHtml(messageText, Html.FROM_HTML_MODE_LEGACY));
            } else {
                dialogMessage.setText(Html.fromHtml(messageText));
            }
            // --- MODIFIKASI SELESAI ---

        } else {
            dialogIcon.setImageResource(R.drawable.ic_not_found);
            dialogTitle.setText("Tidak Ditemukan");
            dialogMessage.setText("Maaf, tidak ada alat musik yang dapat kami kenali pada gambar ini.");
        }

        okButton.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }


    private void initializeGeminiChatFromHistory() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        List<Content> history = new ArrayList<>();

        Content.Builder systemPromptBuilder = new Content.Builder();
        systemPromptBuilder.setRole("user");
        systemPromptBuilder.addText(MAESTRO_GENTA_SYSTEM_PROMPT);
        history.add(systemPromptBuilder.build());

        for (ChatMessage message : messageList) {
            String role = message.isSentByUser() ? "user" : "model";
            Content.Builder contentBuilder = new Content.Builder();
            contentBuilder.setRole(role);
            contentBuilder.addText(message.getMessageText());
            history.add(contentBuilder.build());
        }

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        chat = modelFutures.startChat(history);
    }


    private void initializeGeminiChat(RoboflowDetectionResponse detectionResponse) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        List<Content> history = new ArrayList<>();

        Content.Builder systemPromptBuilder = new Content.Builder();
        systemPromptBuilder.setRole("user");
        systemPromptBuilder.addText(MAESTRO_GENTA_SYSTEM_PROMPT);
        history.add(systemPromptBuilder.build());

        Content.Builder modelGreetingBuilder = new Content.Builder();
        modelGreetingBuilder.setRole("model");
        modelGreetingBuilder.addText("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");
        history.add(modelGreetingBuilder.build());
        addBotMessage("Salam! Saya Maestro Genta. Silakan bertanya tentang alat musik tradisional Indonesia.");

        if (detectionResponse != null && detectionResponse.getPredictions() != null && !detectionResponse.getPredictions().isEmpty()) {
            List<String> detectedInstruments = detectionResponse.getPredictions().stream()
                    .map(Prediction::getClassName)
                    .distinct()
                    .collect(Collectors.toList());

            String detectionContextForGemini;
            if (detectedInstruments.size() == 1) {
                detectionContextForGemini = "Konteks: Alat musik yang terdeteksi di gambar adalah " + detectedInstruments.get(0) + ".";
            } else {
                detectionContextForGemini = "Konteks: Alat musik yang terdeteksi di gambar adalah " +
                        String.join(", ", detectedInstruments) + ".";
            }
            Content.Builder detectionContextBuilder = new Content.Builder();
            detectionContextBuilder.setRole("user");
            detectionContextBuilder.addText(detectionContextForGemini);
            history.add(detectionContextBuilder.build());

            String modelResponseToUser;
            if (detectedInstruments.size() == 1) {
                modelResponseToUser = "Baik, saya melihat ada **" + detectedInstruments.get(0) + "** pada gambar. Apa yang ingin Anda ketahui tentang alat musik ini?";
            } else {
                modelResponseToUser = "Baik, saya melihat ada beberapa alat musik: **" +
                        String.join(", ", detectedInstruments) + "**. Alat musik mana yang ingin Anda diskusikan terlebih dahulu?";
            }
            Content.Builder modelAcknowledgementBuilder = new Content.Builder();
            modelAcknowledgementBuilder.setRole("model");
            modelAcknowledgementBuilder.addText(modelResponseToUser);
            history.add(modelAcknowledgementBuilder.build());
            addBotMessage(modelResponseToUser);

        } else {
            Content.Builder noDetectionMessageBuilder = new Content.Builder();
            noDetectionMessageBuilder.setRole("user");
            noDetectionMessageBuilder.addText("Konteks: Tidak ada alat musik yang terdeteksi pada gambar.");
            history.add(noDetectionMessageBuilder.build());

            Content.Builder modelNoDetectionAcknowledgementBuilder = new Content.Builder();
            modelNoDetectionAcknowledgementBuilder.setRole("model");
            modelNoDetectionAcknowledgementBuilder.addText("Sepertinya tidak ada alat musik tradisional yang bisa saya kenali pada gambar ini. Namun, jangan khawatir! Anda tetap bisa bertanya apa saja mengenai alat musik tradisional Indonesia.");
            history.add(modelNoDetectionAcknowledgementBuilder.build());
            addBotMessage("Sepertinya tidak ada alat musik tradisional yang bisa saya kenali pada gambar ini. Namun, jangan khawatir! Anda tetap bisa bertanya apa saja mengenai alat musik tradisional Indonesia.");
        }

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
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
            messageEditText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
            messageEditText.setEnabled(true);
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
        executorService.shutdown();
    }
}
