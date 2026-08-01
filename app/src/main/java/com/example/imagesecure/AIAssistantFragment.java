package com.example.imagesecure;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AIAssistantFragment extends Fragment {


    // ==== put your Gemini key here ====
    private static final String GEMINI_API_KEY = "AIzaSyCdDZ6EcvMXtN1VaKVpqgxHglDgE9DyH34"; //
    private static final String GEMINI_MODEL = "gemini-2.0-flash"; // fast and good

    private EditText etPrompt;
    private Button btnSend;
    private ProgressBar progress;
    private TextView tvAnswer;

    private final OkHttpClient http = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
    private final Gson gson = new Gson();
    private ExecutorService ioPool;
    private Handler main;
    public AIAssistantFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ioPool = Executors.newSingleThreadExecutor();
        main = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioPool.shutdownNow();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_a_i_assistant, container, false);

        etPrompt = v.findViewById(R.id.etPrompt);
        btnSend = v.findViewById(R.id.btnSend);
        progress = v.findViewById(R.id.progress);
        tvAnswer = v.findViewById(R.id.tvAnswer);
        tvAnswer.setMovementMethod(new ScrollingMovementMethod());

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                final String prompt = etPrompt.getText().toString().trim();
                if (prompt.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a question.", Toast.LENGTH_SHORT).show();
                    return;
                }
                etPrompt.clearFocus();
                sendToGemini(prompt);
            }
        });


        return v;
    }

    private void setLoading(final boolean on) {
        if (getActivity() == null) return;
        main.post(new Runnable() {
            @Override public void run() {
                progress.setVisibility(on ? View.VISIBLE : View.GONE);
                if (on) progress.setProgress(10);
            }
        });
    }

    private void appendAnswer(final String text) {
        if (getActivity() == null) return;
        main.post(new Runnable() {
            @Override public void run() {
                tvAnswer.setText(text);
            }
        });
    }

    private void sendToGemini(final String userPrompt) {
        setLoading(true);
        tvAnswer.setText("");

        ioPool.execute(new Runnable() {
            @Override public void run() {
                try {
                    // REST endpoint
                    HttpUrl url = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent")
                            .newBuilder()
                            .addQueryParameter("key", GEMINI_API_KEY)
                            .build();

                    // Build request body
                    GeminiRequest reqObj = new GeminiRequest(userPrompt);
                    String json = gson.toJson(reqObj);

                    RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
                    Request req = new Request.Builder().url(url).post(body).build();
                    Response resp = http.newCall(req).execute();

                    if (!resp.isSuccessful()) {
                        throw new IOException("Gemini API error: " + resp.code());
                    }

                    String respStr = resp.body().string();
                    GeminiResponse g = gson.fromJson(respStr, GeminiResponse.class);

                    String answer = extractText(g);
                    if (answer == null || answer.trim().isEmpty()) {
                        answer = "No response received.";
                    }

                    setLoading(false);
                    appendAnswer(answer);

                } catch (final Exception e) {
                    setLoading(false);
                    appendAnswer("Error: " + e.getMessage());
                }
            }
        });
    }

    // Safely extract text from Gemini's JSON
    private String extractText(GeminiResponse g) {
        if (g == null || g.candidates == null || g.candidates.isEmpty()) return null;
        GeminiResponse.Candidate c = g.candidates.get(0);
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return null;
        GeminiResponse.Part p = c.content.parts.get(0);
        return p == null ? null : p.text;
    }

    // ====== DTOs for Gemini REST ======
    static class GeminiRequest {
        @SerializedName("contents")
        List<Content> contents;
        GeminiRequest(String prompt) {
            Content c = new Content();
            c.parts = new ArrayList<Part>();
            Part p = new Part();
            p.text = prompt;
            c.parts.add(p);
            contents = new ArrayList<Content>();
            contents.add(c);
        }
    }
    static class Content { @SerializedName("parts") List<Part> parts; }
    static class Part { @SerializedName("text") String text; }

    static class GeminiResponse {
        @SerializedName("candidates") List<Candidate> candidates;
        static class Candidate { @SerializedName("content") Content content; }
        static class Content { @SerializedName("parts") List<Part> parts; }
        static class Part { @SerializedName("text") String text; }
    }


}