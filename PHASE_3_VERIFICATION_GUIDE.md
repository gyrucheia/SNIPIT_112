# 🔍 Phase 3 Verification Guide

Follow these 10 tests to ensure the SnipIT IT Intelligence core is functioning correctly.

## 📡 Beam IT & Compression Tests
1. **Large QR Generation**: Create a snippet with 300+ lines of code. Open **Beam IT** -> **QR Mode**. Verify the QR generates and does not show an error.
2. **Compression Prefix**: Scan the QR with a standard phone camera. Verify the text starts with `z!`.
3. **Auto-Brightness**: Enter the **Beam IT** fragment and tap the **QR** tab. The screen should immediately brighten. Switch back to **PIN** tab; it should dim.
4. **Teleportation Integrity**: Upload a large compressed snippet to Firebase via Beam and verify it is retrieved correctly on the web receiver.

## 🏗️ Dev-Dex & Search Tests
5. **JSON Data Loading**: Open **Dev-Dex**. Tap **HTTP CODES**. Verify that entries like `404 Not Found` appear immediately.
6. **Master Search**: Search for `chmod`. Verify that the Linux command appears.
7. **Regex Hub**: Search for `Email`. Verify the Regex pattern is displayed and the "Copy" function works.
8. **Navigation Persistence**: Search for a command, tap it to see documentation, then go back. The search results should still be visible.

## 🛠️ Network Diagnostics Tests
9. **LAN Info Accuracy**: Open **Dev-Dex** -> **IP TOOLS**. Verify the **Local IP**, **Gateway**, and **Netmask** match your system settings.
10. **Crash Resilience**: Turn OFF your WiFi and open the **IP TOOLS** tab. The app should NOT crash and should display "—" for network fields.

---
**Pass/Fail Log**: 
- [ ] Test 1-4: Beam Optimization
- [ ] Test 5-8: Handbook Intelligence
- [ ] Test 9-10: Network Diagnostics
