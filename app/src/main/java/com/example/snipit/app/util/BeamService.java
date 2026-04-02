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

    private final DatabaseReference db =
            FirebaseDatabase.getInstance().getReference(PATH);
    private final Handler main = new Handler(Looper.getMainLooper());

    public void uploadPin(
            String pin, String snippetCode, String snippetTitle, String language) {
        if (pin == null || pin.length() != 6) return;
        Map<String, Object> data = new HashMap<>();
        data.put("code", snippetCode != null ? snippetCode : "");
        data.put("title", snippetTitle != null ? snippetTitle : "");
        data.put("language", language != null ? language : "");
        data.put("expires_at", System.currentTimeMillis() + TTL_MS);

        db.child(pin).setValue(data);
        main.postDelayed(() -> db.child(pin).removeValue(), TTL_MS);
    }

    public void deletePin(String pin) {
        if (pin == null || pin.length() != 6) return;
        db.child(pin).removeValue();
    }
}
