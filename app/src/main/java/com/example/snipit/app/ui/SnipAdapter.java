package com.example.snipit.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.models.Snip;
import java.util.ArrayList;
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
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_snip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Snip s = items.get(position);
        h.title.setText(s.title != null ? s.title : "(untitled)");
        h.lang.setText(s.language != null ? s.language : "—");
        h.code.setText(s.code != null ? s.code : "");
        h.tags.setText(s.tags != null ? s.tags.replace(",", "  ") : "");
        h.btnCopy.setOnClickListener(v -> listener.onCopy(s));
        h.btnBeam.setOnClickListener(v -> listener.onBeam(s));
        h.btnFixAi.setOnClickListener(v -> listener.onFixWithAi(s));
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
}
