package com.example.snipit.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OcrScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView statusLabel;
    private ProgressBar progress;
    private FloatingActionButton btnCapture;
    private MaterialButton btnImport;
    
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private TextRecognizer recognizer;
    private String lastScannedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ocr_scan);

        previewView = findViewById(R.id.scan_preview);
        statusLabel = findViewById(R.id.scan_status);
        progress = findViewById(R.id.scan_progress);
        btnCapture = findViewById(R.id.fab_capture_scan);
        btnImport = findViewById(R.id.btn_import_scan);

        cameraExecutor = Executors.newSingleThreadExecutor();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }

        btnCapture.setOnClickListener(v -> takePhoto());
        btnImport.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("scanned_text", lastScannedText);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("OcrScan", "Camera binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        progress.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(false);
        statusLabel.setText("READING BUFFER...");

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnCapture.setEnabled(true);
                    statusLabel.setText("CAPTURE FAILED");
                });
            }
        });
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    lastScannedText = visionText.getText();
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        btnCapture.setEnabled(true);
                        if (lastScannedText.isEmpty()) {
                            statusLabel.setText("NO CODE DETECTED");
                        } else {
                            statusLabel.setText("SNIP DETECTED (" + lastScannedText.length() + " chars)");
                            btnImport.setVisibility(View.VISIBLE);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        btnCapture.setEnabled(true);
                        statusLabel.setText("OCR ERROR");
                    });
                })
                .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera access is required for scanning.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
