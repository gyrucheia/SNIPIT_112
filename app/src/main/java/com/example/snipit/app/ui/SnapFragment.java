package com.example.snipit.app.ui;

import android.Manifest;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
    private EditText extracted;
    private MaterialButton captureBtn;
    private MaterialButton saveBtn;
    private MaterialButton fixAiBtn;

    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private final GitHubModelService aiModel = new GitHubModelService();

    private int state = STATE_PREVIEW;
    private boolean aiBusy = false;

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

        previewView = v.findViewById(R.id.previewView);
        status = v.findViewById(R.id.ocr_status);
        progress = v.findViewById(R.id.ocr_progress);
        extracted = v.findViewById(R.id.extracted_code);
        captureBtn = v.findViewById(R.id.btn_scan);
        saveBtn = v.findViewById(R.id.btn_save_extracted);
        fixAiBtn = v.findViewById(R.id.btn_fix_ai);

        cameraExecutor = Executors.newSingleThreadExecutor();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        extracted.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        refreshActionButtons();
                    }
                });

        captureBtn.setOnClickListener(
                view -> {
                    if (state == STATE_PROCESSING) {
                        return;
                    }
                    if (state == STATE_RESULT) {
                        extracted.setText("");
                        state = STATE_PREVIEW;
                        captureBtn.setText(R.string.snap_take_picture);
                        saveBtn.setEnabled(false);
                        status.setText(R.string.snap_status_preview);
                        extracted.setEnabled(true);
                        refreshActionButtons();
                        return;
                    }
                    takePictureAndRecognize();
                });

        fixAiBtn.setOnClickListener(
                view -> {
                    String body =
                            extracted.getText() != null
                                    ? extracted.getText().toString().trim()
                                    : "";
                    if (body.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.fix_ai_need_code, Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    aiBusy = true;
                    refreshActionButtons();
                    captureBtn.setEnabled(false);
                    saveBtn.setEnabled(false);
                    extracted.setEnabled(false);
                    progress.setVisibility(View.VISIBLE);
                    status.setText(R.string.snap_status_ai_working);
                    aiModel.fixOcrText(
                            body,
                            "",
                            new GitHubModelService.Callback() {
                                @Override
                                public void onResult(String result) {
                                    if (!isAdded()) {
                                        return;
                                    }
                                    aiBusy = false;
                                    progress.setVisibility(View.GONE);
                                    captureBtn.setEnabled(true);
                                    extracted.setEnabled(true);
                                    if (result != null) {
                                        extracted.setText(result.trim());
                                    }
                                    status.setText(R.string.snap_status_result);
                                    refreshActionButtons();
                                }

                                @Override
                                public void onError(String error) {
                                    if (!isAdded()) {
                                        return;
                                    }
                                    aiBusy = false;
                                    progress.setVisibility(View.GONE);
                                    captureBtn.setEnabled(true);
                                    extracted.setEnabled(true);
                                    refreshActionButtons();
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                                    status.setText(R.string.snap_status_result);
                                }
                            });
                });

        saveBtn.setOnClickListener(
                view -> {
                    String code = extracted.getText().toString().trim();
                    if (!code.isEmpty()) {
                        Intent i = new Intent(requireContext(), NewSnipActivity.class);
                        i.putExtra("prefill_title", "Snap Capture");
                        i.putExtra("prefill_code", code);
                        i.putExtra("prefill_tags", "#Snap,#OCR");
                        startActivity(i);
                        XpManager.addXp(requireContext(), 25);
                    }
                });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permLauncher.launch(Manifest.permission.CAMERA);
        }

        refreshActionButtons();
    }

    private void refreshActionButtons() {
        if (fixAiBtn == null || extracted == null || saveBtn == null) {
            return;
        }
        String t = extracted.getText() != null ? extracted.getText().toString().trim() : "";
        fixAiBtn.setEnabled(!t.isEmpty() && !aiBusy && state != STATE_PROCESSING);
        if (state == STATE_RESULT) {
            saveBtn.setEnabled(!t.isEmpty());
        }
    }

    private void takePictureAndRecognize() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), R.string.snap_camera_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        state = STATE_PROCESSING;
        captureBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        fixAiBtn.setEnabled(false);
        extracted.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText(R.string.snap_status_ocr);

        imageCapture.takePicture(
                cameraExecutor,
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        runOcrOnImage(imageProxy);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Capture failed", exception);
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity()
                                .runOnUiThread(
                                        () -> {
                                            state = STATE_PREVIEW;
                                            progress.setVisibility(View.GONE);
                                            captureBtn.setEnabled(true);
                                            extracted.setEnabled(true);
                                            status.setText(R.string.snap_status_preview);
                                            Toast.makeText(
                                                            requireContext(),
                                                            R.string.snap_capture_failed,
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        });
                    }
                });
    }

    private void runOcrOnImage(@NonNull ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            if (!isAdded()) {
                return;
            }
            requireActivity()
                    .runOnUiThread(
                            () -> {
                                state = STATE_PREVIEW;
                                progress.setVisibility(View.GONE);
                                captureBtn.setEnabled(true);
                                extracted.setEnabled(true);
                                status.setText(R.string.snap_status_preview);
                                Toast.makeText(requireContext(), R.string.snap_capture_failed, Toast.LENGTH_SHORT)
                                        .show();
                            });
            return;
        }

        InputImage input =
                InputImage.fromMediaImage(
                        mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        recognizer
                .process(input)
                .addOnSuccessListener(
                        text -> {
                            if (!isAdded()) {
                                return;
                            }
                            StringBuilder result = new StringBuilder();
                            for (Text.TextBlock block : text.getTextBlocks()) {
                                result.append(block.getText()).append("\n");
                            }
                            String finalResult = result.toString().trim();
                            extracted.setText(finalResult);
                            state = STATE_RESULT;
                            progress.setVisibility(View.GONE);
                            captureBtn.setEnabled(true);
                            captureBtn.setText(R.string.snap_retake);
                            extracted.setEnabled(true);
                            saveBtn.setEnabled(!finalResult.isEmpty());
                            if (finalResult.isEmpty()) {
                                Toast.makeText(requireContext(), R.string.snap_no_text_found, Toast.LENGTH_SHORT)
                                        .show();
                            }
                            status.setText(R.string.snap_status_result);
                            refreshActionButtons();
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e(TAG, "OCR failed", e);
                            if (!isAdded()) {
                                return;
                            }
                            state = STATE_PREVIEW;
                            progress.setVisibility(View.GONE);
                            captureBtn.setEnabled(true);
                            extracted.setEnabled(true);
                            status.setText(R.string.snap_status_preview);
                            Toast.makeText(requireContext(), R.string.snap_ocr_failed, Toast.LENGTH_SHORT).show();
                        })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(
                () -> {
                    try {
                        cameraProvider = cameraProviderFuture.get();
                        bindCameraUseCases();
                        requireActivity()
                                .runOnUiThread(() -> status.setText(R.string.snap_status_preview));
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, "Camera initialization failed", e);
                    }
                },
                ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture =
                new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
