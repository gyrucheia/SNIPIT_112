package com.example.snipit.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.R;
import com.example.snipit.app.util.XpManager;

public class XPFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        refresh(v);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) refresh(v);
    }

    private void refresh(View v) {
        int xp = XpManager.getXp(requireContext());
        int level = XpManager.levelFromXp(xp);
        int prog = XpManager.progressInLevel(xp);

        TextView lvl = v.findViewById(R.id.xp_level);
        ProgressBar bar = v.findViewById(R.id.xp_bar);
        TextView detail = v.findViewById(R.id.xp_detail);

        lvl.setText("Lv " + level);
        bar.setProgress(prog);
        detail.setText(xp + " XP · " + prog + "/100 to next level");

        LinearLayout bSnip = v.findViewById(R.id.badge_snip);
        LinearLayout bBeam = v.findViewById(R.id.badge_beam);
        LinearLayout bDex = v.findViewById(R.id.badge_dex);
        styleBadge(bSnip, xp >= 30);
        styleBadge(bBeam, xp >= 15);
        styleBadge(bDex, xp >= 10);
    }

    private void styleBadge(LinearLayout row, boolean earned) {
        float a = earned ? 1f : 0.35f;
        row.setAlpha(a);
    }
}
