package com.example.snipit.app.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import androidx.core.content.ContextCompat;
import com.example.snipit.app.R;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance regex-based syntax highlighter for SnipIT.
 * Supports commands, strings, comments, and language-specific keywords.
 */
public class CodeHighlighter {

    private static final Pattern KEYWORDS_JAVA = Pattern.compile("\\b(public|private|protected|class|interface|extends|implements|static|final|void|int|long|float|double|boolean|if|else|for|while|return|new|import|package|try|catch|finally|throw|throws|abstract|volatile|transient|native|synchronized)\\b");
    private static final Pattern KEYWORDS_SQL = Pattern.compile("\\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|JOIN|GROUP BY|ORDER BY|CREATE|TABLE|ALTER|DROP|INTO|VALUES|AND|OR|NOT|NULL|PRIMARY KEY|FOREIGN KEY|INNER|LEFT|RIGHT|FULL|OUTER)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEYWORDS_CLI = Pattern.compile("\\b(git|sudo|apt-get|ls|cd|mkdir|rm|chmod|chown|grep|cat|echo|systemctl|docker|adb|ps|kill|man|history)\\b");
    private static final Pattern STRINGS = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern COMMENTS = Pattern.compile("//.*|/\\*.*?\\*/|#.*", Pattern.DOTALL);

    public static Spannable highlight(Context context, String code, String lang) {
        if (code == null) return new SpannableString("");
        SpannableString spannable = new SpannableString(code);

        int colorKeyword = ContextCompat.getColor(context, R.color.terminal_green);
        int colorString = ContextCompat.getColor(context, R.color.accent_cyan);
        int colorComment = ContextCompat.getColor(context, R.color.text_muted);

        // 1. Highlight Comments (Grey)
        applyPattern(spannable, COMMENTS, colorComment);

        // 2. Highlight Strings (Cyan)
        applyPattern(spannable, STRINGS, colorString);

        // 3. Highlight Keywords (Green)
        if ("Java".equalsIgnoreCase(lang) || "Kotlin".equalsIgnoreCase(lang)) {
            applyPattern(spannable, KEYWORDS_JAVA, colorKeyword);
        } else if ("SQL".equalsIgnoreCase(lang)) {
            applyPattern(spannable, KEYWORDS_SQL, colorKeyword);
        } else if ("Git".equalsIgnoreCase(lang) || "Linux".equalsIgnoreCase(lang) || "CLI".equalsIgnoreCase(lang)) {
            applyPattern(spannable, KEYWORDS_CLI, colorKeyword);
        } else {
            // Default: Try to find common CLI commands if unknown
            applyPattern(spannable, KEYWORDS_CLI, colorKeyword);
        }

        return spannable;
    }

    private static void applyPattern(Spannable spannable, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
