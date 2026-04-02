package com.example.snipit.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.services.GitHubModelService;
import com.example.snipit.app.util.AutoTagger;
import com.example.snipit.app.util.LanguagePickerHelper;
import com.example.snipit.app.util.QrUtils;
import com.example.snipit.app.util.XpManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.WriterException;

public class EditSnipActivity extends AppCompatActivity {

    public static final String EXTRA_SNIP_ID = "snip_id";

    private int snipId = -1;
    private final GitHubModelService aiModel = new GitHubModelService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_snip);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        snipId = getIntent().getIntExtra(EXTRA_SNIP_ID, -1);
        if (snipId <= 0) {
            Toast.makeText(this, R.string.snip_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText title = findViewById(R.id.input_title);
        AutoCompleteTextView lang = findViewById(R.id.input_language);
        EditText tags = findViewById(R.id.input_tags);
        EditText code = findViewById(R.id.input_code);
        ProgressBar fixProgress = findViewById(R.id.progress_fix_ai);
        View btnFixAi = findViewById(R.id.btn_fix_with_ai);

        LanguagePickerHelper.bind(lang, this);

        SnipRepository repo = new SnipRepository(getApplication());
        repo.getSnipById(
                snipId,
                s -> {
                    if (s == null) {
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(this, R.string.snip_not_found, Toast.LENGTH_SHORT)
                                            .show();
                                    finish();
                                });
                        return;
                    }
                    runOnUiThread(
                            () -> {
                                title.setText(s.title != null ? s.title : "");
                                LanguagePickerHelper.setDisplayFromVaultLabel(
                                        lang, this, s.language != null ? s.language : "");
                                tags.setText(s.tags != null ? s.tags : "");
                                code.setText(s.code != null ? s.code : "");
                            });
                });

        code.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence seq, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence seq, int start, int before, int count) {
                        if (tags.getText() == null || tags.getText().toString().trim().length() > 0) {
                            return;
                        }
                        String langLabel =
                                LanguagePickerHelper.vaultLabel(
                                        lang.getText() != null ? lang.getText().toString() : "");
                        String suggested = AutoTagger.suggest(seq.toString(), langLabel);
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
                                    Toast.makeText(EditSnipActivity.this, error, Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        findViewById(R.id.btn_show_qr)
                .setOnClickListener(
                        v -> {
                            String payload = safe(code);
                            if (payload.isEmpty()) {
                                Toast.makeText(this, R.string.qr_need_code, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                Bitmap bmp = QrUtils.encodeQr(payload, 512);
                                ImageView iv = new ImageView(this);
                                iv.setImageBitmap(bmp);
                                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                                iv.setPadding(pad, pad, pad, pad);
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle(R.string.show_qr)
                                        .setMessage(R.string.qr_share_peer_hint)
                                        .setView(iv)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            } catch (WriterException e) {
                                Toast.makeText(this, R.string.qr_too_long, Toast.LENGTH_SHORT).show();
                            }
                        });

        findViewById(R.id.btn_delete)
                .setOnClickListener(
                        v ->
                                new MaterialAlertDialogBuilder(this)
                                        .setTitle(R.string.delete_snip)
                                        .setMessage(R.string.delete_snip_confirm)
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(
                                                android.R.string.ok,
                                                (d, w) ->
                                                        repo.getSnipById(
                                                                snipId,
                                                                sn -> {
                                                                    if (sn != null) {
                                                                        repo.delete(
                                                                                sn,
                                                                                () -> {
                                                                                    Toast.makeText(
                                                                                                    EditSnipActivity
                                                                                                            .this,
                                                                                                    R.string
                                                                                                            .deleted,
                                                                                                    Toast
                                                                                                            .LENGTH_SHORT)
                                                                                            .show();
                                                                                    finish();
                                                                                });
                                                                    } else {
                                                                        Toast.makeText(
                                                                                        EditSnipActivity.this,
                                                                                        R.string.snip_not_found,
                                                                                        Toast.LENGTH_SHORT)
                                                                                .show();
                                                                    }
                                                                }))
                                        .show());

        findViewById(R.id.btn_commit)
                .setOnClickListener(
                        v ->
                                repo.getSnipById(
                                        snipId,
                                                existing -> {
                                            if (existing == null) {
                                                Toast.makeText(
                                                                EditSnipActivity.this,
                                                                R.string.snip_not_found,
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                                return;
                                            }
                                            existing.title = safe(title);
                                            existing.language =
                                                    LanguagePickerHelper.vaultLabel(
                                                            lang.getText() != null
                                                                    ? lang.getText().toString()
                                                                    : "");
                                            existing.tags = safe(tags);
                                            existing.code = safe(code);
                                            repo.update(
                                                    existing,
                                                    () -> {
                                                        XpManager.addXp(EditSnipActivity.this, 2);
                                                        Toast.makeText(
                                                                        EditSnipActivity.this,
                                                                        R.string.saved,
                                                                        Toast.LENGTH_SHORT)
                                                                .show();
                                                        finish();
                                                    });
                                        }));
    }

    private static String safe(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
