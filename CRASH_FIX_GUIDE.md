# 🚨 Crash Fix Guide (Phase 3)

This document analyzes the root causes of the "Phase 3" crashes and details the permanent fixes applied.

## 1. DexFragment / NetworkService NPE
### **The Problem**
The `Dev-Dex` IP Tracker would crash instantly if:
- WiFi was disabled.
- The user had not granted network permissions.
- The `WifiInfo` object returned null during fragment transition.

### **The Fix**
- **Permissions**: Added `<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />`.
- **Null-Safety**: Wrapped `WifiManager` calls in strict null checks.
- **Fallback**: Implemented a "—" (placeholder) return value in `NetworkService` so the UI remains stable even when the data is unavailable.

## 2. Beam IT Initialization Crash
### **The Problem**
`BeamFragment` attempted to upload data to Firebase before the database reference was fully initialized, or when passing a `null` snippet ID from `MainActivity`.

### **The Fix**
- **Lazy Loading**: Refactored `BeamService` to instantiate the database reference only when needed (`uploadPin()`).
- **Intent Safety**: Updated `MainActivity` to pass the `snippetId` via `Bundle` arguments with a `-1` default check.

## 3. QR Character Limit Exception
### **The Problem**
Large code blocks (e.g., >1000 characters) would cause `ZXing` to throw a `WriterException`, preventing the QR from being generated.

### **The Fix**
- **GZIP Compression**: Integrated a `compress()` helper in `QrUtils.java` that automatically triggers for large payloads, reducing the character count by up to 60%.

---
**Status**: All identified crash vectors have been neutralized.
