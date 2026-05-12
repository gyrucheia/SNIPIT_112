package com.example.snipit.app.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.services.GitHubModelService;
import com.example.snipit.app.util.XpManager;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnapFragment extends Fragment {

    private static final String TAG = "SnapFragment";

    /** Camera preview only — user taps Take picture when ready. */
    private static final int STATE_PREVIEW = 0;
    /** Photo taken, OCR running. */
    private static final int STATE_PROCESSING = 1;
    /** OCR finished (text may be empty). */
    private static final int STATE_RESULT = 2;

    private PreviewView previewView;
    private TextView status;
    private ProgressBar progress;
    private View scannerBox;
    private TextView extractedPreview;
    private MaterialButton captureBtn;
    private MaterialButton btnContinue;
    private View scannerHint;
    private android.widget.ImageView capturePreview;
    private View scrimOverlay;
    private float dX, dY; 
    private String capturedText = "";

    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private final GitHubModelService aiModel = new GitHubModelService();

    private int state = STATE_PREVIEW;
    private boolean isLiveScanning = false; // Start with scanning OFF
    private long lastAnalysisTime = 0;
    private androidx.camera.core.Camera camera;
    private boolean isReadyToCapture = false;
    private java.util.Set<String> accumulatedLines = new java.util.LinkedHashSet<>();
    private static final int STABILITY_THRESHOLD = 3; 

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_snap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        try {
            previewView = v.findViewById(R.id.previewView);
            status = v.findViewById(R.id.ocr_status);
            progress = v.findViewById(R.id.ocr_progress);
            scannerBox = v.findViewById(R.id.scanner_box);
            extractedPreview = v.findViewById(R.id.extracted_preview);
            captureBtn = v.findViewById(R.id.btn_scan);
            btnContinue = v.findViewById(R.id.btn_continue_to_editor);
            scannerHint = v.findViewById(R.id.scanner_hint_container);
            capturePreview = v.findViewById(R.id.capture_preview_image);
            scrimOverlay = v.findViewById(R.id.scrim_overlay);
            
            setupResizableFrame();
        } catch (Exception e) {
            return;
        }

        try {
            cameraExecutor = Executors.newSingleThreadExecutor();
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        } catch (Exception e) {
            recognizer = null;
            return;
        }

        try {
            captureBtn.setOnClickListener(
                    view -> {
                        if (!isLiveScanning && state == STATE_PREVIEW) {
                            // Ready to start session
                            startCaptureSession();
                        } else if (isLiveScanning && state == STATE_PROCESSING) {
                            // Currently capturing, so STOP it
                            stopCaptureSession();
                        } else if (state == STATE_RESULT) {
                            resetScanner();
                        }
                    });

            btnContinue.setOnClickListener(v1 -> {
                if (capturedText != null && !capturedText.isEmpty()) {
                    Intent i = new Intent(requireContext(), NewSnipActivity.class);
                    i.putExtra("prefill_title", "Snap Capture " + new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
                    i.putExtra("prefill_code", capturedText);
                    i.putExtra("prefill_tags", "#Snap,#OCR");
                    startActivity(i);
                    
                    // Automatically switch to Vault tab when user returns from editor
                    if (getActivity() instanceof com.example.snipit.app.MainActivity) {
                        ((com.example.snipit.app.MainActivity) getActivity()).switchTab(0);
                    }
                }
            });

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                permLauncher.launch(Manifest.permission.CAMERA);
            }
        } catch (Exception e) {}
    }

    private void setupResizableFrame() {
        scannerBox.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth, initialHeight;
            private float initialX, initialY;

            @Override
            public boolean onTouch(View view, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        initialWidth = view.getWidth();
                        initialHeight = view.getHeight();
                        initialX = event.getRawX();
                        initialY = event.getRawY();
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        // Simple logic: if touch is in bottom-right corner, resize. Else drag.
                        float localX = event.getX();
                        float localY = event.getY();
                        boolean isResize = localX > view.getWidth() * 0.8f && localY > view.getHeight() * 0.8f;

                        if (isResize) {
                            int newWidth = (int) (initialWidth + (event.getRawX() - initialX));
                            int newHeight = (int) (initialHeight + (event.getRawY() - initialY));
                            if (newWidth > 100 && newHeight > 50) {
                                ViewGroup.LayoutParams lp = view.getLayoutParams();
                                lp.width = newWidth;
                                lp.height = newHeight;
                                view.setLayoutParams(lp);
                            }
                        } else {
                            view.animate()
                                    .x(event.getRawX() + dX)
                                    .y(event.getRawY() + dY)
                                    .setDuration(0)
                                    .start();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    private void resetScanner() {
        isLiveScanning = false;
        capturedText = "";
        state = STATE_PREVIEW;
        captureBtn.setText("INITIATE CAPTURE");
        captureBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.terminal_green)));
        captureBtn.setVisibility(View.VISIBLE);
        captureBtn.setEnabled(true);
        btnContinue.setVisibility(View.GONE);
        extractedPreview.setText("");
        scannerHint.setVisibility(View.VISIBLE);
        status.setText("SYSTEM READY");
        captureBtn.setEnabled(false); // Guarded until focus/clarity
        captureBtn.setAlpha(0.5f);
        
        // Ensure UI elements are visible for scanning
        scrimOverlay.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.VISIBLE);
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        @SuppressWarnings("UnsafeOptInUsageError")
        android.media.Image.Plane[] planes = image.getImage().getPlanes();
        java.nio.ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        if (bitmap == null) return null;

        // Rotate if needed
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap cropBitmapToScanner(Bitmap bitmap) {
        try {
            // Calculate scale between preview and bitmap
            float scaleX = (float) bitmap.getWidth() / previewView.getWidth();
            float scaleY = (float) bitmap.getHeight() / previewView.getHeight();

            int left = (int) (scannerBox.getX() * scaleX);
            int top = (int) (scannerBox.getY() * scaleX); // Using scaleX for both assuming aspect ratio is preserved
            int width = (int) (scannerBox.getWidth() * scaleX);
            int height = (int) (scannerBox.getHeight() * scaleY);

            // Bounds check
            left = Math.max(0, left);
            top = Math.max(0, top);
            if (left + width > bitmap.getWidth()) width = bitmap.getWidth() - left;
            if (top + height > bitmap.getHeight()) height = bitmap.getHeight() - top;

            return Bitmap.createBitmap(bitmap, left, top, width, height);
        } catch (Exception e) {
            return null;
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                requireActivity().runOnUiThread(() -> status.setText("TERMINAL READY"));
            } catch (Exception e) {
                Log.e("Snap", "Camera Init Fail", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startCaptureSession() {
        state = STATE_PROCESSING;
        isLiveScanning = true;
        accumulatedLines.clear();
        capturedText = "";
        captureBtn.setText("STOP CAPTURE");
        captureBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_red)));
        status.setText("ACCUMULATING CODE...");
        progress.setVisibility(View.VISIBLE);
        scannerHint.setVisibility(View.GONE);
    }

    private void stopCaptureSession() {
        isLiveScanning = false;
        state = STATE_RESULT;
        
        // Finalize accumulated text
        StringBuilder sb = new StringBuilder();
        for (String line : accumulatedLines) {
            sb.append(line).append("\n");
        }
        capturedText = sb.toString().trim();
        extractedPreview.setText(capturedText);

        captureBtn.setText("RESCAN");
        captureBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.terminal_green)));
        btnContinue.setVisibility(View.VISIBLE);
        btnContinue.animate().alpha(1.0f).setDuration(300).start();
        progress.setVisibility(View.GONE);
        status.setText("SESSION COMPLETE");
        
        // Haptic feedback for "Lock"
        try {
            android.os.Vibrator v = (android.os.Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(30);
                }
            }
        } catch (Exception ignored) {}
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            // Background check for "Ready" state even if not recording
            if (!isLiveScanning && state == STATE_PREVIEW) {
                checkFrameClarity(image);
                return;
            }

            if (!isLiveScanning) {
                image.close();
                return;
            }

            // Throttle to 2fps for stability and battery
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnalysisTime < 500) {
                image.close();
                return;
            }
            lastAnalysisTime = currentTime;

            runLiveOcr(image);
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
        } catch (Exception e) {
            Log.e("Snap", "Binding Fail", e);
        }
    }

    private void runLiveOcr(@NonNull ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null || recognizer == null) {
            imageProxy.close();
            return;
        }

        InputImage input = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        
        recognizer.process(input)
                .addOnSuccessListener(text -> {
                    if (isLiveScanning) {
                        // Filter text blocks by their position relative to the scanner box
                        StringBuilder sb = new StringBuilder();
                        for (com.google.mlkit.vision.text.Text.TextBlock block : text.getTextBlocks()) {
                            android.graphics.Rect rect = block.getBoundingBox();
                            if (rect != null && isInsideScanner(rect, imageProxy.getWidth(), imageProxy.getHeight())) {
                                sb.append(block.getText()).append("\n");
                            }
                        }
                        String filtered = sb.toString().trim();
                        if (!filtered.isEmpty()) {
                            handleLiveDetection(filtered);
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private boolean isInsideScanner(android.graphics.Rect rect, int imgW, int imgH) {
        // Map the image coordinates to the PreviewView coordinates
        // This is a simplified check: if the block is too high or too low, it's noise
        // Since the scanner is centered, we ignore the top and bottom 20% by default
        // or we can use the scannerBox's actual bounds if we want to be very precise.
        
        float relativeTop = (float) rect.top / imgH;
        float relativeBottom = (float) rect.bottom / imgH;
        
        // Ignore top 15% (Status bar, VIVO, etc) and bottom 15%
        return relativeTop > 0.15f && relativeBottom < 0.85f;
    }

    private void handleLiveDetection(String text) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                // LOCK: Only process if we are actively in the PROCESSING state
                if (state != STATE_PROCESSING) return;

                String[] lines = text.split("\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.length() > 3) {
                        // Jitter Guard: Check if a similar line already exists
                        boolean isNew = true;
                        for (String existing : accumulatedLines) {
                            if (isSimilar(trimmed, existing)) {
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew) accumulatedLines.add(trimmed);
                    }
                }
                
                StringBuilder sb = new StringBuilder();
                for (String line : accumulatedLines) {
                    sb.append(line).append("\n");
                }
                extractedPreview.setText(sb.toString().trim());
                status.setText(getString(R.string.badge_progress_label, accumulatedLines.size(), 0).replace("toward this badge", "LINES CAPTURED"));
            });
        }
    }

    private boolean isSimilar(String s1, String s2) {
        if (s1.equals(s2)) return true;
        // Simple length and content check for jitter
        if (Math.abs(s1.length() - s2.length()) < 2) {
            if (s1.contains(s2) || s2.contains(s1)) return true;
        }
        return false;
    }

    private void checkFrameClarity(@NonNull ImageProxy imageProxy) {
        // LOCK: Only run in PREVIEW state
        if (state != STATE_PREVIEW) {
            imageProxy.close();
            return;
        }
        
        @SuppressWarnings("UnsafeOptInUsageError")
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null || recognizer == null) {
            imageProxy.close();
            return;
        }

        InputImage input = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        recognizer.process(input)
                .addOnSuccessListener(text -> {
                    boolean hasText = !text.getText().trim().isEmpty();
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (hasText) {
                                captureBtn.setEnabled(true);
                                captureBtn.setAlpha(1.0f);
                                status.setText("TERMINAL READY (CLEAR)");
                            } else {
                                captureBtn.setEnabled(false);
                                captureBtn.setAlpha(0.5f);
                                status.setText("FOCUSING terminal...");
                            }
                        });
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && state == STATE_RESULT) {
            // Automatically reset if returning from a successful capture
            resetScanner();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }
}
