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
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.util.XpManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class XPFragment extends Fragment {

    private SnipRepository snipRepo;

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
        snipRepo = new SnipRepository(requireActivity().getApplication());

        v.findViewById(R.id.badge_snip)
                .setOnClickListener(
                        x ->
                                snipRepo.getSnippetCount(
                                        c -> {
                                            if (!isAdded()) return;
                                            showBadgeDetail(
                                                    getString(R.string.badge_syntax_wizard_title),
                                                    getString(R.string.badge_syntax_wizard_desc),
                                                    c,
                                                    BadgeTracker.TARGET_SYNTAX_WIZARD);
                                        }));
        v.findViewById(R.id.badge_beam)
                .setOnClickListener(
                        x -> {
                            int beam = BadgeTracker.getBeamCount(requireContext());
                            showBadgeDetail(
                                    getString(R.string.badge_beam_operator_title),
                                    getString(R.string.badge_beam_operator_desc),
                                    beam,
                                    BadgeTracker.TARGET_BEAM_OPERATOR);
                        });
        v.findViewById(R.id.badge_dex)
                .setOnClickListener(
                        x -> {
                            int dex = BadgeTracker.getDexCopyCount(requireContext());
                            showBadgeDetail(
                                    getString(R.string.badge_cli_scout_title),
                                    getString(R.string.badge_cli_scout_desc),
                                    dex,
                                    BadgeTracker.TARGET_CLI_SCOUT);
                        });
        v.findViewById(R.id.badge_ai_refactor)
                .setOnClickListener(
                        x -> {
                            int n = BadgeTracker.getAiAutoSnipCount(requireContext());
                            showBadgeDetail(
                                    getString(R.string.badge_ai_refactor_title),
                                    getString(R.string.badge_ai_refactor_desc),
                                    n,
                                    BadgeTracker.TARGET_AI_REFACTOR);
                        });
        v.findViewById(R.id.badge_snap_hunter)
                .setOnClickListener(
                        x -> {
                            int n = BadgeTracker.getSnapHunterCount(requireContext());
                            showBadgeDetail(
                                    getString(R.string.badge_snap_hunter_title),
                                    getString(R.string.badge_snap_hunter_desc),
                                    n,
                                    BadgeTracker.TARGET_SNAP_HUNTER);
                        });
        v.findViewById(R.id.badge_streak)
                .setOnClickListener(
                        x -> {
                            int n = BadgeTracker.getStreakConsecutiveDays(requireContext());
                            showBadgeDetail(
                                    getString(R.string.badge_streak_title),
                                    getString(R.string.badge_streak_desc),
                                    n,
                                    BadgeTracker.TARGET_STREAK);
                        });

        refresh(v);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) refresh(v);
    }

    private void showBadgeDetail(String title, String description, int current, int target) {
        View root =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_badge_detail, null, false);
        TextView msg = root.findViewById(R.id.badge_dialog_message);
        ProgressBar bar = root.findViewById(R.id.badge_dialog_progress);
        TextView status = root.findViewById(R.id.badge_dialog_status);
        msg.setText(description);
        boolean earned = current >= target;
        bar.setMax(Math.max(1, target));
        bar.setProgress(Math.min(current, target));
        if (earned) {
            status.setText(R.string.badge_earned_label);
        } else {
            status.setText(
                    getString(R.string.badge_progress_label, current, target)
                            + "\n"
                            + getString(R.string.badge_next_steps, target - current));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void refresh(View v) {
        int xp = XpManager.getXp(requireContext());
        int level = XpManager.levelFromXp(xp);
        int prog = XpManager.progressInLevel(xp);

        TextView lvl = v.findViewById(R.id.xp_level);
        ProgressBar bar = v.findViewById(R.id.xp_bar);
        TextView detail = v.findViewById(R.id.xp_detail);

        lvl.setText(
                getString(
                        R.string.xp_level_line,
                        level,
                        getString(R.string.xp_level_code_wizard)));
        bar.setProgress(prog);
        int next = 100 - prog;
        detail.setText(
                xp
                        + " XP · "
                        + prog
                        + "/100 in this level · "
                        + next
                        + " XP to next level");

        LinearLayout bSnip = v.findViewById(R.id.badge_snip);
        LinearLayout bBeam = v.findViewById(R.id.badge_beam);
        LinearLayout bDex = v.findViewById(R.id.badge_dex);
        LinearLayout bAi = v.findViewById(R.id.badge_ai_refactor);
        LinearLayout bSnap = v.findViewById(R.id.badge_snap_hunter);
        LinearLayout bStreak = v.findViewById(R.id.badge_streak);

        snipRepo.getSnippetCount(
                count -> {
                    if (!isAdded()) return;
                    int beam = BadgeTracker.getBeamCount(requireContext());
                    int dex = BadgeTracker.getDexCopyCount(requireContext());
                    int ai = BadgeTracker.getAiAutoSnipCount(requireContext());
                    int snap = BadgeTracker.getSnapHunterCount(requireContext());
                    int streak = BadgeTracker.getStreakConsecutiveDays(requireContext());
                    styleBadge(bSnip, count >= BadgeTracker.TARGET_SYNTAX_WIZARD);
                    styleBadge(bBeam, beam >= BadgeTracker.TARGET_BEAM_OPERATOR);
                    styleBadge(bDex, dex >= BadgeTracker.TARGET_CLI_SCOUT);
                    styleBadge(bAi, ai >= BadgeTracker.TARGET_AI_REFACTOR);
                    styleBadge(bSnap, snap >= BadgeTracker.TARGET_SNAP_HUNTER);
                    styleBadge(bStreak, streak >= BadgeTracker.TARGET_STREAK);
                });
    }

    private void styleBadge(LinearLayout row, boolean earned) {
        row.setAlpha(earned ? 1f : 0.4f);
    }
}
