package com.example.snipit.app.util;

/** Heuristic OCR quality score (no ML model — fast, explainable for demos). */
public final class OcrConfidence {

    public enum Level {
        HIGH,
        MEDIUM,
        LOW
    }

    private OcrConfidence() {}

    /** Returns 0–100 and level from raw OCR text. */
    public static Result analyze(String text) {
        if (text == null) text = "";
        String t = text.trim();
        if (t.isEmpty()) {
            return new Result(0, Level.LOW);
        }
        int score = 40;
        int lines = t.split("\n").length;
        if (lines >= 3) score += 15;
        if (lines >= 8) score += 10;
        long alnum = t.chars().filter(Character::isLetterOrDigit).count();
        double ratio = alnum / (double) Math.max(1, t.length());
        if (ratio > 0.55) score += 20;
        if (ratio > 0.7) score += 10;
        if (t.contains("{") && t.contains("}")) score += 5;
        if (t.contains("(") && t.contains(")")) score += 5;
        if (t.contains(";") || t.contains("def ") || t.contains("public ")) score += 5;
        int openB = count(t, '{');
        int closeB = count(t, '}');
        if (openB > 0 && openB == closeB) score += 5;
        score = Math.min(100, score);
        Level lvl = score >= 75 ? Level.HIGH : (score >= 45 ? Level.MEDIUM : Level.LOW);
        return new Result(score, lvl);
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    public static final class Result {
        public final int percent;
        public final Level level;

        Result(int percent, Level level) {
            this.percent = percent;
            this.level = level;
        }
    }
}
