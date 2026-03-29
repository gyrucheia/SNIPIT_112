package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.EditSnipActivity;
import com.example.snipit.app.MainActivity;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.QrScanActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.util.XpManager;
import java.util.ArrayList;
import java.util.List;

public class VaultFragment extends Fragment implements SnipAdapter.Listener {

    private static final String[] TAG_FILTERS =
            new String[] {"All", "#Java", "#Kotlin", "#Firebase", "#CLI", "#Git", "#Android"};

    private SnipRepository repo;
    private SnipAdapter adapter;
    private String selectedTag = "All";
    private String searchQuery = "";
    private List<Snip> lastAll = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vault, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        repo = new SnipRepository(requireActivity().getApplication());

        RecyclerView rv = v.findViewById(R.id.snip_recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SnipAdapter(this);
        rv.setAdapter(adapter);

        EditText search = v.findViewById(R.id.search_input);
        search.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchQuery = s != null ? s.toString().trim() : "";
                        applyFilter();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        LinearLayout tagRow = v.findViewById(R.id.tag_row);
        buildTagRow(tagRow);

        v.findViewById(R.id.fab_new)
                .setOnClickListener(
                        x -> startActivity(new Intent(requireContext(), NewSnipActivity.class)));

        repo.getAllSnips()
                .observe(
                        getViewLifecycleOwner(),
                        snips -> {
                            lastAll = snips != null ? new ArrayList<>(snips) : new ArrayList<>();
                            applyFilter();
                            refreshXp(v);
                        });
    }

    private void buildTagRow(LinearLayout row) {
        row.removeAllViews();
        for (String tag : TAG_FILTERS) {
            TextView chip = new TextView(requireContext());
            chip.setText(tag);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setTextSize(10);
            chip.setTypeface(Typeface.MONOSPACE);
            chip.setBackgroundResource(R.drawable.bg_tag);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(
                    x -> {
                        selectedTag = tag;
                        highlightTags(row);
                        applyFilter();
                    });
            row.addView(chip);
        }
        highlightTags(row);
    }

    private void highlightTags(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView chip = (TextView) row.getChildAt(i);
            boolean on = chip.getText().toString().equals(selectedTag);
            chip.setTextColor(
                    requireContext()
                            .getResources()
                            .getColor(on ? R.color.accent_green : R.color.text_muted, null));
        }
    }

    private void applyFilter() {
        List<Snip> out = new ArrayList<>();
        for (Snip s : lastAll) {
            if (!"All".equals(selectedTag)) {
                String needle =
                        selectedTag.startsWith("#") ? selectedTag.substring(1) : selectedTag;
                if (s.tags == null
                        || !s.tags.toLowerCase().contains(needle.toLowerCase())) {
                    continue;
                }
            }
            if (!searchQuery.isEmpty()) {
                String t = (s.title != null ? s.title : "") + (s.code != null ? s.code : "");
                if (!t.toLowerCase().contains(searchQuery.toLowerCase())) continue;
            }
            out.add(s);
        }
        adapter.setItems(out);
    }

    private void refreshXp(View root) {
        TextView xp = root.findViewById(R.id.xp_display);
        int xpVal = XpManager.getXp(requireContext());
        xp.setText(xpVal + " XP");
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    @Override
    public void onCopy(Snip snip) {
        ClipboardManager cm =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("code", snip.code != null ? snip.code : ""));
        XpManager.addXp(requireContext(), 2);
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
        repo.incrementUsage(snip.id);
    }

    @Override
    public void onBeam(Snip snip) {
        XpManager.addXp(requireContext(), 5);
        ((MainActivity) requireActivity()).openBeamForSnip(snip.id);
    }

    @Override
    public void onOpenEditor(Snip snip) {
        Intent i = new Intent(requireContext(), EditSnipActivity.class);
        i.putExtra(EditSnipActivity.EXTRA_SNIP_ID, snip.id);
        startActivity(i);
    }

    @Override
    public void onFixWithAi(Snip snip) {
        onOpenEditor(snip);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) refreshXp(v);
    }
}
