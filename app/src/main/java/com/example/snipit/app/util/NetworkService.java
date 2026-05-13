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
}
