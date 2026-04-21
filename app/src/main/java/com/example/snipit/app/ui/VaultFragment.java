package com.example.snipit.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.EditSnipActivity;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
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
    private String selectedLanguage = null;

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

        RecyclerView rv = v.findViewById(R.id.vault_recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SnipAdapter(this);
        rv.setAdapter(adapter);

        EditText search = v.findViewById(R.id.search_bar);
        if (search != null) {
            search.addTextChangedListener(
                    new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            searchQuery = s != null ? s.toString().trim() : "";
                            selectedLanguage = null;
                            applyFilter();
                        }

                        @Override
                        public void afterTextChanged(Editable s) {}
                    });
            search.setOnEditorActionListener(
                    (tv, actionId, event) -> {
                        boolean isSearch =
                                actionId == EditorInfo.IME_ACTION_SEARCH
                                        || (event != null
                                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                                && event.getAction() == KeyEvent.ACTION_DOWN);
                        if (!isSearch) return false;
                        hideKeyboard(search);
                        return true;
                    });
        }

        LinearLayout tagRow = v.findViewById(R.id.filter_chips_container);
        if (tagRow != null) buildTagRow(tagRow);

        View fab = v.findViewById(R.id.btn_add_snip);
        if (fab != null) {
            fab.setOnClickListener(
                x -> startActivity(new Intent(requireContext(), NewSnipActivity.class)));
        }

        repo.getAllSnips()
                .observe(
                        getViewLifecycleOwner(),
                        snips -> {
                            lastAll = snips != null ? new ArrayList<>(snips) : new ArrayList<>();
                            applyFilter();
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
        List<String> tokens = tokenize(searchQuery);
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
            if (!tokens.isEmpty()) {
                String hay =
                        ((s.title != null ? s.title : "")
                                        + "\n"
                                        + (s.code != null ? s.code : "")
                                        + "\n"
                                        + (s.tags != null ? s.tags : "")
                                        + "\n"
                                        + (s.language != null ? s.language : ""))
                                .toLowerCase();
                boolean ok = true;
                for (String t : tokens) {
                    if (!hay.contains(t)) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;
            }
            out.add(s);
        }
        adapter.setHighlightTokens(tokens);
        adapter.setItems(out);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private static List<String> tokenize(String q) {
        List<String> out = new ArrayList<>();
        if (q == null) return out;
        String[] parts = q.trim().split("\\s+");
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim().toLowerCase();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onOpenEditor(Snip snip) {
        Intent intent = new Intent(requireContext(), EditSnipActivity.class);
        intent.putExtra("snip_id", snip.id);
        startActivity(intent);
    }

    @Override
    public void onBeam(Snip snip) {
        // Implement beam logic if needed
    }

    @Override
    public void onFixWithAi(Snip snip) {
        if (getActivity() instanceof com.example.snipit.app.MainActivity) {
            com.example.snipit.app.MainActivity main = (com.example.snipit.app.MainActivity) getActivity();
            // Switch to AI Tab (Index 3)
            main.switchTab(3);
            
            // Post a small delay to allow the fragment to load, then send the code
            View view = getView();
            if (view != null) {
                view.postDelayed(() -> {
                    AIFragment aiFrag = (AIFragment) getParentFragmentManager().findFragmentByTag("frag_ai");
                    if (aiFrag != null && snip.code != null) {
                        aiFrag.startFixSession(snip.code);
                    }
                }, 300);
            }
        }
    }

    @Override
    public void onCopy(Snip snip) {
        android.content.ClipboardManager cb = (android.content.ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData data = android.content.ClipData.newPlainText("SnipIT Code", snip.code);
        cb.setPrimaryClip(data);
        android.widget.Toast.makeText(requireContext(), "Code copied!", android.widget.Toast.LENGTH_SHORT).show();
    }
}
