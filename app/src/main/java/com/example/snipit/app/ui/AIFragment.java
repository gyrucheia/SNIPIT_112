package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        chatRepo = new ChatRepository(requireActivity().getApplication());

        drawer = v.findViewById(R.id.ai_drawer_layout);
        aiToolbarWrap = v.findViewById(R.id.ai_toolbar_wrap);
        chatContainer = v.findViewById(R.id.chat_container);
        chatScroll = v.findViewById(R.id.chat_scroll);
        greetingWrap = v.findViewById(R.id.ai_greeting_wrap);
        input = v.findViewById(R.id.ai_input);

        aiOnlineDot = v.findViewById(R.id.ai_online_dot);
        aiOnlineLabel = v.findViewById(R.id.ai_online_label);
        refreshOnlineStatus();

        v.findViewById(R.id.btn_main_profile).setOnClickListener(x -> {
            if (getActivity() instanceof com.example.snipit.app.MainActivity) {
                ((com.example.snipit.app.MainActivity) getActivity()).openMainDrawer();
            }
        });

        ImageButton menu = v.findViewById(R.id.btn_ai_menu);
        menu.setOnClickListener(x -> drawer.openDrawer(GravityCompat.END));

        View newChatToolbar = v.findViewById(R.id.btn_new_chat_toolbar);
        if (newChatToolbar != null) {
            newChatToolbar.setOnClickListener(x -> startNewChat());
        }

        RecyclerView historyRv = v.findViewById(R.id.ai_history_list);
        historyRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        sessionAdapter =
                new AiSessionAdapter(
                        session -> {
                            drawer.closeDrawer(GravityCompat.END);
                            loadSession(session);
                        });
        historyRv.setAdapter(sessionAdapter);

        chatRepo
                .sessions()
                .observe(
                        getViewLifecycleOwner(),
                        sessions -> {
                            if (sessionAdapter != null) sessionAdapter.submit(sessions);
                        });

        v.findViewById(R.id.btn_new_ai_chat).setOnClickListener(x -> startNewChat());
        v.findViewById(R.id.btn_clear_ai_history)
                .setOnClickListener(
                        x ->
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.clear_all_history)
                                        .setMessage(R.string.clear_history_confirm)
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(
                                                android.R.string.ok,
                                                (d, w) ->
                                                        chatRepo.clearAllHistory(
                                                                () -> {
                                                                    currentSessionId = null;
                                                                    clearChatMessages();
                                                                    showGreeting(true);
                                                                }))
                                        .show());

        v.findViewById(R.id.btn_send).setOnClickListener(x -> send());

        v.findViewById(R.id.chip_tagalog)
                .setOnClickListener(x -> input.setText("Explain this in Tagalog:\n"));
        v.findViewById(R.id.chip_kotlin)
                .setOnClickListener(x -> input.setText("Convert this Java to Kotlin:\n"));
        v.findViewById(R.id.chip_optimize)
                .setOnClickListener(x -> input.setText("Optimize this for mobile:\n"));
        v.findViewById(R.id.chip_debug)
                .setOnClickListener(x -> input.setText("Find the bug in:\n"));
        v.findViewById(R.id.chip_errors)
                .setOnClickListener(x -> input.setText("Add error handling to:\n"));
    }

    private void startNewChat() {
        currentSessionId = null;
        clearChatMessages();
        showGreeting(true);
        drawer.closeDrawer(GravityCompat.END);
    }

    private void refreshOnlineStatus() {
        if (aiOnlineDot == null || aiOnlineLabel == null) return;

        boolean internetOn = NetworkUtils.isOnline(requireContext());
        boolean keyConfigured = !TextUtils.isEmpty(BuildConfig.OPENROUTER_API_KEY);
        updateStatusUi(internetOn && keyConfigured);
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
        chatRepo.loadMessages(
                session.id,
                messages -> {
                    if (!isAdded()) return;
                    for (AiChatMessage m : messages) {
                        if ("user".equals(m.role)) {
                            addBubble(m.body, true);
                        } else {
                            addBubble(m.body, false);
                        }
                    }
                    if (!messages.isEmpty()) {
                        AiChatMessage last = messages.get(messages.size() - 1);
                        if ("assistant".equals(last.role)) {
                            addAssistantActionsRow(last.body);
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
        String prompt = "Please review and fix this code snippet:\n\n" + code;
        input.setText(prompt);
        send(); // Automatically trigger the AI analysis
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
            chatRepo.createSession(
                    title,
                    id -> {
                        currentSessionId = id;
                        performAiAnalysis(id, msg);
                    });
        } else {
            performAiAnalysis(currentSessionId, msg);
        }
    }

    private void performAiAnalysis(long sessionId, String msg) {
        chatRepo.saveUserMessage(sessionId, msg, () -> {
            // Use the direct AI service instead of the local Python server
            aiModel.analyzeCode(msg, new GitHubModelService.AiCallback() {
                @Override
                public void onResponse(String fixedCode, String explanation) {
                    if (!isAdded()) return;
                    removeTypingIndicator();
                    
                    String reply = fixedCode + "\n\n" + explanation;
                    addBubble(reply, false);
                    addAssistantActionsRow(fixedCode);
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
    }

    private void addTypingIndicator() {
        TextView tv = new TextView(requireContext());
        tv.setTag(TAG_TYPING);
        tv.setText("Thinking…");
        tv.setTextSize(12);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(getResources().getColor(R.color.text_muted, null));
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START;
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        chatContainer.addView(tv);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void removeTypingIndicator() {
        for (int i = chatContainer.getChildCount() - 1; i >= 0; i--) {
            View c = chatContainer.getChildAt(i);
            if (TAG_TYPING.equals(c.getTag())) {
                chatContainer.removeViewAt(i);
                return;
            }
        }
    }

    /** Like the HTML mock: Auto-Snip + Copy under assistant replies. */
    private void addAssistantActionsRow(String code) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.gravity = Gravity.START;
        rowLp.bottomMargin = dp(12);
        row.setLayoutParams(rowLp);

        Button auto = new Button(requireContext());
        auto.setText(R.string.auto_snip_short);
        auto.setTextSize(11);
        auto.setBackgroundResource(R.drawable.bg_button_green);
        auto.setTextColor(getResources().getColor(R.color.black, null));
        LinearLayout.LayoutParams lpA =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpA.setMarginEnd(dp(6));
        auto.setLayoutParams(lpA);
        auto.setOnClickListener(
                v -> {
                    BadgeTracker.recordAiAutoSnip(requireContext());
                    Intent i = new Intent(requireContext(), NewSnipActivity.class);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "AI output");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, code);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Text");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#AI,#Refactored");
                    startActivity(i);
                    XpManager.addXp(requireContext(), 15);
                });

        Button copy = new Button(requireContext());
        copy.setText(R.string.copy_code);
        copy.setTextSize(11);
        copy.setBackgroundResource(R.drawable.bg_card);
        copy.setTextColor(getResources().getColor(R.color.text_secondary, null));
        copy.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        copy.setOnClickListener(
                v -> {
                    ClipboardManager cm =
                            (ClipboardManager)
                                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("code", code != null ? code : ""));
                    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                });

        row.addView(auto);
        row.addView(copy);
        chatContainer.addView(row);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private String cannedReply(String msg) {
        String m = msg.toLowerCase();
        if (m.contains("tagalog")) {
            return "Offline fallback: Halimbawa sagot — i-save ang snippet sa Vault pagkatapos.";
        }
        if (m.contains("kotlin")) {
            return "Offline fallback:\nfun main() = println(\"SnipIT\")";
        }

        if (m.contains("optimize") || m.contains("mobile")) {
            return "Offline fallback: bawasan ang work sa main thread; gamitin ang DiffUtil.";
        }
        if (m.contains("bug")) {
            return "Offline fallback: tingnan ang null safety at bounds.";
        }
        return "Offline mode: walang GitHub token o walang network. Ilagay ang GITHUB_MODELS_TOKEN sa local.properties (PAT na may models scope).";
    }

    private void addBubble(String text, boolean user) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        int maxW = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
        tv.setMaxWidth(maxW);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = user ? Gravity.END : Gravity.START;
        lp.bottomMargin = dp(10);
        tv.setLayoutParams(lp);
        if (user) {
            tv.setBackgroundResource(R.drawable.bg_card);
            tv.setTextColor(getResources().getColor(R.color.text_primary, null));
            tv.setOnLongClickListener(
                    v -> {
                        CharSequence body = tv.getText();
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.prompt_actions)
                                .setItems(
                                        new CharSequence[] {
                                            getString(R.string.copy_prompt),
                                            getString(R.string.edit_prompt),
                                            getString(R.string.snip_prompt)
                                        },
                                        (d, which) -> {
                                            if (which == 0) {
                                                ClipboardManager cm =
                                                        (ClipboardManager)
                                                                requireContext()
                                                                        .getSystemService(
                                                                                Context.CLIPBOARD_SERVICE);
                                                cm.setPrimaryClip(
                                                        ClipData.newPlainText(
                                                                "prompt",
                                                                body != null ? body : ""));
                                                Toast.makeText(
                                                                requireContext(),
                                                                R.string.copied,
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                            } else if (which == 1) {
                                                input.setText(body != null ? body : "");
                                                input.requestFocus();
                                                input.setSelection(input.getText().length());
                                                InputMethodManager imm =
                                                        (InputMethodManager)
                                                                requireContext()
                                                                        .getSystemService(
                                                                                Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.showSoftInput(
                                                            input, InputMethodManager.SHOW_IMPLICIT);
                                                }
                                                chatScroll.post(
                                                        () ->
                                                                chatScroll.fullScroll(
                                                                        View.FOCUS_DOWN));
                                            } else if (which == 2) {
                                                Intent i = new Intent(requireContext(), NewSnipActivity.class);
                                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "AI prompt");
                                                i.putExtra(
                                                        NewSnipActivity.EXTRA_PREFILL_CODE,
                                                        body != null ? body.toString() : "");
                                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Text");
                                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#AI,#Prompt");
                                                startActivity(i);
                                            }
                                        })
                                .show();
                        return true;
                    });
        } else {
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setBackgroundResource(R.drawable.bg_code_block);
            tv.setTextColor(getResources().getColor(R.color.accent_cyan, null));
            tv.setOnLongClickListener(
                    v -> {
                        CharSequence body = tv.getText();
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.ai_reply_actions)
                                .setItems(
                                        new CharSequence[] {
                                            getString(R.string.snip_it),
                                            getString(R.string.copy_code),
                                            getString(R.string.share)
                                        },
                                        (d, which) -> {
                                            if (which == 0) {
                                                BadgeTracker.recordAiAutoSnip(requireContext());
                                                Intent i = new Intent(requireContext(), NewSnipActivity.class);
                                                i.putExtra(
                                                        NewSnipActivity.EXTRA_PREFILL_TITLE,
                                                        "AI output");
                                                i.putExtra(
                                                        NewSnipActivity.EXTRA_PREFILL_CODE,
                                                        body != null ? body.toString() : "");
                                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Text");
                                                i.putExtra(
                                                        NewSnipActivity.EXTRA_PREFILL_TAGS,
                                                        "#AI,#Refactored");
                                                startActivity(i);
                                                XpManager.addXp(requireContext(), 15);
                                            } else if (which == 1) {
                                                ClipboardManager cm =
                                                        (ClipboardManager)
                                                                requireContext()
                                                                        .getSystemService(
                                                                                Context.CLIPBOARD_SERVICE);
                                                cm.setPrimaryClip(
                                                        ClipData.newPlainText(
                                                                "code",
                                                                body != null ? body : ""));
                                                Toast.makeText(
                                                                requireContext(),
                                                                R.string.copied,
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                            } else if (which == 2) {
                                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                                shareIntent.setType("text/plain");
                                                String payload = body != null ? body.toString() : "";
                                                shareIntent.putExtra(Intent.EXTRA_TEXT, payload);
                                                startActivity(
                                                        Intent.createChooser(
                                                                shareIntent, getString(R.string.share)));
                                            }
                                        })
                                .show();
                        return true;
                    });
        }
        chatContainer.addView(tv);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }
}
