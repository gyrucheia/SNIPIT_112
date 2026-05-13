package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.models.HandbookEntry;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.util.XpManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class HandbookAdapter extends RecyclerView.Adapter<HandbookAdapter.VH> {

    private final List<HandbookEntry> entries;

    public HandbookAdapter(List<HandbookEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_handbook, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        HandbookEntry e = entries.get(position);
        h.category.setText(e.getCategory());
        h.code.setText(com.example.snipit.app.util.CodeHighlighter.highlight(h.itemView.getContext(), e.getCommand(), e.getCategory()));
        h.summary.setText(e.getSummary());

        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            String body = e.getSummary() + "\n\n" + e.getDocumentation();
            
            new MaterialAlertDialogBuilder(ctx)
                .setTitle(e.getCommand())
                .setMessage(body)
                .setNegativeButton("DISMISS", null)
                .setNeutralButton("SNIP TO VAULT", (dialog, which) -> {
                    Intent intent = new Intent(ctx, NewSnipActivity.class);
                    intent.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, e.getCommand());
                    intent.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, e.getCommand());
                    intent.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, e.getCategory());
                    intent.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#DevDex,#" + e.getCategory());
                    ctx.startActivity(intent);
                })
                .setPositiveButton("COPY COMMAND", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("cmd", e.getCommand()));
                    BadgeTracker.recordDexCommandCopy(ctx);
                    XpManager.addXp(ctx, 1);
                    Toast.makeText(ctx, "Command copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .show();
        });
    }

    public void updateEntries(List<HandbookEntry> newEntries) {
        this.entries.clear();
        this.entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView category, code, summary;

        VH(View v) {
            super(v);
            category = v.findViewById(R.id.entry_category);
            code = v.findViewById(R.id.entry_code);
            summary = v.findViewById(R.id.entry_summary);
        }
    }
}
