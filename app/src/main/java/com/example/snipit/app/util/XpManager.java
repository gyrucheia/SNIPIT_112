package com.example.snipit.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public final class XpManager {

    private static final String TAG = "XpManager";
    private static final String PREFS = "snipit_xp";
    private static final String KEY_APP_XP = "app_xp";
    private static final String KEY_WEB_XP = "web_xp";
    private static final int[] LEVELS = {0, 100, 250, 500, 1000, 2500, 5000};

    private XpManager() {}

    public static int getAppXp(Context context) {
        try {
            return prefs(context).getInt(KEY_APP_XP, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getWebXp(Context context) {
        try {
            return prefs(context).getInt(KEY_WEB_XP, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getTotalXp(Context context) {
        return getAppXp(context) + getWebXp(context);
    }

    public static void addXp(Context context, int delta) {
        if (delta == 0) return;
        try {
            int current = getAppXp(context);
            int newValue = Math.max(0, current + delta);
            prefs(context).edit().putInt(KEY_APP_XP, newValue).apply();
            syncToFirebase(newValue);
        } catch (Exception e) {
            Log.e(TAG, "addXp failed", e);
        }
    }

    public static void syncFromFirebase(Context context) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("stats");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        try {
                            Long fxpApp = snapshot.child("app_xp").getValue(Long.class);
                            Long fxpWeb = snapshot.child("web_xp").getValue(Long.class);
                            
                            SharedPreferences.Editor editor = prefs(context).edit();
                            if (fxpApp != null && fxpApp.intValue() > getAppXp(context)) {
                                editor.putInt(KEY_APP_XP, fxpApp.intValue());
                            }
                            if (fxpWeb != null) {
                                editor.putInt(KEY_WEB_XP, fxpWeb.intValue());
                            }
                            editor.apply();
                        } catch (Exception e) {
                            Log.e(TAG, "Sync parsing failed", e);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Sync cancelled", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "syncFromFirebase failed", e);
        }
    }

    private static void syncToFirebase(int appXp) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("stats");

            Map<String, Object> updates = new HashMap<>();
            updates.put("app_xp", appXp);
            updates.put("lastUpdated", new java.util.Date().toString());
            ref.updateChildren(updates);
        } catch (Exception e) {
            Log.e(TAG, "syncToFirebase failed", e);
        }
    }

    public static int levelFromXp(int xp) {
        for (int i = LEVELS.length - 1; i >= 0; i--) {
            if (xp >= LEVELS[i]) return i + 1;
        }
        return 1;
    }

    public static int progressInLevel(int xp) {
        int lvl = levelFromXp(xp);
        if (lvl >= LEVELS.length) return 100;
        int start = LEVELS[lvl - 1];
        int end = LEVELS[lvl];
        return (int) (((float)(xp - start) / (end - start)) * 100);
    }

    public static int getNextLevelXp(int level) {
        int idx = level - 1;
        if (idx < 0 || idx >= LEVELS.length - 1) return LEVELS[LEVELS.length - 1];
        return LEVELS[idx + 1];
    }

    public static String getLevelName(Context context, int level) {
        try {
            int resId;
            switch (level) {
                case 1: resId = com.example.snipit.app.R.string.rank_1; break;
                case 2: resId = com.example.snipit.app.R.string.rank_2; break;
                case 3: resId = com.example.snipit.app.R.string.rank_3; break;
                case 4: resId = com.example.snipit.app.R.string.rank_4; break;
                case 5: resId = com.example.snipit.app.R.string.rank_5; break;
                case 6: resId = com.example.snipit.app.R.string.rank_6; break;
                case 7: resId = com.example.snipit.app.R.string.rank_7; break;
                default: resId = com.example.snipit.app.R.string.rank_7; break;
            }
            return context.getString(resId);
        } catch (Exception e) {
            return "Dev";
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
