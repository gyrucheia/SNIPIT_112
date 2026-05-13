package com.example.snipit.app.util;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility for compressing large code snippets before QR generation.
 * This solves the "Beam IT" data constraint for long files.
 */
public class GzipUtil {

    public static String compress(String data) throws IOException {
        if (data == null || data.isEmpty()) return "";
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(data.getBytes(StandardCharsets.UTF_8));
        gos.close();
        
        byte[] compressed = bos.toByteArray();
        bos.close();
        
        // Add a prefix so the receiver knows it's compressed
        return "snipit_gz:" + Base64.encodeToString(compressed, Base64.NO_WRAP);
    }

    public static String decompress(String compressedBase64) throws IOException {
        if (compressedBase64 == null || !compressedBase64.startsWith("snipit_gz:")) return compressedBase64;
        
        String cleanBase64 = compressedBase64.substring("snipit_gz:".length());
        byte[] compressed = Base64.decode(cleanBase64, Base64.NO_WRAP);
        
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        
        String result = bos.toString(StandardCharsets.UTF_8.name());
        
        gis.close();
        bis.close();
        bos.close();
        
        return result;
    }
}
