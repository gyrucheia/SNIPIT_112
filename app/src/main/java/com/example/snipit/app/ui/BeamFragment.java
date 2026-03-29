package com.example.snipit.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.snipit.app.R;
import com.example.snipit.app.database.SnipRepository;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.util.BadgeTracker;
import com.example.snipit.app.util.BeamService;
import com.example.snipit.app.util.QrUtils;
import com.example.snipit.app.util.XpManager;
import com.google.zxing.WriterException;
import java.security.SecureRandom;

public class BeamFragment extends Fragment {

    public static final String ARG_SNIP_ID = "snip_id";

    private static final int TIMER_SEC = 300;

    private SnipRepository repo;
    private Snip current;
    private final BeamService beamService = new BeamService();
    private final SecureRandom secureRandom = new SecureRandom();
    private String activeFirebasePin;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private int secondsLeft;
    private String pin = "------";

    private TextView[] pinDigits;
    private TextView pinTimer;
    private ProgressBar barL;
    private ProgressBar barR;
    private View pinMode;
    private View qrMode;
    private TextView tabPin;
    private TextView tabQr;
    private ImageView qrImage;
    private TextView title;
    private TextView lang;
    private TextView code;
    private View beamActiveSection;
    private TextView beamEmptyState;
    private View btnNewPin;
    private View btnCopyPin;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_beam, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        repo = new SnipRepository(requireActivity().getApplication());

        title = v.findViewById(R.id.beam_snip_title);
        lang = v.findViewById(R.id.beam_snip_lang);
        code = v.findViewById(R.id.beam_snip_code);
        beamActiveSection = v.findViewById(R.id.beam_active_section);
        beamEmptyState = v.findViewById(R.id.beam_empty_state);
        btnNewPin = v.findViewById(R.id.btn_new_pin);
        btnCopyPin = v.findViewById(R.id.btn_copy_pin);
        pinMode = v.findViewById(R.id.pin_mode_container);
        qrMode = v.findViewById(R.id.qr_mode_container);
        tabPin = v.findViewById(R.id.tab_pin);
        tabQr = v.findViewById(R.id.tab_qr);
        pinTimer = v.findViewById(R.id.pin_timer);
        barL = v.findViewById(R.id.timer_bar_left);
        barR = v.findViewById(R.id.timer_bar_right);
        qrImage = v.findViewById(R.id.qr_image);

        pinDigits =
                new TextView[] {
                    v.findViewById(R.id.pin_d0),
                    v.findViewById(R.id.pin_d1),
                    v.findViewById(R.id.pin_d2),
                    v.findViewById(R.id.pin_d3),
                    v.findViewById(R.id.pin_d4),
                    v.findViewById(R.id.pin_d5)
                };

        barL.setMax(TIMER_SEC);
        barR.setMax(TIMER_SEC);

        tabPin.setOnClickListener(x -> showMode(true));
        tabQr.setOnClickListener(x -> showMode(false));

        v.findViewById(R.id.btn_new_pin).setOnClickListener(x -> regeneratePinAndTimer());
        v.findViewById(R.id.btn_copy_pin).setOnClickListener(x -> copyPin());

        int snipId = -1;
        Bundle args = getArguments();
        if (args != null) snipId = args.getInt(ARG_SNIP_ID, -1);

        if (snipId > 0) {
            int sid = snipId;
            repo.getSnipById(
                    sid,
                    s -> {
                        if (s == null) {
                            bindEmptyBeamState();
                            title.setText("Snippet not found");
                            return;
                        }
                        current = s;
                        bindSnip(s);
                        regeneratePinAndTimer();
                    });
        } else {
            bindEmptyBeamState();
        }
    }

    /** No Vault snippet was chosen — do not auto-load “latest” so the tab matches user intent. */
    private void bindEmptyBeamState() {
        current = null;
        activeFirebasePin = null;
        if (ticker != null) handler.removeCallbacks(ticker);
        title.setText(R.string.beam_no_snippet_title);
        lang.setText("—");
        code.setText("");
        if (beamEmptyState != null) {
            beamEmptyState.setVisibility(View.VISIBLE);
        }
        if (beamActiveSection != null) {
            beamActiveSection.setVisibility(View.GONE);
        }
        if (btnNewPin != null) btnNewPin.setEnabled(false);
        if (btnCopyPin != null) btnCopyPin.setEnabled(false);
    }

    private void bindSnip(Snip s) {
        if (s == null) return;
        if (beamEmptyState != null) {
            beamEmptyState.setVisibility(View.GONE);
        }
        if (beamActiveSection != null) {
            beamActiveSection.setVisibility(View.VISIBLE);
        }
        if (btnNewPin != null) btnNewPin.setEnabled(true);
        if (btnCopyPin != null) btnCopyPin.setEnabled(true);
        title.setText(s.title != null ? s.title : "(untitled)");
        lang.setText(s.language != null ? s.language : "—");
        code.setText(s.code != null ? s.code : "");
        buildQr();
    }

    private void showMode(boolean pin) {
        tabPin.setTextColor(
                requireContext()
                        .getResources()
                        .getColor(pin ? R.color.accent_green : R.color.text_muted, null));
        tabQr.setTextColor(
                requireContext()
                        .getResources()
                        .getColor(!pin ? R.color.accent_green : R.color.text_muted, null));
        tabPin.setBackgroundResource(pin ? R.drawable.bg_card : 0);
        tabQr.setBackgroundResource(!pin ? R.drawable.bg_card : 0);
        pinMode.setVisibility(pin ? View.VISIBLE : View.GONE);
        qrMode.setVisibility(pin ? View.GONE : View.VISIBLE);
        if (!pin) buildQr();
    }

    private void regeneratePinAndTimer() {
        if (activeFirebasePin != null) {
            beamService.deletePin(activeFirebasePin);
            activeFirebasePin = null;
        }
        pin = String.format("%06d", secureRandom.nextInt(1_000_000));
        for (int i = 0; i < 6; i++) {
            pinDigits[i].setText(String.valueOf(pin.charAt(i)));
        }
        if (current != null) {
            beamService.uploadPin(
                    pin,
                    current.code != null ? current.code : "",
                    current.title != null ? current.title : "",
                    current.language != null ? current.language : "");
            activeFirebasePin = pin;
            BadgeTracker.recordBeamSession(requireContext());
        }
        secondsLeft = TIMER_SEC;
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker =
                new Runnable() {
                    @Override
                    public void run() {
                        if (secondsLeft <= 0) {
                            pinTimer.setText("0:00");
                            barL.setProgress(0);
                            barR.setProgress(0);
                            return;
                        }
                        secondsLeft--;
                        int m = secondsLeft / 60;
                        int s = secondsLeft % 60;
                        pinTimer.setText(String.format("%d:%02d", m, s));
                        barL.setProgress(secondsLeft);
                        barR.setProgress(secondsLeft);
                        handler.postDelayed(this, 1000);
                    }
                };
        handler.post(ticker);
        XpManager.addXp(requireContext(), 3);
    }

    private void copyPin() {
        ClipboardManager cm =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("pin", pin));
        Toast.makeText(requireContext(), "PIN copied", Toast.LENGTH_SHORT).show();
    }

    private void buildQr() {
        if (current == null || current.code == null) return;
        try {
            qrImage.setImageBitmap(QrUtils.encodeQr(current.code, 384));
        } catch (WriterException e) {
            Toast.makeText(requireContext(), "Code too long for QR", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ticker != null) handler.removeCallbacks(ticker);
    }
}
