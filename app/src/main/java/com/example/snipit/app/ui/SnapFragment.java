package com.example.snipit.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.SnapReviewActivity;
import com.example.snipit.app.util.OcrConfidence;
import com.example.snipit.app.util.OcrHelper;
import com.example.snipit.app.util.XpManager;

public class SnapFragment extends Fragment {

    private TextView status;
    private ProgressBar progress;
    private EditText extracted;
    private Button saveBtn;
    private final OcrHelper ocr = new OcrHelper();

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() == null) return;
                        Bundle ex = result.getData().getExtras();
                        if (ex == null) return;
                        Bitmap photo = (Bitmap) ex.get("data");
                        if (photo == null) return;
                        runOcrOnBitmap(photo);
                    });

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (Boolean.TRUE.equals(granted)) openCamera();
                        else
                            Toast.makeText(
                                            requireContext(),
                                            "Camera permission needed for Snap-to-Snip",
                                            Toast.LENGTH_SHORT)
                                    .show();
                    });

    private final ActivityResultLauncher<Intent> newSnipLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                            resetSnapAfterVaultSave();
                        }
                    });

    private final ActivityResultLauncher<Intent> snapReviewLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                            resetSnapAfterVaultSave();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_snap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        status = v.findViewById(R.id.ocr_status);
        progress = v.findViewById(R.id.ocr_progress);
        extracted = v.findViewById(R.id.extracted_code);
        saveBtn = v.findViewById(R.id.btn_save_extracted);

        v.findViewById(R.id.btn_scan).setOnClickListener(x -> checkCameraAndOpen());

        saveBtn.setOnClickListener(
                x -> {
                    Intent i = new Intent(requireContext(), NewSnipActivity.class);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "Board capture");
                    i.putExtra(
                            NewSnipActivity.EXTRA_PREFILL_CODE,
                            extracted.getText() != null ? extracted.getText().toString() : "");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Text");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#OCR,#Snap");
                    newSnipLauncher.launch(i);
                });
    }

    private void resetSnapAfterVaultSave() {
        if (extracted != null) extracted.setText("");
        if (status != null) status.setText(getString(R.string.snap_ready_again));
        if (progress != null) {
            progress.setVisibility(View.GONE);
            progress.setProgress(0);
        }
        saveBtn.setEnabled(false);
        saveBtn.setText(R.string.saved_to_vault_done);
    }

    private void checkCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void runOcrOnBitmap(Bitmap bitmap) {
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(10);
        status.setText("Running on-device OCR…");

        ocr.extractText(
                bitmap,
                new OcrHelper.OcrCallback() {
                    @Override
                    public void onSuccess(String text) {
                        progress.setProgress(100);
                        boolean empty = text == null || text.trim().isEmpty();
                        status.setText(
                                empty ? "No text detected — try closer / better light" : "Done — review & save");
                        extracted.setText(empty ? "" : text);
                        saveBtn.setText(R.string.save_to_vault);
                        saveBtn.setEnabled(!empty);
                        if (!empty) {
                            XpManager.addXp(requireContext(), 20);
                            OcrConfidence.Result conf = OcrConfidence.analyze(text);
                            Intent review =
                                    new Intent(requireContext(), SnapReviewActivity.class);
                            review.putExtra(SnapReviewActivity.EXTRA_OCR_TEXT, text);
                            review.putExtra(SnapReviewActivity.EXTRA_CONFIDENCE_PERCENT, conf.percent);
                            review.putExtra(
                                    SnapReviewActivity.EXTRA_CONFIDENCE_LEVEL, conf.level.name());
                            snapReviewLauncher.launch(review);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        progress.setVisibility(View.GONE);
                        status.setText("Scan failed: " + error);
                    }
                });
    }
}
