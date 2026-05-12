package com.example.snipit.app.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public final class QrUtils {

    private QrUtils() {}

    /**
     * Compresses the input text using GZIP and encodes it in Base64.
     * This is used to bypass the QR code character limit for large snippets.
     */
    public static String compress(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(text.getBytes("UTF-8"));
            gzip.close();
            // Prefix with "z!" so the receiver knows it's compressed
            return "z!" + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return text; // Fallback to raw if compression fails
        }
    }

    public static Bitmap encodeQr(String text, int size) throws WriterException {
        // Automatically compress if text is long or contains complex logic
        String payload = text.length() > 100 ? compress(text) : text;
        
        BitMatrix result =
                new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
