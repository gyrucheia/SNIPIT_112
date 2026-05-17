package com.example.snipit.app;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SnipApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Already initialized or fallback
        }
    }
}
