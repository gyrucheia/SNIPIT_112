package com.example.snipit.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.data.DexContent;
import com.example.snipit.app.data.DexDoc;
import com.example.snipit.app.util.HandbookManager;
import com.example.snipit.app.util.NetworkService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DexFragment extends Fragment {

    private final ExecutorService netExec = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EditText searchInput;
    private ImageView btnClearSearch;
    private TextView tabCat, tabHttp, tabIp, cmdListTitle;
    private View panelCategories, panelHttp, panelIp, cmdListContainer;
    private RecyclerView cmdRv;
    private HandbookAdapter searchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Bind Views
        searchInput = v.findViewById(R.id.dex_search_input);
        btnClearSearch = v.findViewById(R.id.btn_clear_search);
        tabCat = v.findViewById(R.id.tab_categories);
        tabHttp = v.findViewById(R.id.tab_http);
        tabIp = v.findViewById(R.id.tab_ip);
        cmdListTitle = v.findViewById(R.id.cmd_list_title);
        
        panelCategories = v.findViewById(R.id.panel_categories);
        panelHttp = v.findViewById(R.id.panel_http);
        panelIp = v.findViewById(R.id.panel_ip);
        cmdListContainer = v.findViewById(R.id.cmd_list_container);
        cmdRv = v.findViewById(R.id.cmd_recycler);

        // Setup Stable Search Adapter
        searchAdapter = new HandbookAdapter(new ArrayList<>());
        cmdRv.setLayoutManager(new LinearLayoutManager(getContext()));
        cmdRv.setAdapter(searchAdapter);

        // Setup Other Recyclers
        RecyclerView httpRv = v.findViewById(R.id.http_recycler);
        if (httpRv != null) {
            httpRv.setLayoutManager(new LinearLayoutManager(getContext()));
            httpRv.setAdapter(new HttpRowAdapter(DexContent.allHttpRows()));
        }

        RecyclerView portRv = v.findViewById(R.id.port_recycler);
        if (portRv != null) {
            portRv.setLayoutManager(new LinearLayoutManager(getContext()));
            portRv.setAdapter(new PortRowAdapter(DexContent.allPortRows()));
        }

        // Network Stats
        refreshNetworkStats(v);

        // Grid Click Listeners
        v.findViewById(R.id.card_git).setOnClickListener(x -> openCategoryList("GIT"));
        v.findViewById(R.id.card_linux).setOnClickListener(x -> openCategoryList("LINUX"));
        v.findViewById(R.id.card_powershell).setOnClickListener(x -> openCategoryList("POWERSHELL"));
        v.findViewById(R.id.card_adb).setOnClickListener(x -> openCategoryList("ADB"));
        v.findViewById(R.id.card_regex).setOnClickListener(x -> openCategoryList("REGEX"));

        v.findViewById(R.id.btn_close_list).setOnClickListener(x -> resetToGrid());
        btnClearSearch.setOnClickListener(x -> searchInput.setText(""));

        tabCat.setOnClickListener(x -> showTab(0));
        tabHttp.setOnClickListener(x -> showTab(1));
        tabIp.setOnClickListener(x -> showTab(2));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        showTab(0);
    }

    private void refreshNetworkStats(View v) {
        TextView localIp = v.findViewById(R.id.local_ip);
        TextView publicIp = v.findViewById(R.id.public_ip);
        if (getContext() == null || localIp == null || publicIp == null) return;
        
        Map<String, String> netInfo = NetworkService.getLocalNetworkInfo(getContext());
        localIp.setText(netInfo.getOrDefault("local_ip", "—"));
        
        publicIp.setText("…");
        fetchPublicIp(publicIp);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            if (btnClearSearch != null) btnClearSearch.setVisibility(View.GONE);
            resetToGrid();
            return;
        }

        if (btnClearSearch != null) btnClearSearch.setVisibility(View.VISIBLE);
        
        // Update tab colors directly to avoid showTab() recursion
        if (getContext() != null) {
            int green = getContext().getColor(R.color.terminal_green);
            int muted = getContext().getColor(R.color.text_muted);
            if (tabCat != null) tabCat.setTextColor(green);
            if (tabHttp != null) tabHttp.setTextColor(muted);
            if (tabIp != null) tabIp.setTextColor(muted);
        }
        
        panelCategories.setVisibility(View.GONE);
        if (panelHttp != null) panelHttp.setVisibility(View.GONE);
        if (panelIp != null) panelIp.setVisibility(View.GONE);
        
        cmdListContainer.setVisibility(View.VISIBLE);
        cmdListTitle.setText("SEARCH RESULTS");
        
        if (getContext() != null) {
            HandbookManager manager = HandbookManager.getInstance(getContext());
            searchAdapter.updateEntries(manager.search(query));
        }
    }

    private void showTab(int which) {
        if (getContext() == null) return;
        int green = getContext().getColor(R.color.terminal_green);
        int muted = getContext().getColor(R.color.text_muted);

        if (tabCat != null) {
            tabCat.setTextColor(which == 0 ? green : muted);
            tabCat.setBackgroundResource(which == 0 ? R.drawable.bg_card : 0);
        }
        if (tabHttp != null) {
            tabHttp.setTextColor(which == 1 ? green : muted);
            tabHttp.setBackgroundResource(which == 1 ? R.drawable.bg_card : 0);
        }
        if (tabIp != null) {
            tabIp.setTextColor(which == 2 ? green : muted);
            tabIp.setBackgroundResource(which == 2 ? R.drawable.bg_card : 0);
        }

        if (panelCategories != null) panelCategories.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        if (panelHttp != null) panelHttp.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        if (panelIp != null) panelIp.setVisibility(which == 2 ? View.VISIBLE : View.GONE);

        if (which != 0) {
            if (cmdListContainer != null) cmdListContainer.setVisibility(View.GONE);
        } else if (searchInput != null && !searchInput.getText().toString().isEmpty()) {
            // If switching to tab 0 and text is present, show results panel instead of grid
            if (panelCategories != null) panelCategories.setVisibility(View.GONE);
            if (cmdListContainer != null) cmdListContainer.setVisibility(View.VISIBLE);
        }
    }

    private void resetToGrid() {
        if (cmdListContainer != null) cmdListContainer.setVisibility(View.GONE);
        if (panelCategories != null) panelCategories.setVisibility(View.VISIBLE);
    }

    private void openCategoryList(String category) {
        if (panelCategories != null) panelCategories.setVisibility(View.GONE);
        if (cmdListContainer != null && getContext() != null) {
            cmdListContainer.setVisibility(View.VISIBLE);
            cmdListTitle.setText(category.toUpperCase() + " REFERENCE");
            
            List<com.example.snipit.app.models.HandbookEntry> entries = 
                    com.example.snipit.app.util.HandbookManager.getInstance(getContext()).getByCategory(category);
            
            searchAdapter.updateEntries(entries);
            cmdRv.setAdapter(searchAdapter);
        }
    }

    private void fetchPublicIp(TextView target) {
        netExec.execute(() -> {
            String result = "Unavailable";
            try {
                URL url = new URL("https://api.ipify.org");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(4000);
                c.setReadTimeout(4000);
                if (c.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    result = br.readLine();
                    br.close();
                }
                c.disconnect();
            } catch (Exception ignored) {}
            String line = result;
            mainHandler.post(() -> {
                if (isAdded() && target != null) target.setText(line);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        netExec.shutdown();
    }
}
