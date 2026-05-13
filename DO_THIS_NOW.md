# ⚡ IMMEDIATE ACTION ITEMS - Do These NOW

## Your App Crashes. Here's What to Do RIGHT NOW.

---

## 🔴 STEP 1: Read the Guides (5 minutes)

Open these files in VS Code in this order:

1. **START_HERE_PHASE_1.md** - Quick overview
2. **QUICK_CHECKLIST.md** - Simple checklist to follow
3. **PHASE_1_BREAKDOWN.md** - Detailed implementation (bookmark this)

---

## 🟠 STEP 2: Fix the #1 Crash (30 minutes)

Open: `app/src/main/java/com/example/snipit/app/ui/MainActivity.java`

Find this code (around line 75):
```java
private void updateNavUi(int selected) {
    int[] ids = {R.id.nav_vault, R.id.nav_dex, R.id.nav_beam, 
                 R.id.nav_ai, R.id.nav_snap, R.id.nav_xp, R.id.nav_me};
    for (int i = 0; i < ids.length; i++) {
        View v = findViewById(ids[i]);
        v.setAlpha(i == selected ? 1.0f : 0.5f);  // ← CRASH HERE!
    }
}
```

**Replace with:**
```java
private void updateNavUi(int selected) {
    int[] ids = {R.id.nav_vault, R.id.nav_dex, R.id.nav_beam, 
                 R.id.nav_ai, R.id.nav_snap, R.id.nav_xp, R.id.nav_me};
    for (int i = 0; i < ids.length; i++) {
        View v = findViewById(ids[i]);
        if (v != null) {  // ← ADD THIS CHECK
            v.setAlpha(i == selected ? 1.0f : 0.5f);
        }
    }
}
```

**Result:** One crash fixed ✅

---

## 🟠 STEP 3: Fix ProfileFragment (45 minutes)

Open: `app/src/main/java/com/example/snipit/app/ui/ProfileFragment.java`

Find: The `onViewCreated()` method

**Add this at the beginning of onViewCreated():**
```java
// Fetch Firebase user data
FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
if (firebaseUser != null) {
    String displayName = firebaseUser.getDisplayName();
    if (displayName != null) {
        profileNameTv.setText(displayName);
    }
    // Load profile photo if available
    if (firebaseUser.getPhotoUrl() != null) {
        // Load image using Glide/Picasso
    }
} else {
    // User not logged in, redirect to login
    startActivity(new Intent(getContext(), com.example.snipit.app.auth.LoginActivity.class));
    getActivity().finish();
}
```

**Result:** Profile shows real user data ✅

---

## 🟠 STEP 4: Test the Fixes (15 minutes)

1. Plug in your Android device (or use emulator)
2. In Android Studio: `Run` → `Run 'app'`
3. Watch for crashes
4. Try clicking each navigation tab

**Expected result:**
- App launches without crashing ✅
- Clicking tabs switches fragments ✅
- Profile shows your username ✅

---

## 📋 After You Fix Crashes: Next Steps

Once the app stops crashing, follow QUICK_CHECKLIST.md:
- [ ] Tier 1: Crash fixes (DONE ✅)
- [ ] Tier 2: Navigation icons (2 hours)
- [ ] Tier 3: Handbook search (1.5 hours)
- [ ] Tier 4: UI polish (1-2 hours)

---

## 🆘 Help! I Got an Error

### Error: "Cannot resolve symbol 'FirebaseAuth'"
**Fix:** Make sure firebase-auth dependency is in build.gradle.kts

### Error: "profileNameTv is null"
**Fix:** Make sure you have `profileNameTv = v.findViewById(R.id.profile_name);` in onViewCreated()

### Error: "v.setAlpha() still crashes"
**Fix:** You didn't add the `if (v != null)` check. Try again.

### App still crashes on startup?
**Fix:** Check logcat for the full error. Read DIAGNOSIS.md for help.

---

## ✅ Completion Checklist

- [ ] Downloaded/opened all 5 guide files
- [ ] Fixed MainActivity.java null pointer check
- [ ] Fixed ProfileFragment Firebase sync
- [ ] Tested that app launches without crashing
- [ ] Verified all 7 navigation tabs work
- [ ] Profile shows your real username

**If all checked:** Tier 1 is complete! Move to Tier 2. 🎉

---

## 🎯 That's It For Right Now

You have everything you need. Just follow these steps:

1. Read START_HERE_PHASE_1.md
2. Fix MainActivity.java (copy-paste the code above)
3. Fix ProfileFragment.java (copy-paste the code above)
4. Test in Android Studio
5. Move to QUICK_CHECKLIST.md for Tier 2

**Time: 90 minutes until your app stops crashing.**

---

**Start now. Don't overthink it. You've got this! 🚀**
