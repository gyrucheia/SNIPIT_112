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
import com.example.snipit.app.util.QrUtils;
import com.example.snipit.app.util.XpManager;
import com.google.zxing.WriterException;
import java.util.Random;

public class BeamFragment extends Fragment {

    public static final String ARG_SNIP_ID = "snip_id";

    private static final int TIMER_SEC = 300;

    private SnipRepository repo;
    private Snip current;
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
                            title.setText("Snippet not found");
                            lang.setText("—");
                            code.setText("");
                            return;
                        }
                        current = s;
                        bindSnip(s);
                        regeneratePinAndTimer();
                    });
        } else {
            repo.getLatestSnip(
                    s -> {
                        if (s != null) {
                            current = s;
                            bindSnip(s);
                            regeneratePinAndTimer();
                        } else {
                            title.setText("No snippet yet");
                            lang.setText("—");
                            code.setText("Create a snip in Vault first.");
                        }
                    });
        }
    }

    private void bindSnip(Snip s) {
        if (s == null) return;
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
        Random r = new Random();
        pin = String.format("%06d", r.nextInt(1_000_000));
        for (int i = 0; i < 6; i++) {
            pinDigits[i].setText(String.valueOf(pin.charAt(i)));
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
