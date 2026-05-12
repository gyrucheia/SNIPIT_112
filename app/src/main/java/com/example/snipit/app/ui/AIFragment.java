package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.BuildConfig;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.database.ChatRepository;
import com.example.snipit.app.models.AiChatMessage;
import com.example.snipit.app.models.AiChatSession;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.services.GitHubModelService;
import com.example.snipit.app.util.NetworkUtils;
import com.example.snipit.app.util.XpManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIFragment extends Fragment {

    private static final Object TAG_TYPING = new Object();

    private DrawerLayout drawer;
    private View aiToolbarWrap;
    private LinearLayout chatContainer;
    private NestedScrollView chatScroll;
    private View greetingWrap;
    private EditText input;
    private final GitHubModelService aiModel = new GitHubModelService();

    private ChatRepository chatRepo;
    private Long currentSessionId;
    private AiSessionAdapter sessionAdapter;
    private View aiOnlineDot;
    private TextView aiOnlineLabel;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        
        try {
            chatRepo = new ChatRepository(requireActivity().getApplication());
        } catch (Exception e) {
            chatRepo = null;
            return;
        }

        try {
            drawer = v.findViewById(R.id.ai_drawer_layout);
            aiToolbarWrap = v.findViewById(R.id.ai_toolbar_wrap);
            chatContainer = v.findViewById(R.id.chat_container);
            chatScroll = v.findViewById(R.id.chat_scroll);
            greetingWrap = v.findViewById(R.id.ai_greeting_wrap);
            input = v.findViewById(R.id.ai_input);

            aiOnlineDot = v.findViewById(R.id.ai_online_dot);
            aiOnlineLabel = v.findViewById(R.id.ai_online_label);
            refreshOnlineStatus();
        } catch (Exception e) {
            return;
        }

        setupListeners(v);
    }

    private void setupListeners(View v) {
        v.findViewById(R.id.btn_main_profile).setOnClickListener(x -> {
            if (getActivity() instanceof com.example.snipit.app.MainActivity) {
                ((com.example.snipit.app.MainActivity) getActivity()).openMainDrawer();
            }
        });

        ImageButton menu = v.findViewById(R.id.btn_ai_menu);
        menu.setOnClickListener(x -> {
            if (drawer != null) drawer.openDrawer(GravityCompat.END);
        });

        View newChatToolbar = v.findViewById(R.id.btn_new_chat_toolbar);
        if (newChatToolbar != null) {
            newChatToolbar.setOnClickListener(x -> startNewChat());
        }

        RecyclerView historyRv = v.findViewById(R.id.ai_history_list);
        historyRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        sessionAdapter = new AiSessionAdapter(session -> {
            if (drawer != null) drawer.closeDrawer(GravityCompat.END);
            loadSession(session);
        });
        historyRv.setAdapter(sessionAdapter);

        if (chatRepo != null) {
            chatRepo.sessions().observe(getViewLifecycleOwner(), sessions -> {
                if (sessionAdapter != null) sessionAdapter.submit(sessions);
            });
        }

        v.findViewById(R.id.btn_clear_ai_history).setOnClickListener(x -> clearHistoryDialog());
        v.findViewById(R.id.btn_send).setOnClickListener(x -> send());

        // Quick Action Chips
        v.findViewById(R.id.chip_tagalog).setOnClickListener(x -> setInputAndFocus("Explain this in Tagalog:\n"));
        v.findViewById(R.id.chip_kotlin).setOnClickListener(x -> setInputAndFocus("Convert this Java to Kotlin:\n"));
        v.findViewById(R.id.chip_optimize).setOnClickListener(x -> setInputAndFocus("Optimize this for mobile:\n"));
        v.findViewById(R.id.chip_debug).setOnClickListener(x -> setInputAndFocus("Find the bug in:\n"));
        v.findViewById(R.id.chip_errors).setOnClickListener(x -> setInputAndFocus("Add error handling to:\n"));
    }

    private void setInputAndFocus(String text) {
        if (input != null) {
            input.setText(text);
            input.requestFocus();
            input.setSelection(text.length());
        }
    }

    private void clearHistoryDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_all_history)
                .setMessage(R.string.clear_history_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (chatRepo != null) {
                        chatRepo.clearAllHistory(() -> {
                            currentSessionId = null;
                            clearChatMessages();
                            showGreeting(true);
                        });
                    }
                }).show();
    }

    private void startNewChat() {
        currentSessionId = null;
        clearChatMessages();
        showGreeting(true);
        if (drawer != null) drawer.closeDrawer(GravityCompat.END);
    }

    private void refreshOnlineStatus() {
        if (aiOnlineDot == null || aiOnlineLabel == null) return;
        try {
            boolean internetOn = NetworkUtils.isOnline(requireContext());
            boolean keyConfigured = !TextUtils.isEmpty(BuildConfig.OPENROUTER_API_KEY);
            updateStatusUi(internetOn && keyConfigured);
        } catch (Exception e) {}
    }

    private void updateStatusUi(boolean isAiOnline) {
        int dotColor = isAiOnline
                ? getResources().getColor(R.color.accent_purple, null)
                : getResources().getColor(R.color.text_muted, null);
        aiOnlineDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));
        aiOnlineLabel.setText(isAiOnline ? R.string.ai_status_online : R.string.ai_status_offline);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshOnlineStatus();
    }

    private void loadSession(AiChatSession session) {
        currentSessionId = session.id;
        clearChatMessages();
        showGreeting(false);
        chatRepo.loadMessages(session.id, messages -> {
            if (!isAdded()) return;
            for (AiChatMessage m : messages) {
                addBubble(m.body, "user".equals(m.role));
            }
            if (!messages.isEmpty()) {
                AiChatMessage last = messages.get(messages.size() - 1);
                if ("assistant".equals(last.role)) {
                    extractAndAddActions(last.body);
                }
            }
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void clearChatMessages() {
        for (int i = chatContainer.getChildCount() - 1; i >= 0; i--) {
            View c = chatContainer.getChildAt(i);
            if (c.getId() != R.id.ai_greeting_wrap) {
                chatContainer.removeViewAt(i);
            }
        }
    }

    public void startFixSession(String code) {
        if (!isAdded()) return;
        showGreeting(false);
        addBubble("🩺 *System: Initiating Code Doctor Session...*", false);
        
        String prompt = "As a Senior Software Engineer and Code Doctor, please review, fix, and optimize this snippet. " +
                "Focus on performance, readability, and modern best practices.\n\n" +
                "```\n" + code + "\n```";
        
        input.setText(prompt);
        send();
    }

    private void showGreeting(boolean show) {
        greetingWrap.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void send() {
        String msg = input.getText() != null ? input.getText().toString().trim() : "";
        if (msg.isEmpty()) return;
        showGreeting(false);
        addBubble(msg, true);
        input.setText("");
        XpManager.addXp(requireContext(), 3);
        addTypingIndicator();

        if (currentSessionId == null) {
            String title = msg.length() > 36 ? msg.substring(0, 36) + "…" : msg;
            chatRepo.createSession(title, id -> {
                currentSessionId = id;
                performAiAnalysis(id, msg);
            });
        } else {
            performAiAnalysis(currentSessionId, msg);
        }
    }

    private void performAiAnalysis(long sessionId, String msg) {
        if (chatRepo == null) return;
        
        // Smart Context Bridge: Search for relevant snippets
        com.example.snipit.app.database.SnipRepository snipRepo = new com.example.snipit.app.database.SnipRepository(requireActivity().getApplication());
        snipRepo.getRecentSnips(15, recentSnips -> {
            StringBuilder contextBuilder = new StringBuilder();
            if (recentSnips != null) {
                for (com.example.snipit.app.models.Snip s : recentSnips) {
                    contextBuilder.append("[").append(s.language).append("] ").append(s.title).append("\n");
                }
            }
            String vaultContext = contextBuilder.toString();

            chatRepo.saveUserMessage(sessionId, msg, () -> {
                chatRepo.loadMessages(sessionId, messages -> {
                    org.json.JSONArray history = buildHistoryArray(messages);
                    aiModel.analyzeCodeWithContext(msg, vaultContext, history, new GitHubModelService.AiCallback() {
                        @Override
                        public void onResponse(String fixedCode, String explanation) {
                            if (!isAdded()) return;
                            removeTypingIndicator();

                            String reply = (fixedCode != null && !fixedCode.isEmpty()) 
                                ? "```\n" + fixedCode + "\n```\n\n" + explanation 
                                : explanation;
                            
                            addBubble(reply, false);
                            if (fixedCode != null && !fixedCode.isEmpty()) {
                                addAssistantActionsRow(fixedCode);
                            }
                            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                            chatRepo.saveAssistantMessage(sessionId, reply, null);
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            removeTypingIndicator();
                            addBubble("AI Error: " + error, false);
                        }
                    });
                });
            });
        });
    }

    private org.json.JSONArray buildHistoryArray(List<AiChatMessage> messages) {
        org.json.JSONArray history = new org.json.JSONArray();
        try {
            int start = Math.max(0, messages.size() - 8);
            for (int i = start; i < messages.size() - 1; i++) {
                AiChatMessage old = messages.get(i);
                history.put(new org.json.JSONObject().put("role", old.role).put("content", old.body));
            }
        } catch (Exception ignored) {}
        return history;
    }

    private void addTypingIndicator() {
        TextView tv = new TextView(requireContext());
        tv.setTag(TAG_TYPING);
        tv.setText("Thinking");
        tv.setTextSize(12);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(getResources().getColor(R.color.text_muted, null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START;
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        chatContainer.addView(tv);
        
        // Simple dot animation
        final int[] dots = {0};
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tv.getParent() != null) {
                    dots[0] = (dots[0] + 1) % 4;
                    String s = "Thinking";
                    for (int i=0; i<dots[0]; i++) s += ".";
                    tv.setText(s);
                    mainHandler.postDelayed(this, 500);
                }
            }
        });
        
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void removeTypingIndicator() {
        // Haptic feedback when the thinking stops (answer arrives)
        try {
            android.os.Vibrator v = (android.os.Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(15);
                }
            }
        } catch (Exception ignored) {}

        for (int i = chatContainer.getChildCount() - 1; i >= 0; i--) {
            View c = chatContainer.getChildAt(i);
            if (TAG_TYPING.equals(c.getTag())) {
                chatContainer.removeViewAt(i);
                return;
            }
        }
    }

    private void extractAndAddActions(String fullBody) {
        if (fullBody.contains("```")) {
            String[] parts = fullBody.split("```");
            if (parts.length > 1) {
                String code = parts[1].replaceAll("^[a-zA-Z]+\\n", "").trim();
                addAssistantActionsRow(code);
            }
        }
    }

    private void addAssistantActionsRow(String code) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.gravity = Gravity.START;
        rowLp.bottomMargin = dp(12);
        rowLp.leftMargin = dp(8);
        row.setLayoutParams(rowLp);

        Button auto = new Button(requireContext());
        auto.setText(R.string.auto_snip_short);
        auto.setTextSize(10);
        auto.setBackgroundResource(R.drawable.bg_button_green);
        auto.setTextColor(getResources().getColor(R.color.black, null));
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, dp(36), 1f);
        lpA.setMarginEnd(dp(6));
        auto.setLayoutParams(lpA);
        auto.setOnClickListener(v -> {
            BadgeTracker.recordAiAutoSnip(requireContext());
            Intent i = new Intent(requireContext(), NewSnipActivity.class);
            i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "AI Solution");
            i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, code);
            i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Auto");
            i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#AI,#Refactored");
            startActivity(i);
            XpManager.addXp(requireContext(), 15);
        });

        Button copy = new Button(requireContext());
        copy.setText(R.string.copy_code);
        copy.setTextSize(10);
        copy.setBackgroundResource(R.drawable.bg_card);
        copy.setTextColor(getResources().getColor(R.color.text_secondary, null));
        copy.setLayoutParams(new LinearLayout.LayoutParams(0, dp(36), 1f));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("code", code));
            Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
        });

        row.addView(auto);
        row.addView(copy);
        chatContainer.addView(row);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addBubble(String text, boolean user) {
        TextView tv = new TextView(requireContext());
        tv.setTextSize(13);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        int maxW = (int) (getResources().getDisplayMetrics().widthPixels * 0.85f);
        tv.setMaxWidth(maxW);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = user ? Gravity.END : Gravity.START;
        lp.bottomMargin = dp(10);
        tv.setLayoutParams(lp);

        if (user) {
            tv.setBackgroundResource(R.drawable.bg_card);
            tv.setTextColor(getResources().getColor(R.color.text_primary, null));
            tv.setText(text);
        } else {
            tv.setBackgroundResource(R.drawable.bg_code_block);
            applyMarkdown(tv, text);
        }

        // Slide up animation
        Animation slide = new TranslateAnimation(0, 0, dp(20), 0);
        slide.setDuration(250);
        Animation fade = new AlphaAnimation(0, 1);
        fade.setDuration(250);
        tv.startAnimation(slide);
        
        chatContainer.addView(tv);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        
        setupBubbleLongClick(tv, user);
    }

    private void applyMarkdown(TextView tv, String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        Pattern p = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        int lastEnd = 0;
        
        while (m.find()) {
            // Text before code block
            ssb.append(text.substring(lastEnd, m.start()));
            
            // Code block
            int start = ssb.length();
            String code = m.group(1).replaceAll("^[a-zA-Z]+\\n", "");
            ssb.append(code);
            int end = ssb.length();
            
            ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.terminal_green, null)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            lastEnd = m.end();
        }
        ssb.append(text.substring(lastEnd));
        tv.setText(ssb);
    }

    private void setupBubbleLongClick(TextView tv, boolean user) {
        tv.setOnLongClickListener(v -> {
            CharSequence body = tv.getText();
            String[] options = user 
                ? new String[] {getString(R.string.copy_prompt), getString(R.string.edit_prompt), getString(R.string.snip_prompt)}
                : new String[] {getString(R.string.snip_it), getString(R.string.copy_code), getString(R.string.share)};
                
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(user ? R.string.prompt_actions : R.string.ai_reply_actions)
                    .setItems(options, (d, which) -> {
                        handleBubbleAction(which, body.toString(), user);
                    }).show();
            return true;
        });
    }

    private void handleBubbleAction(int which, String body, boolean user) {
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (user) {
            if (which == 0) {
                cm.setPrimaryClip(ClipData.newPlainText("prompt", body));
                Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
            } else if (which == 1) {
                setInputAndFocus(body);
            } else if (which == 2) {
                startNewSnipActivity("AI Prompt", body, "#AI,#Prompt");
            }
        } else {
            if (which == 0) {
                startNewSnipActivity("AI Solution", body, "#AI,#Refactored");
            } else if (which == 1) {
                cm.setPrimaryClip(ClipData.newPlainText("code", body));
                Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
            } else if (which == 2) {
                shareText(body);
            }
        }
    }

    private void startNewSnipActivity(String title, String code, String tags) {
        Intent i = new Intent(requireContext(), NewSnipActivity.class);
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, title);
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, code);
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Auto");
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, tags);
        startActivity(i);
    }

    private void shareText(String text) {
        Intent si = new Intent(Intent.ACTION_SEND);
        si.setType("text/plain");
        si.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(si, getString(R.string.share)));
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }
}
