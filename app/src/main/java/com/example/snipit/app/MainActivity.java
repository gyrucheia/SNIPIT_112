package com.example.snipit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.snipit.app.auth.LoginActivity;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.ui.AIFragment;
import com.example.snipit.app.ui.BeamFragment;
import com.example.snipit.app.ui.DexFragment;
import com.example.snipit.app.ui.SnapFragment;
import com.example.snipit.app.ui.VaultFragment;
import com.example.snipit.app.ui.XPFragment;
import com.example.snipit.app.util.BadgeTracker;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_BEAM_SNIP_ID = "beam_snip_id";

    private static final String[] FRAGMENT_TAGS =
            new String[] {"frag_vault", "frag_dex", "frag_beam", "frag_ai", "frag_snap", "frag_xp"};

    private int pendingBeamSnipId = -1;
    private int currentTab = 0;

    public void openMainDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        if (drawer != null) drawer.openDrawer(androidx.core.view.GravityCompat.START);
    }

    // may dinagdag me
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);

        android.content.SharedPreferences pref = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isTutorialDone = pref.getBoolean("tutorial_done", false);

        if (!isTutorialDone) {
            startActivity(new android.content.Intent(this, TutorialActivity.class));
            finish();
            return;
        }

        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, com.example.snipit.app.auth.LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_drawer_layout), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        SnipRepository repo = new SnipRepository(getApplication());
        repo.seedIfEmpty();

        if (getIntent() != null && getIntent().hasExtra(EXTRA_BEAM_SNIP_ID)) {
            pendingBeamSnipId = getIntent().getIntExtra(EXTRA_BEAM_SNIP_ID, -1);
        }

        findViewById(R.id.nav_vault).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.nav_dex).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.nav_beam).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.nav_ai).setOnClickListener(v -> switchTab(3));
        findViewById(R.id.nav_snap).setOnClickListener(v -> switchTab(4));
        findViewById(R.id.nav_xp).setOnClickListener(v -> switchTab(5));

        if (savedInstanceState == null) {
            switchTab(0);
        } else {
            currentTab = savedInstanceState.getInt("tab", 0);
            pendingBeamSnipId = savedInstanceState.getInt("pending_beam", -1);
            highlightNav(currentTab);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            for (int i = 0; i < FRAGMENT_TAGS.length; i++) {
                Fragment f = fm.findFragmentByTag(FRAGMENT_TAGS[i]);
                if (f != null) {
                    if (i == currentTab) {
                        ft.show(f);
                    } else {
                        ft.hide(f);
                    }
                }
            }
            ft.commit();
            fm.executePendingTransactions();
            if (fm.findFragmentByTag(FRAGMENT_TAGS[currentTab]) == null) {
                switchTab(currentTab);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BadgeTracker.recordStreakDay(this);
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

    public void switchTab(int index) {
        currentTab = index;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        for (int i = 0; i < FRAGMENT_TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(FRAGMENT_TAGS[i]);
            if (f != null) {
                ft.hide(f);
            }
        }

        Fragment target = fm.findFragmentByTag(FRAGMENT_TAGS[index]);
        if (index == 2 && pendingBeamSnipId > 0 && target != null) {
            ft.remove(target);
            target = null;
        }

        if (target == null) {
            target = createFragment(index);
            if (index == 2 && pendingBeamSnipId > 0) {
                Bundle b = new Bundle();
                b.putInt(BeamFragment.ARG_SNIP_ID, pendingBeamSnipId);
                target.setArguments(b);
                pendingBeamSnipId = -1;
            }
            ft.add(R.id.fragment_container, target, FRAGMENT_TAGS[index]);
        } else {
            ft.show(target);
        }
        ft.commit();
        highlightNav(index);
    }

    private static Fragment createFragment(int index) {
        switch (index) {
            case 0:
                return new VaultFragment();
            case 1:
                return new DexFragment();
            case 2:
                return new BeamFragment();
            case 3:
                return new AIFragment();
            case 4:
                return new SnapFragment();
            case 5:
                return new XPFragment();
            default:
                return new VaultFragment();
        }
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
