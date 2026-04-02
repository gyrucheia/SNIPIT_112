package com.example.snipit.app.util;

import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OcrHelper {

    public interface OcrCallback {
        void onSuccess(String extractedText);

        void onFailure(String error);
    }

    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void extractText(Bitmap bitmap, OcrCallback callback) {
        if (bitmap == null) {
            callback.onFailure("No image");
            return;
        }
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(
                        result ->
                                callback.onSuccess(
                                        result.getText() != null ? result.getText() : ""))
                .addOnFailureListener(
                        e ->
                                callback.onFailure(
                                        e.getMessage() != null ? e.getMessage() : "OCR failed"));
    }
}
