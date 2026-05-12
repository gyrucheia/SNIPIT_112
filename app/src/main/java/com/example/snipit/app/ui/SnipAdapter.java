package com.example.snipit.app.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
        void onOpenEditor(Snip snip);
        void onFixWithAi(Snip snip);
        void onDelete(Snip snip);
    }

    private final List<Snip> items = new ArrayList<>();
    private final Listener listener;

    public SnipAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Snip> snips) {
        items.clear();
        if (snips != null) items.addAll(snips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_snip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Snip s = items.get(position);
        
        // Professional Header: Branded text colors
        h.title.setText(s.title != null ? s.title : "Untitled_Snippet");
        h.title.setTextColor(h.itemView.getContext().getResources().getColor(R.color.text_primary, null));

        // Language Pill: Professional branded colors (No emojis)
        String langStr = s.language != null ? s.language.toUpperCase() : "PLAINTEXT";
        h.lang.setText(langStr);
        applyLanguageStyle(h.lang, langStr);

        // Code Workspace: Mono-spaced high-contrast preview
        h.code.setText(com.example.snipit.app.util.CodeHighlighter.highlight(h.itemView.getContext(), s.code, s.language));
        
        // Tags: Subdued metadata
        h.tags.setText(s.tags != null ? s.tags.replace(",", "  ") : "");

        // Actions with Safety Checks
        h.btnEdit.setOnClickListener(v -> {
            if (listener != null && s != null) listener.onOpenEditor(s);
        });
        h.btnBeam.setOnClickListener(v -> {
            if (listener != null && s != null) listener.onBeam(s);
        });
        h.btnFixAi.setOnClickListener(v -> {
            if (listener != null && s != null) listener.onFixWithAi(s);
        });
        h.clickArea.setOnClickListener(v -> {
            if (listener != null && s != null) listener.onCopy(s);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null && s != null) listener.onDelete(s);
        });
    }

    private void applyLanguageStyle(TextView tv, String lang) {
        int color;
        switch (lang) {
            case "JAVA": case "KOTLIN": color = Color.parseColor("#f89820"); break;
            case "PYTHON": color = Color.parseColor("#3776ab"); break;
            case "JAVASCRIPT": case "JS": case "TYPESCRIPT": color = Color.parseColor("#61dafb"); break;
            case "BASH": case "SHELL": case "CLI": color = Color.parseColor("#3fb950"); break;
            case "GO": color = Color.parseColor("#00add8"); break;
            default: color = Color.parseColor("#00CCFF"); break; // Default Cyber Blue
        }
        tv.setTextColor(color);
        // Add a subtle border or background if needed via bg_search or bg_tag
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
        final View btnEdit;
        final View btnBeam;
        final View btnFixAi;
        final View btnDelete;

        VH(View v) {
            super(v);
            clickArea = v.findViewById(R.id.snip_click_area);
            title = v.findViewById(R.id.snip_title);
            lang = v.findViewById(R.id.snip_language);
            code = v.findViewById(R.id.snip_code);
            tags = v.findViewById(R.id.snip_tags);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnBeam = v.findViewById(R.id.btn_beam);
            btnFixAi = v.findViewById(R.id.btn_fix_ai);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
