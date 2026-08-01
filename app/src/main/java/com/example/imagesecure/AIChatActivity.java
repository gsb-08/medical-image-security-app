package com.example.imagesecure;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChatActivity extends AppCompatActivity {

    // ⚠️ For testing only—don’t ship hard-coded keys in production.
    private static final String GEMINI_API_KEY = "AIzaSyD0sIKe-AVn3cKSLGOLQ9085tDseohGYes";
    private static final String MODEL = "gemini-1.5-flash-latest";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1/models/" + MODEL + ":generateContent?key=" + GEMINI_API_KEY;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client;
    private Gson gson;

    private EditText etPrompt;
    private Button btnSend;
    private TextView tvChat;
    private ProgressBar progress;
    private ScrollView scroll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aichat);

        client = new OkHttpClient();
        gson = new Gson();

        etPrompt = findViewById(R.id.etPrompt);
        btnSend = findViewById(R.id.btnSend);
        tvChat = findViewById(R.id.tvChat);
        progress = findViewById(R.id.progress);
        scroll = findViewById(R.id.scroll);

        btnSend.setOnClickListener(v -> {
            String prompt = etPrompt.getText().toString().trim();
            if (TextUtils.isEmpty(prompt)) {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPrompt(prompt);
        });

        // Send when pressing IME send on keyboard
        etPrompt.setOnEditorActionListener((v, actionId, event) -> {
            btnSend.performClick();
            return true;
        });
    }

    private void sendPrompt(String prompt) {
        setLoading(true);
        appendChat("You: " + prompt + "\n");

        // Build request body per Gemini REST spec
        // { "contents": [ { "parts": [ { "text": "..." } ] } ] }
        GenerateRequest bodyObj = new GenerateRequest(
                Collections.singletonList(
                        new Content(Collections.singletonList(new Part(prompt))))
        );
        String json = gson.toJson(bodyObj);

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(RequestBody.create(json, JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    appendChat("Gemini: (request failed) " + e.getMessage() + "\n");
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        appendChat("Gemini: (HTTP " + response.code() + ") " + resp + "\n");
                    });
                    return;
                }

                // Parse JSON
                GenerateResponse parsed = null;
                try {
                    parsed = gson.fromJson(resp, GenerateResponse.class);
                } catch (Exception ex) {
                    // ignore; show raw
                }

                final String answer = extractText(parsed, resp);
                runOnUiThread(() -> {
                    setLoading(false);
                    appendChat("Gemini: " + answer + "\n");
                });
            }
        });

        etPrompt.setText("");
    }

    private String extractText(GenerateResponse parsed, String fallbackRaw) {
        if (parsed != null
                && parsed.candidates != null
                && !parsed.candidates.isEmpty()
                && parsed.candidates.get(0).content != null
                && parsed.candidates.get(0).content.parts != null
                && !parsed.candidates.get(0).content.parts.isEmpty()
                && parsed.candidates.get(0).content.parts.get(0).text != null) {
            return parsed.candidates.get(0).content.parts.get(0).text.trim();
        }
        return "(no text in response)\n" + fallbackRaw;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
    }

    private void appendChat(String text) {
        tvChat.append(text);
        // autoscroll
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    // ---------- Minimal DTOs for JSON ----------

    static class GenerateRequest {
        @SerializedName("contents")
        java.util.List<Content> contents;
        GenerateRequest(java.util.List<Content> contents) { this.contents = contents; }
    }

    static class Content {
        @SerializedName("parts")
        java.util.List<Part> parts;
        Content(java.util.List<Part> parts) { this.parts = parts; }
    }

    static class Part {
        @SerializedName("text")
        String text;
        Part(String text) { this.text = text; }
    }

    static class GenerateResponse {
        java.util.List<Candidate> candidates;
    }

    static class Candidate {
        Content content;
        SafetyRating[] safetyRatings;
        String finishReason;
    }

    static class SafetyRating {
        String category;
        String probability;
    }
}