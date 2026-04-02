package com.example.snipit.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Tracks badge-related actions (incremented on real user actions). */
public final class BadgeTracker {

    private static final String PREFS = "snipit_badges";
    private static final String KEY_BEAM = "beam_upload_count";
    private static final String KEY_DEX = "dex_copy_count";
    private static final String KEY_AI_AUTOSNIP = "ai_autosnip_count";
    private static final String KEY_SNAP_SAVES = "snap_hunter_saves";
    private static final String KEY_LAST_VISIT_MS = "streak_last_visit_ms";
    private static final String KEY_STREAK_COUNT = "streak_consecutive_days";

    public static final int TARGET_SYNTAX_WIZARD = 5;
    public static final int TARGET_BEAM_OPERATOR = 3;
    public static final int TARGET_CLI_SCOUT = 10;
    public static final int TARGET_AI_REFACTOR = 1;
    public static final int TARGET_SNAP_HUNTER = 3;
    public static final int TARGET_STREAK = 7;

    private BadgeTracker() {}

    public static void recordBeamSession(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_BEAM, p.getInt(KEY_BEAM, 0) + 1).apply();
    }

    public static int getBeamCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BEAM, 0);
    }

    public static void recordDexCommandCopy(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_DEX, p.getInt(KEY_DEX, 0) + 1).apply();
    }

    public static int getDexCopyCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_DEX, 0);
    }

    /** User taps Auto-Snip from an AI assistant reply. */
    public static void recordAiAutoSnip(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_AI_AUTOSNIP, p.getInt(KEY_AI_AUTOSNIP, 0) + 1).apply();
    }

    public static int getAiAutoSnipCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_AI_AUTOSNIP, 0);
    }

    /** User saved a snippet that came from Snap / OCR (tags include #Snap). */
    public static void recordSnapHunterSave(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_SNAP_SAVES, p.getInt(KEY_SNAP_SAVES, 0) + 1).apply();
    }

    public static int getSnapHunterCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SNAP_SAVES, 0);
    }

    /**
     * Call once per day when the user opens the app (e.g. MainActivity onResume). Builds a
     * consecutive-local-day streak when they open on back-to-back calendar days.
     */
    public static void recordStreakDay(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long last = p.getLong(KEY_LAST_VISIT_MS, -1);
        int streak = p.getInt(KEY_STREAK_COUNT, 0);
        if (last >= 0 && isSameLocalDay(last, now)) {
            return;
        }
        if (last >= 0 && isPreviousLocalDay(last, now)) {
            streak++;
        } else {
            streak = 1;
        }
        p.edit().putLong(KEY_LAST_VISIT_MS, now).putInt(KEY_STREAK_COUNT, streak).apply();
    }

    public static int getStreakConsecutiveDays(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_STREAK_COUNT, 0);
    }

    private static boolean isSameLocalDay(long a, long b) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(a);
        Calendar cb = Calendar.getInstance();
        cb.setTimeInMillis(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isPreviousLocalDay(long last, long now) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(last);
        next.add(Calendar.DAY_OF_YEAR, 1);
        Calendar n = Calendar.getInstance();
        n.setTimeInMillis(now);
        return next.get(Calendar.YEAR) == n.get(Calendar.YEAR)
                && next.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR);
    }
}
