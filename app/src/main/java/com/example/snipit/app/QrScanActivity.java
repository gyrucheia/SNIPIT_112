package com.example.snipit.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.List;

public class QrScanActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            decodeQr(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        TextView back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());

        Button pick = findViewById(R.id.btn_pick);
        pick.setOnClickListener(v -> pickImage.launch("image/*"));
    }

    private void decodeQr(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = loadBitmapForBarcode(uri);
            if (bitmap == null) {
                Toast.makeText(this, R.string.qr_no_barcode, Toast.LENGTH_SHORT).show();
                return;
            }
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            final Bitmap toRecycle = bitmap;
            BarcodeScanning.getClient()
                    .process(image)
                    .addOnSuccessListener(
                            barcodes -> {
                                if (!toRecycle.isRecycled()) {
                                    toRecycle.recycle();
                                }
                                String text = firstQrPayload(barcodes);
                                if (text == null || text.isEmpty()) {
                                    Toast.makeText(this, R.string.qr_no_barcode, Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }
                                Intent i = new Intent(this, NewSnipActivity.class);
                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "From QR");
                                i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, text);
                                startActivity(i);
                                finish();
                            })
                    .addOnFailureListener(
                            e -> {
                                if (!toRecycle.isRecycled()) {
                                    toRecycle.recycle();
                                }
                                Toast.makeText(
                                                this,
                                                e.getMessage() != null
                                                        ? e.getMessage()
                                                        : "Decode failed",
                                                Toast.LENGTH_LONG)
                                        .show();
                            });
        } catch (Exception e) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            Toast.makeText(
                            this,
                            e.getMessage() != null ? e.getMessage() : "Could not read image",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    /** Older vision-common builds omit {@code InputImage.fromUri}; decode and use {@link InputImage#fromBitmap}. */
    private Bitmap loadBitmapForBarcode(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            BitmapFactory.decodeStream(is, null, bounds);
        }
        int w = bounds.outWidth;
        int h = bounds.outHeight;
        if (w <= 0 || h <= 0) return null;

        int maxDim = 2048;
        int sample = 1;
        while (w / sample > maxDim || h / sample > maxDim) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            return BitmapFactory.decodeStream(is, null, opts);
        }
    }

    private static String firstQrPayload(List<Barcode> barcodes) {
        for (Barcode b : barcodes) {
            if (b.getRawValue() != null && !b.getRawValue().isEmpty()) {
                return b.getRawValue();
            }
        }
        return null;
    }
}
