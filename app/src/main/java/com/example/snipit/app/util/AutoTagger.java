package com.example.snipit.app.util;

public final class AutoTagger {

    private AutoTagger() {}

    /** Rule-based tag suggestions (offline). Comma-separated, no spaces between tags. */
    public static String suggest(String code, String language) {
        if (code == null) code = "";
        if (language == null) language = "";
        StringBuilder tags = new StringBuilder();

        String c = code.toLowerCase();
        String l = language.toLowerCase();

        if (l.contains("java") || c.contains("public class") || c.contains("import java")) {
            append(tags, "#Java");
        }
        if (l.contains("kotlin") || c.contains("fun ") || c.contains("val ") || c.contains("var ")) {
            append(tags, "#Kotlin");
        }
        if (l.contains("python") || c.contains("def ") || c.contains("import os")) {
            append(tags, "#Python");
        }
        if (l.contains("sql") || c.contains("select ") || c.contains("insert into")) {
            append(tags, "#SQL");
        }
        if (c.contains("firebase") || c.contains("firestore") || c.contains("realtime")) {
            append(tags, "#Firebase");
        }
        if (c.contains("recyclerview")
                || c.contains("adapter")
                || c.contains("viewholder")) {
            append(tags, "#Android");
        }
        if (c.contains("git ") || c.contains("adb ") || c.contains("sudo ")) {
            append(tags, "#CLI");
        }
        if (c.contains("@app.route") || c.contains("express") || c.contains(".get(\"/")) {
            append(tags, "#Backend");
        }

        return tags.toString();
    }

    private static void append(StringBuilder sb, String tag) {
        if (sb.length() > 0) sb.append(",");
        sb.append(tag);
    }
}
