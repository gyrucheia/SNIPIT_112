package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.util.XpManager;

public class CmdRowAdapter extends RecyclerView.Adapter<CmdRowAdapter.VH> {

    private final String[][] rows;

    public CmdRowAdapter(String[][] rows) {
        this.rows = rows;
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
        String cmd = rows[position][0];
        h.code.setText(cmd);
        h.desc.setText(rows[position][1]);
        h.itemView.setOnClickListener(
                v -> {
                    ClipboardManager cm =
                            (ClipboardManager)
                                    v.getContext()
                                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("cmd", cmd));
                    XpManager.addXp(v.getContext(), 1);
                    Toast.makeText(v.getContext(), "Copied", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return rows.length;
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
