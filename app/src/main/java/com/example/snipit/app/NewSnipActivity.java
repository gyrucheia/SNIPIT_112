package com.example.snipit.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
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

public class NewSnipActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_TITLE = "prefill_title";
    public static final String EXTRA_PREFILL_CODE = "prefill_code";
    public static final String EXTRA_PREFILL_LANG = "prefill_lang";
    public static final String EXTRA_PREFILL_TAGS = "prefill_tags";

    private final GitHubModelService aiModel = new GitHubModelService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_snip);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        EditText title = findViewById(R.id.input_title);
        AutoCompleteTextView lang = findViewById(R.id.input_language);
        EditText tags = findViewById(R.id.input_tags);
        EditText code = findViewById(R.id.input_code);
        ProgressBar fixProgress = findViewById(R.id.progress_fix_ai);
        View btnFixAi = findViewById(R.id.btn_fix_with_ai);

        LanguagePickerHelper.bind(lang, this);
        if (getIntent() != null && getIntent().hasExtra(EXTRA_PREFILL_LANG)) {
            LanguagePickerHelper.setDisplayFromVaultLabel(
                    lang, this, getIntent().getStringExtra(EXTRA_PREFILL_LANG));
        }

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_PREFILL_TITLE)) {
                title.setText(getIntent().getStringExtra(EXTRA_PREFILL_TITLE));
            }
            if (getIntent().hasExtra(EXTRA_PREFILL_CODE)) {
                code.setText(getIntent().getStringExtra(EXTRA_PREFILL_CODE));
            }
            if (getIntent().hasExtra(EXTRA_PREFILL_TAGS)) {
                tags.setText(getIntent().getStringExtra(EXTRA_PREFILL_TAGS));
            }
        }

        code.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (tags.getText() == null || tags.getText().toString().trim().length() > 0) {
                            return;
                        }
                        String langLabel =
                                LanguagePickerHelper.vaultLabel(
                                        lang.getText() != null ? lang.getText().toString() : "");
                        String suggested = AutoTagger.suggest(s.toString(), langLabel);
                        if (!suggested.isEmpty()) {
                            tags.setText(suggested);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        btnFixAi.setOnClickListener(
                v -> {
                    String body = safe(code);
                    if (body.isEmpty()) {
                        Toast.makeText(this, R.string.fix_ai_need_code, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String langHint =
                            LanguagePickerHelper.vaultLabel(
                                    lang.getText() != null ? lang.getText().toString() : "");
                    btnFixAi.setEnabled(false);
                    fixProgress.setVisibility(View.VISIBLE);
                    aiModel.improveSnippet(
                            body,
                            langHint,
                            new GitHubModelService.Callback() {
                                @Override
                                public void onResult(String result) {
                                    fixProgress.setVisibility(View.GONE);
                                    btnFixAi.setEnabled(true);
                                    if (result != null) {
                                        code.setText(result.trim());
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    fixProgress.setVisibility(View.GONE);
                                    btnFixAi.setEnabled(true);
                                    Toast.makeText(
                                                    NewSnipActivity.this,
                                                    error,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        SnipRepository repo = new SnipRepository(getApplication());
        findViewById(R.id.btn_commit)
                .setOnClickListener(
                        v -> {
                            Snip s = new Snip();
                            s.title = safe(title);
                            s.language =
                                    LanguagePickerHelper.vaultLabel(
                                            lang.getText() != null ? lang.getText().toString() : "");
                            s.tags = safe(tags);
                            s.code = safe(code);
                            if (s.tags != null
                                    && (s.tags.contains("#Snap") || s.tags.contains("#OCR"))) {
                                BadgeTracker.recordSnapHunterSave(NewSnipActivity.this);
                            }
                            repo.insert(
                                    s,
                                    () -> {
                                        setResult(Activity.RESULT_OK, new Intent());
                                        XpManager.addXp(this, 15);
                                        finish();
                                    });
                        });
    }

    private static String safe(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
