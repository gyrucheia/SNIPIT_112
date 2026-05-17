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
import androidx.activity.result.PickVisualMediaRequest;
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
import com.example.snipit.app.SnapReviewActivity;
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
    private MaterialButton btnGallery;
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

    private final ActivityResultLauncher<PickVisualMediaRequest> pickGalleryMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (uris != null && !uris.isEmpty() && getContext() != null) {
                    try {
                        progress.setVisibility(View.VISIBLE);
                        status.setText("IMPORTING " + uris.size() + " IMAGES...");
                        
                        accumulatedLines.clear();
                        final int total = uris.size();
                        final java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
                        
                        android.net.Uri firstUri = uris.get(0);
                        java.io.InputStream is = getContext().getContentResolver().openInputStream(firstUri);
                        if (is != null) {
                            Bitmap bmp = BitmapFactory.decodeStream(is);
                            is.close();
                            if (bmp != null) {
                                capturePreview.setImageBitmap(bmp);
                                capturePreview.setVisibility(View.VISIBLE);
                                scrimOverlay.setVisibility(View.GONE);
                            }
                        }
                        
                        for (android.net.Uri uri : uris) {
                            java.io.InputStream stream = getContext().getContentResolver().openInputStream(uri);
                            if (stream != null) {
                                Bitmap bmp = BitmapFactory.decodeStream(stream);
                                stream.close();
                                if (bmp != null) {
                                    InputImage inputImage = InputImage.fromBitmap(bmp, 0);
                                    recognizer.process(inputImage)
                                            .addOnSuccessListener(visionText -> {
                                                String text = visionText.getText().trim();
                                                String[] lines = text.split("\n");
                                                for (String line : lines) {
                                                    String trimmed = line.trim();
                                                    if (trimmed.length() > 3 && !isNoiseLine(trimmed)) {
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
                                                
                                                int progressCount = completed.incrementAndGet();
                                                if (progressCount == total) {
                                                    if (isAdded()) {
                                                        requireActivity().runOnUiThread(() -> {
                                                            StringBuilder sb = new StringBuilder();
                                                            for (String line : accumulatedLines) {
                                                                    sb.append(line).append("\n");
                                                            }
                                                            capturedText = sb.toString().trim();
                                                            extractedPreview.setText(capturedText);
                                                            progress.setVisibility(View.GONE);
                                                            status.setText("BATCH OCR COMPLETE");
                                                            
                                                            state = STATE_RESULT;
                                                            captureBtn.setText("RESCAN");
                                                            btnContinue.setVisibility(View.VISIBLE);
                                                            
                                                            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("snipit_prefs", Context.MODE_PRIVATE);
                                                            int newCount = prefs.getInt("gallery_upload_count", 0) + 1;
                                                            prefs.edit().putInt("gallery_upload_count", newCount).apply();
                                                            Toast.makeText(getContext(), "Import " + newCount + "/5 successful", Toast.LENGTH_SHORT).show();
                                                            
                                                            View container = getView() != null ? getView().findViewById(R.id.btn_container_snap) : null;
                                                            if (container != null) container.setVisibility(View.GONE);
                                                        });
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                int progressCount = completed.incrementAndGet();
                                                if (progressCount == total && isAdded()) {
                                                    requireActivity().runOnUiThread(() -> progress.setVisibility(View.GONE));
                                                }
                                            });
                                } else {
                                    completed.incrementAndGet();
                                }
                            } else {
                                completed.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        progress.setVisibility(View.GONE);
                        status.setText("IMPORT FAILED");
                    }
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
            btnGallery = v.findViewById(R.id.btn_gallery_upload);
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
                        if (state == STATE_PREVIEW) {
                            startCaptureSession();
                        } else if (state == STATE_PROCESSING) {
                            stopCaptureSession();
                        } else if (state == STATE_RESULT) {
                            resetScanner();
                        }
                    });

            btnContinue.setOnClickListener(v1 -> {
                if (capturedText != null && !capturedText.isEmpty()) {
                    Intent i = new Intent(requireContext(), NewSnipActivity.class);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "Board capture");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_CODE, capturedText);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Plain Text");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#OCR,#Snap");
                    startActivity(i);
                    
                    // Automatically switch to Vault tab when user returns from editor
                    if (getActivity() instanceof com.example.snipit.app.MainActivity) {
                        ((com.example.snipit.app.MainActivity) getActivity()).switchTab(0);
                    }
                }
            });

            if (btnGallery != null) {
                btnGallery.setOnClickListener(view -> {
                    int currentUploads = requireContext().getSharedPreferences("snipit_prefs", Context.MODE_PRIVATE)
                            .getInt("gallery_upload_count", 0);
                    if (currentUploads >= 5) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Developer Import Limit")
                                .setMessage("You have reached the maximum limit of 5 local gallery imports. Use high-precision Live Scan to continue or upgrade to SnipIT Pro!")
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return;
                    }

                    pickGalleryMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                });
            }

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
        captureBtn.setText("START CAPTURE");
        captureBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.terminal_green)));
        captureBtn.setVisibility(View.VISIBLE);
        captureBtn.setEnabled(true);
        captureBtn.setAlpha(1.0f);
        btnContinue.setVisibility(View.GONE);
        extractedPreview.setText("");
        scannerHint.setVisibility(View.VISIBLE);
        status.setText("SYSTEM READY");

        if (getView() != null) {
            View container = getView().findViewById(R.id.btn_container_snap);
            if (container != null) container.setVisibility(View.VISIBLE);
        }
        
        // Ensure UI elements are visible for scanning
        scrimOverlay.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.VISIBLE);
        capturePreview.setVisibility(View.GONE);
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

    private void takeSnapshot() {
        if (imageCapture == null) return;

        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                state = STATE_PROCESSING;
                progress.setVisibility(View.VISIBLE);
                captureBtn.setEnabled(false);
                status.setText("CAPTURING IMAGE...");
                scannerHint.setVisibility(View.GONE);
            });
        }

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap fullBitmap = imageProxyToBitmap(image);
                image.close();

                if (fullBitmap != null) {
                    Bitmap croppedBitmap = cropBitmapToScanner(fullBitmap);
                    if (croppedBitmap != null) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                capturePreview.setImageBitmap(croppedBitmap);
                                capturePreview.setVisibility(View.VISIBLE);
                                scrimOverlay.setVisibility(View.GONE);
                            });
                        }

                        InputImage inputImage = InputImage.fromBitmap(croppedBitmap, 0);
                        recognizer.process(inputImage)
                                .addOnSuccessListener(visionText -> {
                                    capturedText = visionText.getText().trim();
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            extractedPreview.setText(capturedText);
                                            progress.setVisibility(View.GONE);
                                            captureBtn.setEnabled(true);
                                            captureBtn.setText("RESCAN");
                                            captureBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.terminal_green)));
                                            state = STATE_RESULT;
                                            status.setText("OCR COMPLETE");

                                            if (!capturedText.isEmpty()) {
                                                btnContinue.setVisibility(View.VISIBLE);
                                                btnContinue.setAlpha(1.0f);
                                            } else {
                                                status.setText("NO TEXT DETECTED");
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() -> {
                                            progress.setVisibility(View.GONE);
                                            captureBtn.setEnabled(true);
                                            status.setText("OCR ERROR");
                                        });
                                    }
                                });
                    } else {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                progress.setVisibility(View.GONE);
                                captureBtn.setEnabled(true);
                                status.setText("CROP FAILED");
                            });
                        }
                    }
                } else {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            captureBtn.setEnabled(true);
                            status.setText("CAPTURE DECODE FAILED");
                        });
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        captureBtn.setEnabled(true);
                        status.setText("CAPTURE FAILED");
                    });
                }
            }
        });
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
        if (previewView == null || previewView.getHeight() == 0 || scannerBox == null) {
            float relativeTop = (float) rect.top / imgH;
            float relativeBottom = (float) rect.bottom / imgH;
            return relativeTop > 0.15f && relativeBottom < 0.85f;
        }

        float boxTop = scannerBox.getY() / previewView.getHeight();
        float boxBottom = (scannerBox.getY() + scannerBox.getHeight()) / previewView.getHeight();

        float relativeTop = (float) rect.top / imgH;
        float relativeBottom = (float) rect.bottom / imgH;

        float tolerance = 0.1f;
        float scanTop = Math.max(0.0f, boxTop - tolerance);
        float scanBottom = Math.min(1.0f, boxBottom + tolerance);

        return relativeTop >= scanTop && relativeBottom <= scanBottom;
    }

    private void handleLiveDetection(String text) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (state != STATE_PROCESSING) return;

                String[] lines = text.split("\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.length() > 3 && !isNoiseLine(trimmed)) {
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

    private boolean isNoiseLine(String line) {
        String lower = line.toLowerCase().trim();
        if (lower.isEmpty()) return true;
        if (lower.replaceAll("[\\s;\\{\\}\\(\\),.\\\"'\\-_\\*\\+]", "").isEmpty()) return true;

        if (lower.contains("vivo") || lower.contains("oppo") || lower.contains("realme") || lower.contains("samsung")) return true;
        if (lower.contains("shot on") || lower.contains("camera") || lower.contains("triple") || lower.contains("dual")) return true;
        if (lower.contains("battery") || lower.matches(".*\\b\\d{1,2}%\\b.*")) return true;
        if (lower.equals("lte") || lower.equals("volte") || lower.equals("3g") || lower.equals("4g") || lower.equals("5g")) return true;
        if (lower.matches(".*\\d{1,2}:\\d{2}.*")) return true;

        return false;
    }

    private boolean isSimilar(String s1, String s2) {
        if (s1 == null || s2 == null) return true;
        if (s1.trim().equalsIgnoreCase(s2.trim())) return true;

        String clean1 = s1.replaceAll("[\\s;\\{\\}\\(\\),.\\\"']", "").toLowerCase();
        String clean2 = s2.replaceAll("[\\s;\\{\\}\\(\\),.\\\"']", "").toLowerCase();

        if (clean1.isEmpty() || clean2.isEmpty()) return true;
        if (clean1.equals(clean2)) return true;
        if (clean1.contains(clean2) || clean2.contains(clean1)) return true;

        int distance = getLevenshteinDistance(clean1, clean2);
        int maxLength = Math.max(clean1.length(), clean2.length());
        double similarity = 1.0 - ((double) distance / maxLength);

        return similarity > 0.75;
    }

    private int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) return 0;
        int n = s.length();
        int m = t.length();
        if (n == 0) return m;
        if (m == 0) return n;

        int[] p = new int[n + 1];
        int[] d = new int[n + 1];

        for (int i = 0; i <= n; i++) p[i] = i;

        for (int j = 1; j <= m; j++) {
            char t_j = t.charAt(j - 1);
            d[0] = j;
            for (int i = 1; i <= n; i++) {
                int cost = s.charAt(i - 1) == t_j ? 0 : 1;
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }
            int[] placeholder = p;
            p = d;
            d = placeholder;
        }
        return p[n];
    }

    private void checkFrameClarity(@NonNull ImageProxy imageProxy) {
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
                    boolean hasTextInBox = false;
                    for (com.google.mlkit.vision.text.Text.TextBlock block : text.getTextBlocks()) {
                        android.graphics.Rect rect = block.getBoundingBox();
                        if (rect != null && isInsideScanner(rect, imageProxy.getWidth(), imageProxy.getHeight())) {
                            hasTextInBox = true;
                            break;
                        }
                    }
                    final boolean inFocus = hasTextInBox;
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (inFocus) {
                                captureBtn.setEnabled(true);
                                captureBtn.setAlpha(1.0f);
                                scannerBox.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.terminal_green));
                                status.setText("TERMINAL IN FOCUS (READY)");
                            } else {
                                captureBtn.setEnabled(false);
                                captureBtn.setAlpha(0.5f);
                                scannerBox.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.error_red));
                                status.setText("ALIGN & FOCUS TERMINAL...");
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
