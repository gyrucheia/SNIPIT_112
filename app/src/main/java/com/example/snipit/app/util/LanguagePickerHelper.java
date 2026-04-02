package com.example.snipit.app.util;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.example.snipit.app.R;

/** Shared searchable language dropdown (emoji + label) for Snap review and Vault editor. */
public final class LanguagePickerHelper {

    private LanguagePickerHelper() {}

    public static void bind(AutoCompleteTextView field, Context context) {
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        context,
                        R.array.language_picker,
                        android.R.layout.simple_dropdown_item_1line);
        field.setAdapter(adapter);
        field.setThreshold(1);
        String[] langs = context.getResources().getStringArray(R.array.language_picker);
        field.setText(langs[langs.length - 1], false);
    }

    /** Sets display from a saved Vault label (e.g. "Kotlin") by finding the best array match. */
    public static void setDisplayFromVaultLabel(AutoCompleteTextView field, Context context, String saved) {
        if (saved == null || saved.trim().isEmpty()) {
            bind(field, context);
            return;
        }
        String[] langs = context.getResources().getStringArray(R.array.language_picker);
        String lower = saved.trim().toLowerCase();
        for (String row : langs) {
            if (vaultLabel(row).toLowerCase().equals(lower)) {
                field.setText(row, false);
                return;
            }
        }
        for (String row : langs) {
            if (vaultLabel(row).toLowerCase().contains(lower) || lower.contains(vaultLabel(row).toLowerCase())) {
                field.setText(row, false);
                return;
            }
        }
        field.setText(saved, false);
    }

    /**
     * Value to store in {@link com.example.snipit.app.models.Snip#language} — no leading emoji,
     * stable for filters.
     */
    public static String vaultLabel(String displayRow) {
        if (displayRow == null) return "Text";
        String t = displayRow.trim();
        int sp = t.indexOf(' ');
        if (sp > 0 && sp < t.length() - 1) {
            return t.substring(sp + 1).trim();
        }
        return t.isEmpty() ? "Text" : t;
    }
}
