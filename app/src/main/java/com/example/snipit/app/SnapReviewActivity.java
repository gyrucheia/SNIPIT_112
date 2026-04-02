package com.example.snipit.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.services.GitHubModelService;
import com.example.snipit.app.util.LanguagePickerHelper;
import com.example.snipit.app.util.OcrConfidence;

public class SnapReviewActivity extends AppCompatActivity {

    public static final String EXTRA_OCR_TEXT = "ocr_text";
    public static final String EXTRA_CONFIDENCE_PERCENT = "confidence_pct";
    public static final String EXTRA_CONFIDENCE_LEVEL = "confidence_level";

    private EditText editor;
    private TextView confidenceBadge;
    private AutoCompleteTextView langPicker;
    private ProgressBar aiFixProgress;
    private Button btnUndo;
    private Button btnRedo;
    private Button btnFormat;
    private Button btnAiFix;

    private final GitHubModelService aiModel = new GitHubModelService();

    private ActivityResultLauncher<Intent> newSnipLauncher;

    private boolean programmaticText;
    private final java.util.List<String> history = new java.util.ArrayList<>();
    private int historyPtr = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        newSnipLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                setResult(Activity.RESULT_OK);
                                finish();
                            }
                        });
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_snap_review);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        editor = findViewById(R.id.snap_editor);
        confidenceBadge = findViewById(R.id.confidence_badge);
        langPicker = findViewById(R.id.lang_picker);
        aiFixProgress = findViewById(R.id.ai_fix_progress);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnFormat = findViewById(R.id.btn_format);
        btnAiFix = findViewById(R.id.btn_ai_fix);

        String initial =
                getIntent() != null && getIntent().hasExtra(EXTRA_OCR_TEXT)
                        ? getIntent().getStringExtra(EXTRA_OCR_TEXT)
                        : "";
        if (initial == null) initial = "";

        int pct = getIntent() != null ? getIntent().getIntExtra(EXTRA_CONFIDENCE_PERCENT, -1) : -1;
        String levelName =
                getIntent() != null ? getIntent().getStringExtra(EXTRA_CONFIDENCE_LEVEL) : null;
        if (pct < 0) {
            OcrConfidence.Result r = OcrConfidence.analyze(initial);
            pct = r.percent;
            levelName = r.level.name();
        }
        bindConfidenceBadge(pct, levelName);

        LanguagePickerHelper.bind(langPicker, this);

        initHistory(initial);
        setProgrammaticText(initial);

        editor.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (programmaticText) return;
                        String cur = s != null ? s.toString() : "";
                        if (historyPtr >= 0 && cur.equals(history.get(historyPtr))) return;
                        pushHistory(cur);
                    }
                });

        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());
        btnFormat.setOnClickListener(v -> applyFormat());
        btnAiFix.setOnClickListener(v -> runAiFix());
        findViewById(R.id.btn_ai_fix_first).setOnClickListener(v -> runAiFix());

        findViewById(R.id.btn_snap_save)
                .setOnClickListener(
                        v -> {
                            String code = editor.getText() != null ? editor.getText().toString() : "";
                            String langHint =
                                    langPicker.getText() != null
                                            ? langPicker.getText().toString()
                                            : "Text";
                            Intent i = new Intent(this, NewSnipActivity.class);
                            i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "Board capture");
                            i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, code);
                            i.putExtra(
                                    NewSnipActivity.EXTRA_PREFILL_LANG,
                                    LanguagePickerHelper.vaultLabel(langHint));
                            i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#OCR,#Snap");
                            newSnipLauncher.launch(i);
                        });
    }

    private void bindConfidenceBadge(int pct, String levelName) {
        int color = Color.parseColor("#888888");
        int bg = R.drawable.bg_conf_low;
        if ("HIGH".equalsIgnoreCase(levelName)) {
            color = getResources().getColor(R.color.accent_green, null);
            bg = R.drawable.bg_conf_high;
        } else if ("MEDIUM".equalsIgnoreCase(levelName)) {
            color = getResources().getColor(R.color.accent_yellow, null);
            bg = R.drawable.bg_conf_med;
        } else if ("LOW".equalsIgnoreCase(levelName)) {
            color = getResources().getColor(R.color.accent_red, null);
            bg = R.drawable.bg_conf_low;
        }
        confidenceBadge.setTextColor(color);
        confidenceBadge.setBackgroundResource(bg);
        String line =
                "HIGH".equalsIgnoreCase(levelName)
                        ? ("✓ " + pct + "% confidence")
                        : (pct + "% · " + (levelName != null ? levelName : "?"));
        confidenceBadge.setText(line);
    }

    private void initHistory(String initial) {
        history.clear();
        history.add(initial);
        historyPtr = 0;
        updateUndoRedo();
    }

    private void pushHistory(String s) {
        while (history.size() > historyPtr + 1) {
            history.remove(history.size() - 1);
        }
        if (historyPtr >= 0 && s.equals(history.get(historyPtr))) {
            updateUndoRedo();
            return;
        }
        history.add(s);
        historyPtr = history.size() - 1;
        while (history.size() > 50) {
            history.remove(0);
            historyPtr--;
        }
        updateUndoRedo();
    }

    private void setProgrammaticText(String s) {
        programmaticText = true;
        editor.setText(s);
        editor.setSelection(s.length());
        programmaticText = false;
    }

    private void undo() {
        if (historyPtr <= 0) return;
        historyPtr--;
        setProgrammaticText(history.get(historyPtr));
        updateUndoRedo();
    }

    private void redo() {
        if (historyPtr >= history.size() - 1) return;
        historyPtr++;
        setProgrammaticText(history.get(historyPtr));
        updateUndoRedo();
    }

    private void updateUndoRedo() {
        btnUndo.setEnabled(historyPtr > 0);
        btnRedo.setEnabled(historyPtr < history.size() - 1);
    }

    private void applyFormat() {
        String raw = editor.getText() != null ? editor.getText().toString() : "";
        String formatted = simpleFormat(raw);
        if (formatted.equals(raw)) return;
        setProgrammaticText(formatted);
    }

    private static String simpleFormat(String raw) {
        String[] lines = raw.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.replaceAll("\\s+$", "")).append("\n");
        }
        String out = sb.toString().replaceAll("\n{3,}", "\n\n");
        return out.trim() + (out.endsWith("\n") ? "" : "\n");
    }

    private void runAiFix() {
        String raw = editor.getText() != null ? editor.getText().toString() : "";
        String lang = langPicker.getText() != null ? langPicker.getText().toString() : "";
        btnAiFix.setEnabled(false);
        aiFixProgress.setVisibility(View.VISIBLE);

        aiModel.fixOcrText(
                raw,
                lang,
                new GitHubModelService.Callback() {
                    @Override
                    public void onResult(String result) {
                        aiFixProgress.setVisibility(View.GONE);
                        btnAiFix.setEnabled(true);
                        if (result != null) {
                            setProgrammaticText(result.trim());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        aiFixProgress.setVisibility(View.GONE);
                        btnAiFix.setEnabled(true);
                    }
                });
    }
}
