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

    public static String detectLanguage(String code) {
        if (code == null || code.isEmpty()) return "";
        String c = code.toLowerCase();
        
        if (c.contains("public class") || c.contains("import java.")) return "Java";
        if (c.contains("fun ") && c.contains("val ")) return "Kotlin";
        if (c.contains("def ") && c.endsWith(":")) return "Python";
        if (c.contains("import react") || c.contains("const ") && c.contains("=>")) return "JavaScript";
        if (c.contains("<?php")) return "PHP";
        if (c.contains("package main") && c.contains("func ")) return "Go";
        if (c.contains("using system;") || c.contains("namespace ")) return "C#";
        if (c.contains("#include <") && c.contains("int main()")) return "C++";
        if (c.contains("<html>") || c.contains("<!doctype html>")) return "HTML";
        if (c.contains("SELECT ") && c.contains(" FROM ")) return "SQL";
        
        return "";
    }

    private static void append(StringBuilder sb, String tag) {
        if (sb.length() > 0) sb.append(",");
        sb.append(tag);
    }
}
