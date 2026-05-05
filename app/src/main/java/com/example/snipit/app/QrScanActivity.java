package com.example.snipit.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";
    private ExecutorService cameraExecutor;
    private PreviewView viewFinder;
    private boolean isScanning = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required for live scanning", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            decodeQrFromUri(uri);
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

        viewFinder = findViewById(R.id.viewFinder);
        cameraExecutor = Executors.newSingleThreadExecutor();

        TextView back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());

        Button pick = findViewById(R.id.btn_pick);
        pick.setOnClickListener(v -> pickImage.launch("image/*"));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (isScanning) {
                        processImageProxy(image);
                    } else {
                        image.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        BarcodeScanning.getClient().process(image)
                .addOnSuccessListener(barcodes -> {
                    String text = firstQrPayload(barcodes);
                    if (text != null && !text.isEmpty()) {
                        isScanning = false;
                        handleResult(text);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleResult(String text) {
        if (getIntent().getBooleanExtra("result_only", false)) {
            Intent result = new Intent();
            result.putExtra("scanned_text", text);
            setResult(RESULT_OK, result);
            finish();
            return;
        }

        Intent i = new Intent(this, NewSnipActivity.class);
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "From QR");
        i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, text);
        startActivity(i);
        finish();
    }

    private void decodeQrFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return;

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            BarcodeScanning.getClient().process(image)
                    .addOnSuccessListener(barcodes -> {
                        String text = firstQrPayload(barcodes);
                        if (text != null && !text.isEmpty()) {
                            handleResult(text);
                        } else {
                            Toast.makeText(this, R.string.qr_no_barcode, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
