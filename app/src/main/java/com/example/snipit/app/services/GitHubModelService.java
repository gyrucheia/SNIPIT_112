package com.example.snipit.app.services;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.example.snipit.app.BuildConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GitHub Models inference API (OpenAI-style chat completions). Requires a GitHub PAT with the
 * {@code models} scope; see GitHub Models quickstart in the docs.
 */
public class GitHubModelService {

    private static final String ENDPOINT =
            "https://models.inference.ai.azure.com/chat/completions";

    /** See model catalog: https://github.com/marketplace?type=models */
    private static final String MODEL_NAME = "gpt-4o-mini";

    private static final String GITHUB_API_VERSION = "2022-11-28";

    private static final String SNIPIT_SYSTEM_PROMPT =
            "You are Snip-AI, the dedicated assistant for the SnipIT Android app. "
            + "Help users fix, explain, and optimize code snippets. Be concise.";

    public interface Callback {
        void onResult(String result);
        void onError(String error);
    }
    
    // Compatibility interface for AIFragment
    public interface AiCallback {
        void onResponse(String fixedCode, String explanation);
        void onError(String error);
    }

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public void analyzeCode(String userMessage, JSONArray history, AiCallback aiCb) {
        analyzeCodeWithContext(userMessage, null, history, aiCb);
    }

    public void analyzeCodeWithContext(String userMessage, String vaultContext, JSONArray history, AiCallback aiCb) {
        sendMessage(userMessage, vaultContext, history, new Callback() {
            @Override
            public void onResult(String result) {
                if (result.contains("```")) {
                    String[] parts = result.split("```");
                    String code = parts.length > 1 ? parts[1].replaceAll("^[a-zA-Z]+\\n", "") : "";
                    String explanation = parts.length > 2 ? parts[2] : "Code fixed by Snip-AI.";
                    aiCb.onResponse(code, explanation);
                } else {
                    aiCb.onResponse("", result);
                }
            }

            @Override
            public void onError(String error) {
                aiCb.onError(error);
            }
        });
    }

    /** Clean up / improve a snippet from the Vault editor. */
    public void improveSnippet(String code, String languageHint, Callback cb) {
        String prompt =
                "As Snip-AI (SnipIT’s editor assistant), improve the following snippet: fix obvious "
                        + "bugs, apply consistent formatting, and preserve the author’s intent. "
                        + "Output ONLY the code or text — no markdown fences, no explanation.\n"
                        + "Language / context: "
                        + (languageHint != null ? languageHint : "unknown")
                        + "\n\n---\n"
                        + (code != null ? code : "")
                        + "\n---";
        sendMessage(prompt, null, cb);
    }

    /** Post-process noisy OCR into cleaner code. */
    public void fixOcrText(String rawOcr, String languageHint, Callback cb) {
        String prompt =
                "As Snip-AI in SnipIT’s Snap flow, fix text extracted by OCR from a whiteboard or screen photo. "
                        + "Fix broken lines, confused characters (0/O, l/1), and indentation. "
                        + "Output ONLY the corrected source code or text — no markdown fences, no explanation.\n"
                        + "Language hint: "
                        + (languageHint != null ? languageHint : "unknown")
                        + "\n\n---\n"
                        + (rawOcr != null ? rawOcr : "")
                        + "\n---";
        sendMessage(prompt, null, cb);
    }

    public void sendMessage(String userMessage, Callback cb) {
        sendMessage(userMessage, null, cb);
    }

    public void sendMessage(String userMessage, JSONArray history, Callback cb) {
        sendMessage(userMessage, null, history, cb);
    }

    public void sendMessage(String userMessage, String vaultContext, JSONArray history, Callback cb) {
        String apiKey = BuildConfig.OPENROUTER_API_KEY;

        if (TextUtils.isEmpty(apiKey)) {
            main.post(() -> cb.onError("Missing OPENROUTER_API_KEY"));
            return;
        }

        exec.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", MODEL_NAME);

                JSONArray messages = new JSONArray();
                String systemPrompt = SNIPIT_SYSTEM_PROMPT;
                if (!TextUtils.isEmpty(vaultContext)) {
                    systemPrompt += "\n\nUser's Vault Context (Recent Snippets):\n" + vaultContext;
                }
                
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                
                if (history != null) {
                    for (int i = 0; i < history.length(); i++) {
                        messages.put(history.get(i));
                    }
                }
                
                messages.put(new JSONObject().put("role", "user").put("content", userMessage));
                jsonBody.put("messages", messages);

                Request request = new Request.Builder()
                        .url(ENDPOINT)
                        .post(RequestBody.create(jsonBody.toString(), MediaType.get("application/json")))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("X-GitHub-Api-Version", GITHUB_API_VERSION)
                        .build();

                try (Response resp = client.newCall(request).execute()) {
                    String respBody = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        main.post(() -> cb.onError("Error " + resp.code() + ": " + respBody));
                        return;
                    }
                    JSONObject json = new JSONObject(respBody);
                    String content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    main.post(() -> cb.onResult(content.trim()));
                }
            } catch (Exception e) {
                main.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

}
