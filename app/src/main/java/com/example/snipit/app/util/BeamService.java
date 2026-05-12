package com.example.snipit.app.util;

import android.os.Handler;
import android.os.Looper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

/**
 * Syncs Beam PIN sessions to Firebase Realtime Database so a web receiver can read the same payload.
 * Replace {@code app/google-services.json} with your Firebase project file and enable Realtime Database.
 */
public class BeamService {

    private static final String PATH = "beam_sessions";
    private static final long TTL_MS = 5 * 60 * 1000L;

    private DatabaseReference getDb() {
        try {
            return FirebaseDatabase.getInstance().getReference(PATH);
        } catch (Exception e) {
            return null;
        }
    }

    public void uploadPin(
            String pin, String snippetCode, String snippetTitle, String language) {
        uploadToSession(pin, snippetCode, snippetTitle, language, true);
    }

    public void uploadToSession(
            String sessionId, String snippetCode, String snippetTitle, String language, boolean autoDelete) {
        if (sessionId == null || sessionId.isEmpty()) return;
        
        DatabaseReference db = getDb();
        if (db == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("code", snippetCode != null ? snippetCode : "");
        data.put("title", snippetTitle != null ? snippetTitle : "");
        data.put("language", language != null ? language : "");
        data.put("expires_at", System.currentTimeMillis() + TTL_MS);

        db.child(sessionId).setValue(data);
        if (autoDelete) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                DatabaseReference db2 = getDb();
                if (db2 != null) db2.child(sessionId).removeValue();
            }, TTL_MS);
        }
    }

    public void deletePin(String pin) {
        if (pin == null || pin.isEmpty()) return;
        DatabaseReference db = getDb();
        if (db != null) {
            db.child(pin).removeValue();
        }
    }
}
