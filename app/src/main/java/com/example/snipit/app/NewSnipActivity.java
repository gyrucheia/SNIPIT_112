package com.example.snipit.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.util.AutoTagger;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.services.GitHubModelService;
import com.example.snipit.app.util.LanguagePickerHelper;
import com.example.snipit.app.util.XpManager;
import com.google.android.material.button.MaterialButton;

public class NewSnipActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_TITLE = "prefill_title";
    public static final String EXTRA_PREFILL_CODE = "prefill_code";
    public static final String EXTRA_PREFILL_LANG = "prefill_lang";
    public static final String EXTRA_PREFILL_TAGS = "prefill_tags";

    private final GitHubModelService aiModel = new GitHubModelService();
    private EditText titleInput, tagsInput, codeInput;
    private AutoCompleteTextView langInput;
    private ProgressBar aiProgress;
    private TextView aiStatusLabel;
    private MaterialButton btnFix;
    private ImageButton btnScan, btnPaste;
    private MaterialButton btnCommit;

    // OCR Integration
    private final ActivityResultLauncher<Intent> snapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String scannedText = result.getData().getStringExtra("scanned_text");
                    if (scannedText != null && !scannedText.isEmpty()) {
                        codeInput.append("\n" + scannedText);
                        Toast.makeText(this, "Text imported from scan", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_snip);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(bars.bottom, ime.bottom);
            v.setPadding(bars.left, bars.top, bars.right, bottom);
            return insets;
        });

        initViews();
        setupLogic();
        handleIntent();
    }

    private void initViews() {
        titleInput = findViewById(R.id.input_title_pro);
        langInput = findViewById(R.id.input_language_pro);
        tagsInput = findViewById(R.id.input_tags_pro);
        codeInput = findViewById(R.id.input_code_pro);
        aiProgress = findViewById(R.id.progress_ai_pro);
        aiStatusLabel = findViewById(R.id.ai_status_label);
        btnFix = findViewById(R.id.btn_fix_ai_pro);
        btnScan = findViewById(R.id.btn_scan_pro);
        btnPaste = findViewById(R.id.btn_paste_pro);
        btnCommit = findViewById(R.id.btn_commit_pro);
        
        findViewById(R.id.btn_cancel_pro).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share_pro).setOnClickListener(v -> shareSnip());
    }

    private void setupLogic() {
        LanguagePickerHelper.bind(langInput, this);

        // Haptic "Click" for Commit
        btnCommit.setOnClickListener(v -> {
            triggerHaptic();
            commitToVault();
        });

        // 📋 Paste Logic
        btnPaste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        codeInput.append(text);
                        Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 📸 Scan Logic
        btnScan.setOnClickListener(v -> {
            // Since we are already in NewSnipActivity, clicking Scan should 
            // ideally launch a simple OCR activity or go to the Snap tab?
            // User wants: Snap -> Editor -> Vault. 
            // If they are already in Editor and want to scan more, launch OcrScanActivity.
            Intent intent = new Intent(this, OcrScanActivity.class);
            snapLauncher.launch(intent);
        });

        // ✨ AI Fix Logic
        btnFix.setOnClickListener(v -> performAiFix());

        // 🏷️ Auto-Tagger & 🎨 Syntax Highlighting
        codeInput.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return;
                
                // 1. Auto-Tagger logic
                if (tagsInput.getText() == null || tagsInput.getText().toString().trim().isEmpty()) {
                    String langLabel = LanguagePickerHelper.vaultLabel(langInput.getText().toString());
                    String suggested = AutoTagger.suggest(s.toString(), langLabel);
                    if (!suggested.isEmpty()) tagsInput.setText(suggested);
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                
                String currentLang = LanguagePickerHelper.vaultLabel(langInput.getText().toString());
                Spannable highlighted = com.example.snipit.app.util.CodeHighlighter.highlight(NewSnipActivity.this, s.toString(), currentLang);
                
                // Preserve selection
                int selectionStart = codeInput.getSelectionStart();
                int selectionEnd = codeInput.getSelectionEnd();
                
                codeInput.setText(highlighted);
                codeInput.setSelection(selectionStart, selectionEnd);
                
                isFormatting = false;
            }
        });
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        if (intent.hasExtra(EXTRA_PREFILL_TITLE)) {
            titleInput.setText(intent.getStringExtra(EXTRA_PREFILL_TITLE));
        }
        if (intent.hasExtra(EXTRA_PREFILL_CODE)) {
            codeInput.setText(intent.getStringExtra(EXTRA_PREFILL_CODE));
        }
        if (intent.hasExtra(EXTRA_PREFILL_TAGS)) {
            tagsInput.setText(intent.getStringExtra(EXTRA_PREFILL_TAGS));
        }
        if (intent.hasExtra(EXTRA_PREFILL_LANG)) {
            LanguagePickerHelper.setDisplayFromVaultLabel(langInput, this, intent.getStringExtra(EXTRA_PREFILL_LANG));
        }
    }

    private void performAiFix() {
        String code = codeInput.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.fix_ai_need_code, Toast.LENGTH_SHORT).show();
            return;
        }

        btnFix.setEnabled(false);
        aiProgress.setVisibility(View.VISIBLE);
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("● AI ENGINE PROCESSING...");
            aiStatusLabel.setTextColor(getResources().getColor(R.color.accent_purple));
        }
        String langHint = LanguagePickerHelper.vaultLabel(langInput.getText().toString());

        aiModel.improveSnippet(code, langHint, new GitHubModelService.Callback() {
            @Override
            public void onResult(String result) {
                runOnUiThread(() -> {
                    aiProgress.setVisibility(View.GONE);
                    if (aiStatusLabel != null) {
                        aiStatusLabel.setText("● REFINEMENT COMPLETE");
                        aiStatusLabel.setTextColor(getResources().getColor(R.color.accent_green));
                    }
                    btnFix.setEnabled(true);
                    codeInput.setText(result);
                    triggerHaptic();
                    Toast.makeText(NewSnipActivity.this, "AI Refactored ✓", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    aiProgress.setVisibility(View.GONE);
                    if (aiStatusLabel != null) {
                        aiStatusLabel.setText("● AI ENGINE ERROR");
                        aiStatusLabel.setTextColor(getResources().getColor(R.color.error_red));
                    }
                    btnFix.setEnabled(true);
                    Toast.makeText(NewSnipActivity.this, "AI Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void commitToVault() {
        String t = titleInput.getText().toString().trim();
        String l = LanguagePickerHelper.vaultLabel(langInput.getText().toString());
        String tg = tagsInput.getText().toString().trim();
        String c = codeInput.getText().toString().trim();

        if (c.isEmpty()) {
            Toast.makeText(this, "No code to commit", Toast.LENGTH_SHORT).show();
            return;
        }

        Snip s = new Snip();
        s.title = t.isEmpty() ? "New Snippet" : t;
        s.language = l;
        s.tags = tg;
        s.code = c;

        new SnipRepository(getApplication()).insert(s, () -> {
            XpManager.addXp(this, 15);
            setResult(Activity.RESULT_OK);
            finish();
        });
    }

    private void shareSnip() {
        String payload = "Snippet: " + titleInput.getText().toString() + "\n\n" + codeInput.getText().toString();
        Intent si = new Intent(Intent.ACTION_SEND);
        si.setType("text/plain");
        si.putExtra(Intent.EXTRA_TEXT, payload);
        startActivity(Intent.createChooser(si, "Share Snippet"));
    }

    private void triggerHaptic() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(20);
            }
        }
    }
}
