package com.example.snipit.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.data.DexContent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HttpRowAdapter extends RecyclerView.Adapter<HttpRowAdapter.VH> {

    private final String[][] rows;

    public HttpRowAdapter(String[][] rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_http_code, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String code = rows[position][0];
        h.code.setText(code);
        h.desc.setText(rows[position][1]);
        h.itemView.setOnClickListener(
                v ->
                        new MaterialAlertDialogBuilder(v.getContext())
                                .setTitle("HTTP " + code)
                                .setMessage(DexContent.httpLongExplain(code))
                                .setPositiveButton(android.R.string.ok, null)
                                .show());
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
            code = v.findViewById(R.id.http_status_code);
            desc = v.findViewById(R.id.http_status_desc);
        }
    }
}
