package com.example.snipit.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.models.Snip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SnipAdapter extends RecyclerView.Adapter<SnipAdapter.VH> {

    public interface Listener {
        void onCopy(Snip snip);

        void onBeam(Snip snip);

        /** Opens the full IDE-style editor for this vault snip. */
        void onOpenEditor(Snip snip);

        /** Same vault row — opens editor (Fix with AI is available inside the editor). */
        void onFixWithAi(Snip snip);
    }

    private final List<Snip> items = new ArrayList<>();
    private final Listener listener;
    private List<String> highlightTokens = Collections.emptyList();

    public SnipAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setHighlightTokens(List<String> tokens) {
        highlightTokens = tokens != null ? tokens : Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setItems(List<Snip> snips) {
        items.clear();
        if (snips != null) items.addAll(snips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_snip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Snip s = items.get(position);
        h.title.setText(highlight(s.title != null ? s.title : "(untitled)"));
        h.lang.setText(highlight(s.language != null ? s.language : "—"));
        h.code.setText(highlight(s.code != null ? s.code : ""));
        h.tags.setText(highlight(s.tags != null ? s.tags.replace(",", "  ") : ""));
        h.btnCopy.setOnClickListener(v -> listener.onCopy(s));
        h.btnBeam.setOnClickListener(v -> listener.onBeam(s));
        h.btnFixAi.setOnClickListener(v -> listener.onFixWithAi(s));
        h.clickArea.setOnClickListener(v -> listener.onOpenEditor(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View clickArea;
        final TextView title;
        final TextView lang;
        final TextView code;
        final TextView tags;
        final TextView btnCopy;
        final TextView btnBeam;
        final TextView btnFixAi;

        VH(View v) {
            super(v);
            clickArea = v.findViewById(R.id.snip_click_area);
            title = v.findViewById(R.id.snip_title);
            lang = v.findViewById(R.id.snip_language);
            code = v.findViewById(R.id.snip_code);
            tags = v.findViewById(R.id.snip_tags);
            btnCopy = v.findViewById(R.id.btn_copy);
            btnBeam = v.findViewById(R.id.btn_beam);
            btnFixAi = v.findViewById(R.id.btn_fix_ai);
        }
    }

    private CharSequence highlight(String text) {
        if (text == null) return "";
        if (highlightTokens == null || highlightTokens.isEmpty()) return text;
        String lower = text.toLowerCase();
        SpannableString sp = new SpannableString(text);
        int color = sp.length() > 0 ? HIGHLIGHT_BG : 0;
        for (String raw : highlightTokens) {
            if (raw == null) continue;
            String token = raw.trim().toLowerCase();
            if (token.isEmpty()) continue;
            int from = 0;
            while (from < lower.length()) {
                int idx = lower.indexOf(token, from);
                if (idx < 0) break;
                int end = idx + token.length();
                if (end > idx) {
                    sp.setSpan(new BackgroundColorSpan(color), idx, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                from = end;
            }
        }
        return sp;
    }

    private static final int HIGHLIGHT_BG = 0x3347FF9A;
}
