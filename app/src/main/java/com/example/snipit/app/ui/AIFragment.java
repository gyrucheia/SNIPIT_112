package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
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
        chatContainer = v.findViewById(R.id.chat_container);
        chatScroll = v.findViewById(R.id.chat_scroll);
        greetingWrap = v.findViewById(R.id.ai_greeting_wrap);
        input = v.findViewById(R.id.ai_input);

        aiOnlineDot = v.findViewById(R.id.ai_online_dot);
        aiOnlineLabel = v.findViewById(R.id.ai_online_label);
        refreshOnlineStatus();

        ImageButton menu = v.findViewById(R.id.btn_ai_menu);
        menu.setOnClickListener(x -> drawer.openDrawer(GravityCompat.END));

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
        v.findViewById(R.id.btn_new_chat_toolbar).setOnClickListener(x -> startNewChat());
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
        boolean on = NetworkUtils.isOnline(requireContext());
        int dot =
                on
                        ? getResources().getColor(R.color.accent_purple, null)
                        : getResources().getColor(R.color.text_muted, null);
        aiOnlineDot.setBackgroundTintList(ColorStateList.valueOf(dot));
        aiOnlineLabel.setText(
                on ? R.string.ai_status_online : R.string.ai_status_offline);
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

        Runnable afterUserSaved =
                () ->
                        aiModel.sendMessage(
                                msg,
                                new GitHubModelService.Callback() {
                                    @Override
                                    public void onResult(String result) {
                                        if (!isAdded()) return;
                                        removeTypingIndicator();
                                        addBubble(result, false);
                                        addAssistantActionsRow(result);
                                        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                                        if (currentSessionId != null) {
                                            chatRepo.saveAssistantMessage(
                                                    currentSessionId,
                                                    result,
                                                    null);
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        if (!isAdded()) return;
                                        removeTypingIndicator();
                                        String fallback = "(" + error + ")\n\n" + cannedReply(msg);
                                        addBubble(fallback, false);
                                        if (currentSessionId != null) {
                                            chatRepo.saveAssistantMessage(
                                                    currentSessionId, fallback, null);
                                        }
                                        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                                    }
                                });

        if (currentSessionId == null) {
            String title = msg.length() > 36 ? msg.substring(0, 36) + "…" : msg;
            chatRepo.createSession(
                    title,
                    id -> {
                        currentSessionId = id;
                        chatRepo.saveUserMessage(id, msg, afterUserSaved);
                    });
        } else {
            chatRepo.saveUserMessage(currentSessionId, msg, afterUserSaved);
        }
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
        return "Offline mode: walang GitHub token o walang network. Ilagay ang GITHUB_TOKEN sa local.properties (GitHub Settings → Tokens).";
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
        } else {
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setBackgroundResource(R.drawable.bg_code_block);
            tv.setTextColor(getResources().getColor(R.color.accent_cyan, null));
        }
        chatContainer.addView(tv);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }
}
