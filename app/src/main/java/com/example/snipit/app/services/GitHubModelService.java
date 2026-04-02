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
 * GitHub Models (Azure OpenAI–compatible chat/completions). Set {@code GITHUB_TOKEN} in
 * {@code local.properties} (classic PAT, often starts with ghp_).
 *
 * @see <a href="https://github.com/marketplace/models">GitHub Models</a>
 */
public class GitHubModelService {

    private static final String ENDPOINT = "https://models.inference.ai.azure.com/chat/completions";

    /** Swap model id here (e.g. gpt-4o-mini, Llama-3.1-8B-Instruct, Phi-3-mini). */
    private static final String MODEL_NAME = "gpt-4o-mini";

    /**
     * Product identity: same idea as a hosted assistant that only serves one app — never “I’m a general
     * model”; always Snip-AI inside SnipIT.
     */
    private static final String SNIPIT_SYSTEM_PROMPT =
            "You are Snip-AI, the dedicated assistant for the SnipIT Android app only. Your job is to help "
                    + "users with code snippets in SnipIT: the Vault (local snippet library), Beam (send code "
                    + "to a PC with a PIN), Snap (OCR from photos), Dev-Dex (CLI reference), QR import, and "
                    + "the snippet editor. You are not a generic chatbot or a detached cloud model—introduce "
                    + "yourself as Snip-AI when it fits, and assume every question is in the SnipIT context. "
                    + "Be warm, direct, and concise; say “we” / “in SnipIT” when helpful. Never tell the user "
                    + "to use a different product instead of SnipIT for snippet workflows.";

    public interface Callback {
        void onResult(String result);

        void onError(String error);
    }

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

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
        sendWithSystemPrompt(prompt, cb);
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
        sendWithSystemPrompt(prompt, cb);
    }

    /** General chat / instruction (Snip-AI). Uses SnipIT system persona + user message. */
    public void sendMessage(String userMessage, Callback cb) {
        sendWithSystemPrompt(userMessage, cb);
    }

    /** Single user turn with SnipIT system instruction (also used for improve/OCR tasks). */
    private void sendWithSystemPrompt(String userContent, Callback cb) {
        if (TextUtils.isEmpty(BuildConfig.GITHUB_TOKEN)) {
            main.post(() -> cb.onError("Missing GITHUB_TOKEN in local.properties"));
            return;
        }
        exec.execute(
                () -> {
                    try {
                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("model", MODEL_NAME);
                        JSONArray messages = new JSONArray();
                        messages.put(
                                new JSONObject()
                                        .put("role", "system")
                                        .put("content", SNIPIT_SYSTEM_PROMPT));
                        messages.put(
                                new JSONObject()
                                        .put("role", "user")
                                        .put("content", userContent != null ? userContent : ""));
                        jsonBody.put("messages", messages);
                        jsonBody.put("max_tokens", 1000);

                        Request request =
                                new Request.Builder()
                                        .url(ENDPOINT)
                                        .post(
                                                RequestBody.create(
                                                        jsonBody.toString(),
                                                        MediaType.get("application/json")))
                                        .addHeader(
                                                "Authorization",
                                                "Bearer " + BuildConfig.GITHUB_TOKEN)
                                        .addHeader("Content-Type", "application/json")
                                        .build();

                        try (Response resp = client.newCall(request).execute()) {
                            String respBody = resp.body() != null ? resp.body().string() : "";
                            if (!resp.isSuccessful()) {
                                main.post(() -> parseOrRawError(resp.code(), respBody, cb));
                                return;
                            }
                            JSONObject json = new JSONObject(respBody);
                            JSONArray choices = json.optJSONArray("choices");
                            if (choices == null || choices.length() == 0) {
                                main.post(
                                        () ->
                                                cb.onError(
                                                        "No choices in model response: "
                                                                + respBody));
                                return;
                            }
                            JSONObject first = choices.getJSONObject(0);
                            JSONObject message = first.optJSONObject("message");
                            String content =
                                    message != null ? message.optString("content") : "";
                            if (TextUtils.isEmpty(content)) {
                                main.post(() -> cb.onError("Empty content in model response"));
                                return;
                            }
                            String out = content.trim();
                            main.post(() -> cb.onResult(out));
                        }
                    } catch (Exception e) {
                        String err = e.getMessage() != null ? e.getMessage() : "network error";
                        main.post(() -> cb.onError(err));
                    }
                });
    }

    private void parseOrRawError(int code, String respBody, Callback cb) {
        try {
            JSONObject json = new JSONObject(respBody);
            if (json.has("error")) {
                JSONObject err = json.optJSONObject("error");
                if (err != null) {
                    cb.onError(
                            "HTTP "
                                    + code
                                    + ": "
                                    + err.optString("message", err.toString()));
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        cb.onError("HTTP " + code + ": " + respBody);
    }
}
