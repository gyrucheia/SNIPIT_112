package com.example.snipit.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.util.XpManager;

public class NewSnipActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_TITLE = "prefill_title";
    public static final String EXTRA_PREFILL_CODE = "prefill_code";
    public static final String EXTRA_PREFILL_LANG = "prefill_lang";
    public static final String EXTRA_PREFILL_TAGS = "prefill_tags";

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
        EditText lang = findViewById(R.id.input_language);
        EditText tags = findViewById(R.id.input_tags);
        EditText code = findViewById(R.id.input_code);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_PREFILL_TITLE)) {
                title.setText(getIntent().getStringExtra(EXTRA_PREFILL_TITLE));
            }
            if (getIntent().hasExtra(EXTRA_PREFILL_CODE)) {
                code.setText(getIntent().getStringExtra(EXTRA_PREFILL_CODE));
            }
            if (getIntent().hasExtra(EXTRA_PREFILL_LANG)) {
                lang.setText(getIntent().getStringExtra(EXTRA_PREFILL_LANG));
            }
            if (getIntent().hasExtra(EXTRA_PREFILL_TAGS)) {
                tags.setText(getIntent().getStringExtra(EXTRA_PREFILL_TAGS));
            }
        }

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        SnipRepository repo = new SnipRepository(getApplication());
        findViewById(R.id.btn_commit)
                .setOnClickListener(
                        v -> {
                            Snip s = new Snip();
                            s.title = safe(title);
                            s.language = safe(lang);
                            s.tags = safe(tags);
                            s.code = safe(code);
                            repo.insert(s);
                            XpManager.addXp(this, 15);
                            finish();
                        });
    }

    private static String safe(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
