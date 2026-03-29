package com.example.snipit.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button; // <--- Add this line
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.NewSnipActivity;
import com.example.snipit.app.R;
import com.example.snipit.app.util.XpManager;

public class SnapFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private ProgressBar progress;
    private EditText extracted;
    private Button saveBtn;
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

        v.findViewById(R.id.btn_scan)
                .setOnClickListener(
                        x -> {
                            progress.setVisibility(View.VISIBLE);
                            progress.setProgress(0);
                            status.setText("Detecting edges…");
                            runStep(1);
                        });

        saveBtn.setOnClickListener(
                x -> {
                    Intent i = new Intent(requireContext(), NewSnipActivity.class);
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TITLE, "Board capture");
                    i.putExtra(
                            NewSnipActivity.EXTRA_PREFILL_CODE,
                            extracted.getText() != null ? extracted.getText().toString() : "");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_LANG, "Text");
                    i.putExtra(NewSnipActivity.EXTRA_PREFILL_TAGS, "#OCR,#Snap");
                    startActivity(i);
                });
    }

    private void runStep(int step) {
        int delay = 500;
        switch (step) {
            case 1:
                progress.setProgress(25);
                status.setText("Reading characters…");
                handler.postDelayed(() -> runStep(2), delay);
                break;
            case 2:
                progress.setProgress(55);
                status.setText("Fixing indentation…");
                handler.postDelayed(() -> runStep(3), delay);
                break;
            case 3:
                progress.setProgress(90);
                status.setText("Formatting snippet…");
                handler.postDelayed(() -> runStep(4), delay);
                break;
            case 4:
                progress.setProgress(100);
                status.setText("Done — review & save");
                extracted.setText(
                        "public class HelloSnip {\n"
                                + "  public static void main(String[] args) {\n"
                                + "    System.out.println(\"Huy, Snip it Quick!\");\n"
                                + "  }\n"
                                + "}");
                saveBtn.setEnabled(true);
                XpManager.addXp(requireContext(), 20);
                break;
            default:
                break;
        }
    }
}
