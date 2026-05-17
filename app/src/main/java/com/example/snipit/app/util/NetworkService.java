package com.example.snipit.app.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import java.util.HashMap;
import java.util.Map;

public class NetworkService {

    /**
     * Gathers all local network details (IP, Gateway, Mask) from WifiManager.
     */
    public static Map<String, String> getLocalNetworkInfo(Context context) {
        Map<String, String> info = new HashMap<>();
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                DhcpInfo dhcp = wm.getDhcpInfo();
                WifiInfo winfo = wm.getConnectionInfo();
                
                if (winfo != null) {
                    info.put("local_ip", Formatter.formatIpAddress(winfo.getIpAddress()));
                    info.put("ssid", winfo.getSSID() != null ? winfo.getSSID().replace("\"", "") : "—");
                }
                
                if (dhcp != null) {
                    info.put("gateway", Formatter.formatIpAddress(dhcp.gateway));
                    info.put("netmask", Formatter.formatIpAddress(dhcp.netmask));
                    info.put("dns1", Formatter.formatIpAddress(dhcp.dns1));
                }
            }
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    /**
     * Logic for fetching public IP is typically handled via external API calls 
     * in the fragment (e.g., ipify.org) to avoid blocking the main service.
     */

    public static void syncCurrentSession(Context context) {
        try {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            Map<String, String> netInfo = getLocalNetworkInfo(context);
            new Thread(() -> {
                String publicIp = "Unavailable";
                try {
                    java.net.URL url = new java.net.URL("https://api.ipify.org");
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(4000);
                    if (c.getResponseCode() == 200) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream()));
                        publicIp = br.readLine();
                        br.close();
                    }
                    c.disconnect();
                } catch (Exception ignored) {}
                netInfo.put("public_ip", publicIp);
                netInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));

                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("current_session")
                    .setValue(netInfo);
            }).start();
        } catch (Exception e) {
            android.util.Log.e("NetworkService", "Failed to sync session network details", e);
        }
    }
}
