package com.example.snipit.app.data;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Loads extra Dev-Dex rows from {@code assets/dex/*.json} so you can grow content without
 * recompiling Java — edit JSON or ship updates via app update.
 */
public final class DexAssetLoader {

    private DexAssetLoader() {}

    /** @return null if file missing or invalid */
    public static DexDoc[] loadArray(Context context, String assetPath) {
        if (context == null) return null;
        try (InputStream is = context.getAssets().open(assetPath)) {
            String json = readAll(is);
            JSONArray arr = new JSONArray(json);
            DexDoc[] out = new DexDoc[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out[i] =
                        new DexDoc(
                                o.optString("command", ""),
                                o.optString("summary", ""),
                                o.optString("documentation", ""));
            }
            return out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readAll(InputStream is) throws Exception {
        BufferedReader br =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static DexDoc[] concat(DexDoc[] base, DexDoc[] extra) {
        if (extra == null || extra.length == 0) return base;
        if (base == null) return extra;
        DexDoc[] out = new DexDoc[base.length + extra.length];
        System.arraycopy(base, 0, out, 0, base.length);
        System.arraycopy(extra, 0, out, base.length, extra.length);
        return out;
    }
}
