package com.example.snipit.app.util;

import android.content.Context;
import android.util.Log;
import com.example.snipit.app.models.HandbookEntry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HandbookManager {
    private static final String TAG = "HandbookManager";
    private static HandbookManager instance;
    private final List<HandbookEntry> entries = new ArrayList<>();

    private HandbookManager(Context context) {
        loadHandbook(context);
    }

    public static synchronized HandbookManager getInstance(Context context) {
        if (instance == null) {
            instance = new HandbookManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadHandbook(Context context) {
        loadJsonFile(context, "handbook.json");
    }

    private void loadJsonFile(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new HandbookEntry(
                        obj.optString("category", "GENERAL"),
                        obj.getString("command"),
                        obj.optString("summary", ""),
                        obj.optString("documentation", "")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading handbook file: " + fileName, e);
        }
    }

    public List<HandbookEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    public List<HandbookEntry> getByCategory(String category) {
        List<HandbookEntry> result = new ArrayList<>();
        for (HandbookEntry entry : entries) {
            if (entry.getCategory().equalsIgnoreCase(category)) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<HandbookEntry> search(String query) {
        List<HandbookEntry> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (HandbookEntry entry : entries) {
            if (entry.getCommand().toLowerCase().contains(lowerQuery) ||
                entry.getSummary().toLowerCase().contains(lowerQuery) ||
                entry.getCategory().toLowerCase().contains(lowerQuery)) {
                result.add(entry);
            }
        }
        return result;
    }
}
