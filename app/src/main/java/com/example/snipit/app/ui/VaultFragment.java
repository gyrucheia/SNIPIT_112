package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
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
    private TextView sectionLabel;
    private HorizontalScrollView historyScroll;
    private LinearLayout historyRow;
    private HorizontalScrollView liveFilterScroll;
    private LinearLayout liveFilterRow;
    private String selectedLanguage = null;

    private static final String PREFS = "vault_prefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";

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
        sectionLabel = v.findViewById(R.id.section_label);
        historyScroll = v.findViewById(R.id.search_history_scroll);
        historyRow = v.findViewById(R.id.search_history_row);
        liveFilterScroll = v.findViewById(R.id.live_filter_scroll);
        liveFilterRow = v.findViewById(R.id.live_filter_row);

        search.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchQuery = s != null ? s.toString().trim() : "";
                        selectedLanguage = null;
                        applyFilter();
                        refreshSearchHistoryUi(search.hasFocus(), searchQuery);
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
                    saveSearchHistory(searchQuery);
                    hideKeyboard(search);
                    refreshSearchHistoryUi(search.hasFocus(), searchQuery);
                    return true;
                });
        search.setOnFocusChangeListener(
                (vv, hasFocus) -> refreshSearchHistoryUi(hasFocus, searchQuery));

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
                            refreshSearchHistoryUi(search.hasFocus(), searchQuery);
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
            if (selectedLanguage != null && !selectedLanguage.isEmpty()) {
                String l = s.language != null ? s.language : "";
                if (!l.equalsIgnoreCase(selectedLanguage)) continue;
            }
            out.add(s);
        }
        adapter.setHighlightTokens(tokens);
        adapter.setItems(out);
        if (sectionLabel != null) {
            if (tokens.isEmpty()) {
                sectionLabel.setText("RECENT SNIPS");
            } else {
                sectionLabel.setText("RESULTS (" + out.size() + ")");
            }
        }
        refreshLiveFilters(tokens, out);
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

    private void saveSearchHistory(String query) {
        String q = query != null ? query.trim() : "";
        if (q.isEmpty()) return;
        android.content.SharedPreferences sp =
                requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_SEARCH_HISTORY, "");
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split("\n");
            for (String p : parts) {
                if (p != null && !p.trim().isEmpty()) list.add(p.trim());
            }
        }
        // de-dup (case-insensitive)
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).equalsIgnoreCase(q)) list.remove(i);
        }
        list.add(0, q);
        while (list.size() > 5) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(list.get(i));
        }
        sp.edit().putString(KEY_SEARCH_HISTORY, sb.toString()).apply();
    }

    private List<String> loadSearchHistory() {
        android.content.SharedPreferences sp =
                requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_SEARCH_HISTORY, "");
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split("\n");
            for (String p : parts) {
                if (p != null && !p.trim().isEmpty()) list.add(p.trim());
            }
        }
        return list;
    }

    private void refreshSearchHistoryUi(boolean hasFocus, String query) {
        if (historyScroll == null || historyRow == null) return;
        String q = query != null ? query.trim() : "";
        List<String> history = loadSearchHistory();
        boolean show = hasFocus && q.isEmpty() && !history.isEmpty();
        historyScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        historyRow.removeAllViews();
        for (String h : history) {
            TextView chip = makeChip(h);
            chip.setOnClickListener(
                    v -> {
                        View root = getView();
                        if (root == null) return;
                        EditText search = root.findViewById(R.id.search_input);
                        search.setText(h);
                        search.setSelection(search.getText().length());
                        searchQuery = h;
                        applyFilter();
                        saveSearchHistory(h);
                        refreshSearchHistoryUi(search.hasFocus(), searchQuery);
                    });
            historyRow.addView(chip);
        }
    }

    private void refreshLiveFilters(List<String> tokens, List<Snip> results) {
        if (liveFilterScroll == null || liveFilterRow == null) return;
        boolean show = tokens != null && !tokens.isEmpty() && results != null && !results.isEmpty();
        liveFilterScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (Snip s : results) {
            String l = s.language != null ? s.language.trim() : "";
            if (l.isEmpty()) l = "—";
            counts.put(l, (counts.containsKey(l) ? counts.get(l) : 0) + 1);
        }
        List<java.util.Map.Entry<String, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        liveFilterRow.removeAllViews();
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> e : list) {
            if (shown >= 5) break;
            String lang = e.getKey();
            int n = e.getValue();
            String label = lang + " (" + n + ")";
            TextView chip = makeChip(label);
            boolean on = selectedLanguage != null && selectedLanguage.equalsIgnoreCase(lang);
            chip.setTextColor(
                    requireContext()
                            .getResources()
                            .getColor(on ? R.color.accent_green : R.color.text_muted, null));
            chip.setOnClickListener(
                    v -> {
                        if (selectedLanguage != null && selectedLanguage.equalsIgnoreCase(lang)) {
                            selectedLanguage = null;
                        } else {
                            selectedLanguage = lang.equals("—") ? "" : lang;
                        }
                        applyFilter();
                    });
            liveFilterRow.addView(chip);
            shown++;
        }
    }

    private TextView makeChip(String text) {
        TextView chip = new TextView(requireContext());
        chip.setText(text);
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
        chip.setTextColor(getResources().getColor(R.color.text_muted, null));
        return chip;
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
