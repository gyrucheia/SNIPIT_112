package com.example.snipit.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;

public class PortRowAdapter extends RecyclerView.Adapter<PortRowAdapter.VH> {

    private final String[][] rows;

    public PortRowAdapter(String[][] rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_port, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.num.setText(rows[position][0]);
        h.desc.setText(rows[position][1]);
    }

    @Override
    public int getItemCount() {
        return rows.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView num;
        final TextView desc;

        VH(View v) {
            super(v);
            num = v.findViewById(R.id.port_number);
            desc = v.findViewById(R.id.port_desc);
        }
    }
}
