package com.example.snipit.app.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.R;
import com.example.snipit.app.util.XpManager;

public class AIFragment extends Fragment {

    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText input;

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
        chatContainer = v.findViewById(R.id.chat_container);
        chatScroll = v.findViewById(R.id.chat_scroll);
        input = v.findViewById(R.id.ai_input);

        v.findViewById(R.id.btn_send).setOnClickListener(x -> send());

        v.findViewById(R.id.chip_tagalog)
                .setOnClickListener(x -> input.setText("Explain this in Tagalog:\n"));
        v.findViewById(R.id.chip_kotlin)
                .setOnClickListener(x -> input.setText("Convert this Java to Kotlin:\n"));
        v.findViewById(R.id.chip_optimize)
                .setOnClickListener(x -> input.setText("Optimize this for mobile:\n"));
        v.findViewById(R.id.chip_debug)
                .setOnClickListener(x -> input.setText("Find the bug in:\n"));
    }

    private void send() {
        String msg = input.getText() != null ? input.getText().toString().trim() : "";
        if (msg.isEmpty()) return;
        addBubble(msg, true);
        input.setText("");
        XpManager.addXp(requireContext(), 3);

        String reply = cannedReply(msg);
        chatScroll.post(
                () ->
                        chatScroll.postDelayed(
                                () -> {
                                    addBubble(reply, false);
                                    chatScroll.fullScroll(View.FOCUS_DOWN);
                                },
                                120));
    }

    private String cannedReply(String msg) {
        String m = msg.toLowerCase();
        if (m.contains("tagalog")) {
            return "Offline mode: Ito ay halimbawa ng sagot. I-save ang snippet sa Vault pagkatapos "
                    + "i-refactor para hindi mawala ang logic.";
        }
        if (m.contains("kotlin")) {
            return "fun main() {\n    println(\"Snip-AI offline demo\")\n}\n\n(Tap + sa Vault para i-save ang Kotlin na ito.)";
        }
        if (m.contains("optimize") || m.contains("mobile")) {
            return "Tip: bawasan ang allocations sa loop, gumamit ng RecyclerView.DiffUtil, "
                    + "at iwasan ang heavy work sa main thread.";
        }
        if (m.contains("bug")) {
            return "Check: null safety, index bounds, at lifecycle (Activity destroyed na ba?).";
        }
        return "Snip-AI (offline demo): Naka-record lang ang sagot — walang cloud API. "
                + "Para sa tunay na AI, pwede mong idugtong ang Gemini/OpenAI sa thesis v2.";
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
