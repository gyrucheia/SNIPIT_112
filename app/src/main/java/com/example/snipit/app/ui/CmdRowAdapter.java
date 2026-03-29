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
import com.example.snipit.app.data.DexDoc;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.util.XpManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CmdRowAdapter extends RecyclerView.Adapter<CmdRowAdapter.VH> {

    private final DexDoc[] docs;

    public CmdRowAdapter(DexDoc[] docs) {
        this.docs = docs;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cmd, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DexDoc d = docs[position];
        h.code.setText(d.command);
        h.desc.setText(d.summary);
        h.itemView.setOnClickListener(
                v -> {
                    Context ctx = v.getContext();
                    String body =
                            d.summary
                                    + "\n\n"
                                    + d.documentation
                                    + "\n\n"
                                    + ctx.getString(R.string.dex_copy_explainer);
                    new MaterialAlertDialogBuilder(ctx)
                            .setTitle(d.command)
                            .setMessage(body)
                            .setNegativeButton(R.string.dex_dialog_close, null)
                            .setNeutralButton(
                                    R.string.dex_snip_to_vault,
                                    (dialog, which) -> {
                                        Intent i = new Intent(ctx, NewSnipActivity.class);
                                        String t = d.command;
                                        if (t.length() > 48) {
                                            t = t.substring(0, 45) + "…";
                                        }
                                        i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, t);
                                        i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, d.command);
                                        i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "CLI");
                                        i.putExtra(
                                                NewSnipActivity.EXTRA_PREFILL_TAGS,
                                                "#DevDex,#CLI");
                                        ctx.startActivity(i);
                                    })
                            .setPositiveButton(
                                    R.string.copy_command,
                                    (dialog, which) -> {
                                        ClipboardManager cm =
                                                (ClipboardManager)
                                                        ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                        cm.setPrimaryClip(ClipData.newPlainText("cmd", d.command));
                                        BadgeTracker.recordDexCommandCopy(ctx);
                                        XpManager.addXp(ctx, 1);
                                        Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT)
                                                .show();
                                    })
                            .show();
                });
    }

    @Override
    public int getItemCount() {
        return docs.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView code;
        final TextView desc;

        VH(View v) {
            super(v);
            code = v.findViewById(R.id.cmd_code);
            desc = v.findViewById(R.id.cmd_desc);
        }
    }
}
