package com.example.snipit.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.data.DexContent;
import com.example.snipit.app.data.DexDoc;
import com.example.snipit.app.util.NetworkUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DexFragment extends Fragment {

    private final ExecutorService netExec = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tabCat;
    private TextView tabHttp;
    private TextView tabIp;
    private LinearLayout panelCategories;
    private View panelHttp;
    private View panelIp;
    private View cmdListContainer;
    private RecyclerView cmdRv;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tabCat = v.findViewById(R.id.tab_categories);
        tabHttp = v.findViewById(R.id.tab_http);
        tabIp = v.findViewById(R.id.tab_ip);
        panelCategories = v.findViewById(R.id.panel_categories);
        panelHttp = v.findViewById(R.id.panel_http);
        panelIp = v.findViewById(R.id.panel_ip);
        cmdListContainer = v.findViewById(R.id.cmd_list_container);
        cmdRv = v.findViewById(R.id.cmd_recycler);

        RecyclerView httpRv = v.findViewById(R.id.http_recycler);
        httpRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        httpRv.setAdapter(new HttpRowAdapter(DexContent.allHttpRows()));

        RecyclerView portRv = v.findViewById(R.id.port_recycler);
        portRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        portRv.setAdapter(new PortRowAdapter(DexContent.allPortRows()));

        TextView localIp = v.findViewById(R.id.local_ip);
        localIp.setText(NetworkUtils.getLanIpv4());

        TextView publicIp = v.findViewById(R.id.public_ip);
        publicIp.setText("…");
        fetchPublicIp(publicIp);

        cmdRv.setLayoutManager(new LinearLayoutManager(requireContext()));

        v.findViewById(R.id.card_git)
                .setOnClickListener(x -> openCmdList(v, "Git", DexContent.gitWith(requireContext())));
        v.findViewById(R.id.card_linux)
                .setOnClickListener(x -> openCmdList(v, "Linux", DexContent.linuxWith(requireContext())));
        v.findViewById(R.id.card_powershell)
                .setOnClickListener(
                        x -> openCmdList(v, "PowerShell", DexContent.powershellWith(requireContext())));
        v.findViewById(R.id.card_adb)
                .setOnClickListener(x -> openCmdList(v, "ADB", DexContent.adbWith(requireContext())));

        v.findViewById(R.id.btn_back_categories).setOnClickListener(x -> showCategoryGrid());

        tabCat.setOnClickListener(x -> showTab(0));
        tabHttp.setOnClickListener(x -> showTab(1));
        tabIp.setOnClickListener(x -> showTab(2));

        showTab(0);
    }

    private void showTab(int which) {
        int green = requireContext().getResources().getColor(R.color.accent_green, null);
        int muted = requireContext().getResources().getColor(R.color.text_muted, null);

        tabCat.setTextColor(which == 0 ? green : muted);
        tabHttp.setTextColor(which == 1 ? green : muted);
        tabIp.setTextColor(which == 2 ? green : muted);
        tabCat.setBackgroundResource(which == 0 ? R.drawable.bg_card : 0);
        tabHttp.setBackgroundResource(which == 1 ? R.drawable.bg_card : 0);
        tabIp.setBackgroundResource(which == 2 ? R.drawable.bg_card : 0);

        panelCategories.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        panelHttp.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        panelIp.setVisibility(which == 2 ? View.VISIBLE : View.GONE);

        if (which == 0) {
            showCategoryGrid();
        }
    }

    private void showCategoryGrid() {
        cmdListContainer.setVisibility(View.GONE);
        for (int i = 0; i < panelCategories.getChildCount(); i++) {
            View child = panelCategories.getChildAt(i);
            if (child.getId() == R.id.cmd_list_container) {
                child.setVisibility(View.GONE);
            } else {
                child.setVisibility(View.VISIBLE);
            }
        }
    }

    private void openCmdList(View root, String title, DexDoc[] data) {
        for (int i = 0; i < panelCategories.getChildCount(); i++) {
            View child = panelCategories.getChildAt(i);
            if (child.getId() == R.id.cmd_list_container) {
                child.setVisibility(View.VISIBLE);
            } else {
                child.setVisibility(View.GONE);
            }
        }
        TextView t = root.findViewById(R.id.cmd_list_title);
        t.setText(title.toUpperCase() + " Commands");
        cmdRv.setAdapter(new CmdRowAdapter(data));
    }

    private void fetchPublicIp(TextView target) {
        netExec.execute(
                () -> {
                    String result = "Unavailable";
                    try {
                        URL url = new URL("https://api.ipify.org");
                        HttpURLConnection c = (HttpURLConnection) url.openConnection();
                        c.setConnectTimeout(4000);
                        c.setReadTimeout(4000);
                        if (c.getResponseCode() == 200) {
                            BufferedReader br =
                                    new BufferedReader(
                                            new InputStreamReader(c.getInputStream()));
                            result = br.readLine();
                            br.close();
                        }
                        c.disconnect();
                    } catch (Exception ignored) {
                    }
                    String line = result;
                    mainHandler.post(() -> target.setText(line));
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        netExec.shutdown();
    }
}
