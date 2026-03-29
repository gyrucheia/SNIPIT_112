package com.example.snipit.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class XpManager {

    private static final String PREFS = "snipit_xp";
    private static final String KEY_XP = "total_xp";
    private static final int XP_PER_LEVEL = 100;

    private XpManager() {}

    public static int getXp(Context context) {
        return prefs(context).getInt(KEY_XP, 0);
    }

    public static void addXp(Context context, int delta) {
        if (delta == 0) return;
        SharedPreferences p = prefs(context);
        p.edit().putInt(KEY_XP, Math.max(0, p.getInt(KEY_XP, 0) + delta)).apply();
    }

    public static int levelFromXp(int xp) {
        return 1 + xp / XP_PER_LEVEL;
    }

    public static int progressInLevel(int xp) {
        return xp % XP_PER_LEVEL;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
