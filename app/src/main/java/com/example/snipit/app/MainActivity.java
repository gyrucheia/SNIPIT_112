package com.example.snipit.app;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.ui.AIFragment;
import com.example.snipit.app.ui.BeamFragment;
import com.example.snipit.app.ui.DexFragment;
import com.example.snipit.app.ui.SnapFragment;
import com.example.snipit.app.ui.VaultFragment;
import com.example.snipit.app.ui.XPFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_BEAM_SNIP_ID = "beam_snip_id";

    private int pendingBeamSnipId = -1;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        SnipRepository repo = new SnipRepository(getApplication());
        repo.seedIfEmpty();

        if (getIntent() != null && getIntent().hasExtra(EXTRA_BEAM_SNIP_ID)) {
            pendingBeamSnipId = getIntent().getIntExtra(EXTRA_BEAM_SNIP_ID, -1);
        }

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("tab", 0);
            pendingBeamSnipId = savedInstanceState.getInt("pending_beam", -1);
        }

        findViewById(R.id.nav_vault).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.nav_dex).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.nav_beam).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.nav_ai).setOnClickListener(v -> switchTab(3));
        findViewById(R.id.nav_snap).setOnClickListener(v -> switchTab(4));
        findViewById(R.id.nav_xp).setOnClickListener(v -> switchTab(5));

        switchTab(currentTab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", currentTab);
        outState.putInt("pending_beam", pendingBeamSnipId);
    }

    public void openBeamForSnip(int snipId) {
        pendingBeamSnipId = snipId;
        switchTab(2);
    }

    void switchTab(int index) {
        currentTab = index;
        Fragment f;
        switch (index) {
            case 0:
                f = new VaultFragment();
                break;
            case 1:
                f = new DexFragment();
                break;
            case 2:
                BeamFragment bf = new BeamFragment();
                if (pendingBeamSnipId > 0) {
                    Bundle b = new Bundle();
                    b.putInt(BeamFragment.ARG_SNIP_ID, pendingBeamSnipId);
                    bf.setArguments(b);
                    pendingBeamSnipId = -1;
                }
                f = bf;
                break;
            case 3:
                f = new AIFragment();
                break;
            case 4:
                f = new SnapFragment();
                break;
            case 5:
                f = new XPFragment();
                break;
            default:
                f = new VaultFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
        highlightNav(index);
    }

    private void highlightNav(int active) {
        int[] ids =
                new int[] {
                    R.id.nav_vault,
                    R.id.nav_dex,
                    R.id.nav_beam,
                    R.id.nav_ai,
                    R.id.nav_snap,
                    R.id.nav_xp
                };
        int colorOn = getResources().getColor(R.color.accent_green, getTheme());
        int colorOff = getResources().getColor(R.color.text_secondary, getTheme());
        for (int i = 0; i < ids.length; i++) {
            LinearLayout row = findViewById(ids[i]);
            boolean on = i == active;
            row.setAlpha(on ? 1f : 0.7f);
            for (int c = 0; c < row.getChildCount(); c++) {
                if (row.getChildAt(c) instanceof TextView) {
                    TextView tv = (TextView) row.getChildAt(c);
                    tv.setTextColor(on ? colorOn : colorOff);
                }
            }
        }
    }
}
