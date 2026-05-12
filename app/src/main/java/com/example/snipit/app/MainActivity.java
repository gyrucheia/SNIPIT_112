package com.example.snipit.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.ui.AIFragment;
import com.example.snipit.app.ui.BeamFragment;
import com.example.snipit.app.ui.DexFragment;
import com.example.snipit.app.ui.ProfileFragment;
import com.example.snipit.app.ui.SnapFragment;
import com.example.snipit.app.ui.VaultFragment;
import com.example.snipit.app.ui.XPFragment;
import com.example.snipit.app.util.XpManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.activity.EdgeToEdge;
import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    private int currentTab = -1;
    private int pendingBeamId = -1;
    private DrawerLayout drawerLayout;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, com.example.snipit.app.auth.LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        
        drawerLayout = findViewById(R.id.main_drawer_layout);
        
        // Setup Bottom Nav Listeners
        findViewById(R.id.nav_vault).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.nav_dex).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.nav_beam).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.nav_ai).setOnClickListener(v -> switchTab(3));
        findViewById(R.id.nav_snap).setOnClickListener(v -> switchTab(4));

        // Drawer Toggle logic
        findViewById(R.id.btn_open_drawer).setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        // Global Add logic
        findViewById(R.id.btn_global_add).setOnClickListener(v -> {
            startActivity(new Intent(this, NewSnipActivity.class));
        });

        // Sidebar Navigation
        NavigationView navView = findViewById(R.id.nav_view);
        if (navView != null && drawerLayout != null) {
            setupSidebar(navView, drawerLayout);
        }

        // Background Sync
        new Thread(() -> {
            new SnipRepository(getApplication()).seedIfEmpty();
            XpManager.syncFromFirebase(this);
            mainHandler.post(() -> {
                if (navView != null) refreshSidebarStats(navView);
            });
        }).start();

        if (savedInstanceState == null) {
            switchTab(0);
        }
    }

    public void switchTab(int index) {
        if (currentTab == index && pendingBeamId <= 0) return;
        
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded()) ft.hide(f);
        }

        String tag = "frag_" + index;
        Fragment target = fm.findFragmentByTag(tag);
        
        if (index == 2 && pendingBeamId > 0 && target != null) {
            ft.remove(target);
            target = null;
        }

        if (target == null) {
            target = createFragment(index);
            if (index == 2 && pendingBeamId > 0) {
                Bundle b = new Bundle();
                b.putInt(BeamFragment.ARG_SNIP_ID, pendingBeamId);
                target.setArguments(b);
                pendingBeamId = -1;
            }
            ft.add(R.id.fragment_container, target, tag);
        } else {
            ft.show(target);
        }
        
        ft.commitAllowingStateLoss();
        currentTab = index;
        
        mainHandler.post(() -> updateNavUi(index));
    }

    private void updateNavUi(int selected) {
        int[] ids = {R.id.nav_vault, R.id.nav_dex, R.id.nav_beam, R.id.nav_ai, R.id.nav_snap};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v == null) continue;
            v.setAlpha(i == selected ? 1.0f : 0.5f);
        }
    }

    private Fragment createFragment(int index) {
        switch (index) {
            case 1: return new DexFragment();
            case 2: return new BeamFragment();
            case 3: return new AIFragment();
            case 4: return new SnapFragment();
            case 5: return new XPFragment();
            case 6: return new ProfileFragment();
            case 0:
            default: return new VaultFragment();
        }
    }

    public void openBeamForSnip(int id) {
        this.pendingBeamId = id;
        switchTab(2);
    }

    public void openMainDrawer() {
        if (drawerLayout != null) {
            refreshSidebarStats(findViewById(R.id.nav_view));
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void setupSidebar(NavigationView navView, DrawerLayout drawer) {
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_drawer_profile) {
                switchTab(6);
            } else if (id == R.id.nav_drawer_sync) {
                switchTab(2);
            } else if (id == R.id.nav_drawer_xp) {
                switchTab(5);
            } else if (id == R.id.nav_drawer_xp_info) {
                showAboutSnipitDialog();
            } else if (id == R.id.nav_drawer_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, com.example.snipit.app.auth.LoginActivity.class));
                finish();
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
        refreshSidebarStats(navView);
    }

    private void refreshSidebarStats(NavigationView navView) {
        if (navView == null) return;
        View header = navView.getHeaderView(0);
        if (header == null) return;

        TextView name = header.findViewById(R.id.header_user_name);
        TextView email = header.findViewById(R.id.header_user_email);
        TextView xpLabel = header.findViewById(R.id.header_xp_label);
        android.widget.ImageView avatar = header.findViewById(R.id.header_avatar);
        android.widget.ProgressBar progress = header.findViewById(R.id.header_xp_progress);
        View toggle = header.findViewById(R.id.btn_toggle_theme);

        if (toggle != null) {
            toggle.setOnClickListener(v -> {
                int mode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK);
                if (mode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                }
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            email.setText(user.getEmail());
            String dName = user.getDisplayName();
            if (dName != null && !dName.isEmpty()) {
                name.setText(dName.toUpperCase());
            } else {
                name.setText(getString(R.string.sidebar_dev_root));
            }
            
            // Load Avatar if photo exists
            if (user.getPhotoUrl() != null && avatar != null) {
                Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(avatar);
            }
        }

        int xp = XpManager.getTotalXp(this);
        int lvl = XpManager.levelFromXp(xp);
        String rankName = XpManager.getLevelName(this, lvl);

        xpLabel.setText(getString(R.string.sidebar_xp_line, lvl, rankName, xp));
        progress.setProgress(XpManager.progressInLevel(xp));
    }

    private void showAboutSnipitDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.about_snipit_title))
            .setMessage(getString(R.string.about_snipit_content))
            .setNeutralButton(getString(R.string.xp_rank_methodology_title), (dialog, which) -> showXpMethodologyDialog())
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void showXpMethodologyDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.xp_rank_methodology_title))
            .setMessage(getString(R.string.xp_rank_methodology_content))
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
}
