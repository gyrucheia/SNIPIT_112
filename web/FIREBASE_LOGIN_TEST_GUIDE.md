# SnipIT Firebase Login - Testing Guide

Your SnipIT app now uses **Firebase Authentication** directly. Test it with your existing Firebase users!

---

## 🔧 Setup (Before Testing)

### Step 1: Add Your Firebase Config
1. Open `web/js/firebase-config.js`
2. Replace the placeholder values with your actual Firebase credentials:

```javascript
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",                                    // From Firebase Console
  authDomain: "YOUR_PROJECT.firebaseapp.com",               // From Firebase Console
  databaseURL: "https://YOUR_PROJECT-default-rtdb.firebaseio.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};
```

### Step 2: Get Firebase Credentials
```
1. Go to Firebase Console: https://console.firebase.google.com
2. Select your project
3. Click ⚙️ (Project Settings)
4. Scroll to "Your Apps" → Web Config
5. Copy all values
```

### Step 3: Enable Authentication in Firebase
```
1. Go to Firebase Console
2. Go to Authentication → Sign-in method
3. Enable "Email/Password"
4. Make sure your users exist in the Authentication tab
```

---

## 👥 Test Users (from your Firebase screenshot)

Use these existing users from your Firebase console:

| Email | Status |
|-------|--------|
| lyzettestamaria@gmail.com | ✅ Exists |
| dubukun07@gmail.com | ✅ Exists |
| gyrucheia@gmail.com | ✅ Exists |
| jamaicarmores0923@... | ✅ Exists |

**Important:** These are read-only. You need to know their Firebase passwords to test login.

---

## 📋 User Flow Testing (Step-by-Step)

### Test Flow 1: Login with Existing User

**Goal:** Verify user can login with Firebase credentials

**Steps:**
1. Open `web/index.html` in your browser
2. You should see the **SnipIT Login Page**
3. Enter an email: `lyzettestamaria@gmail.com`
4. Enter the password (ask if you don't know)
5. Click **"Sign In"**

**Expected Result:**
```
✅ Login successful! message appears
✅ Redirected to Dashboard
✅ User card shows user's name/email in sidebar
✅ User can see their Firebase snippets
```

**What happens behind the scenes:**
- Firebase Auth validates email/password
- onAuthStateChanged detects user is logged in
- User data loaded from Firebase Realtime DB
- Portal hidden, dashboard shown

---

### Test Flow 2: Create New Account

**Goal:** Verify new user registration works

**Steps:**
1. On login page, click **"Sign Up"** tab
2. Enter: 
   - Name: `Your Name`
   - Email: `youremail@example.com`
   - Password: `TestPassword123`
3. Click **"Create Account"**

**Expected Result:**
```
✅ Account created! message appears
✅ Automatically logged in
✅ Redirected to Dashboard with empty vault
✅ User card shows new user's name
```

**Verification in Firebase Console:**
```
1. Go to Firebase Console
2. Authentication → Users tab
3. You should see your new user in the list
```

---

### Test Flow 3: Logout

**Goal:** Verify logout clears session

**Steps:**
1. Click user card in sidebar
2. Click **"🚪 Logout"** button
3. Confirm the logout dialog

**Expected Result:**
```
✅ Redirected to login page
✅ All form fields are cleared
✅ "Logged out" toast appears
✅ Refreshing page shows login again (no auto-login)
```

---

### Test Flow 4: Add Snippet (Logged In)

**Goal:** Verify snippets save to Firebase

**Steps:**
1. Login with a user
2. Click **"+ New Snip"** button
3. Fill in:
   - Title: `My Test Snippet`
   - Language: `Java`
   - Tags: `#test,#firebase`
   - Code: `public static void main(String[] args) { }`
4. Click **"⚡ Commit to Vault"**

**Expected Result:**
```
✅ "Snippet saved to Firebase! +15 XP" message
✅ Snippet appears in Vault page
✅ Close modal and open again - snippet still there
```

**Verification in Firebase Console:**
```
1. Go to Firebase Console
2. Realtime Database
3. Look for: users → {uid} → snippets
4. You should see your snippet data
```

---

### Test Flow 5: Refresh - Data Persists

**Goal:** Verify data survives page refresh

**Steps:**
1. Login and add a snippet
2. Press F5 (refresh page)
3. Login again if needed

**Expected Result:**
```
✅ Page loads
✅ Login page appears (no auto-login with refresh)
✅ Login with same credentials
✅ Your snippet is still there in Vault
```

---

### Test Flow 6: Wrong Password Error

**Goal:** Verify error handling

**Steps:**
1. On login page, enter email
2. Enter wrong password (anything)
3. Click **"Sign In"**

**Expected Result:**
```
❌ "Wrong password." error message appears
❌ Modal stays open
❌ Redirects to login page (doesn't enter app)
```

---

### Test Flow 7: Non-existent User

**Goal:** Verify user not found error

**Steps:**
1. Enter fake email: `nouser@example.com`
2. Enter any password
3. Click **"Sign In"**

**Expected Result:**
```
❌ "User not found. Create account first." message
❌ Modal stays open
```

---

## 🧪 Test Checklist

Print this and check off as you test:

```
□ Firebase config updated with real credentials
□ Login page displays on first load
□ Can login with existing Firebase user
□ User card updates with logged-in user name
□ Can create new account
□ New account appears in Firebase Console
□ Can logout and return to login
□ Page refresh requires re-login
□ Can add snippets while logged in
□ Snippets save to Firebase (check console)
□ Snippets persist after refresh
□ Wrong password shows error
□ Non-existent user shows error
□ Multiple users can login with different credentials
□ Each user only sees their own snippets
```

---

## 🐛 Debugging Tips

### "Firebase config not set up" error
```
❌ Firebase config values are wrong
✅ Fix: Update firebase-config.js with real values
✅ Verify in Firebase Console → Project Settings
```

### "User not found" when trying to login
```
❌ User doesn't exist in Firebase yet
✅ Fix: Create user in Firebase Console → Authentication
✅ Or use "Sign Up" tab in login page
```

### "Can't add snippets"
```
❌ Firebase Realtime Database rules might be wrong
✅ Fix: Go to Firebase Console → Realtime Database → Rules
✅ Apply the security rules from FIREBASE_NETLIFY_SETUP.md
```

### Console shows red errors
```
Press F12 → Console tab
Look for error messages
Common issues:
- apiKey is wrong
- authDomain is wrong
- Firebase Auth not enabled
- Wrong database URL
```

---

## 📊 Expected Console Output (Open F12)

When page loads, you should see in DevTools Console:

```
✅ Firebase initialized successfully
✅ Signed in anonymously (before setup)
   OR
✅ Firebase user authenticated: abc123xyz...
✅ Firebase connection: ONLINE
```

---

## 🔄 Full User Journey (Test Scenario)

**Scenario: Company dev wants to save code snippets**

```
1. [DAY 1] New user visits https://yourapp.netlify.app
   → Sees login page
   → Clicks "Sign Up"
   → Creates account with email/password
   → Auto-logged in
   → Empty vault shown

2. [DAY 1] User adds first snippet
   → Clicks "+ New Snip"
   → Fills in: Title, Language, Code
   → Clicks "Commit to Vault"
   → Snippet appears in list
   → Data saved to Firebase

3. [DAY 2] User returns next day
   → Visits app again
   → Sees login page (no auto-login)
   → Enters email/password
   → Dashboard shown
   → Previous snippet still there (loaded from Firebase)

4. [DAY 2] User adds more snippets
   → Repeat step 2
   → Multiple snippets accumulate

5. [DAY 7] User manages account
   → Checks profile
   → See all their snippets
   → Can export vault as JSON
   → Can logout
```

---

## 🌐 Deployment Testing

Once working locally:

```
1. Deploy web/ folder to Netlify
2. Open your Netlify URL
3. Repeat all tests above
4. Everything should work the same!
```

---

## 💾 Database Structure (What You're Testing)

When you add snippets, this is what's saved in Firebase:

```
users/
  {uid}/
    snippets/
      {id1}/
        title: "My Snippet"
        language: "Java"
        code: "public static..."
        tags: "#test"
        created: "2026-05-03..."
      {id2}/
        (more snippets)
    aiChat/
      {id1}/
        message: "user question"
        response: "AI response"
        timestamp: "..."
```

Each user (identified by `uid`) only sees their own data.

---

## ✅ Success Criteria

You're done testing when:

```
✅ Can login/signup with Firebase
✅ User data persists in Firebase
✅ Each user only sees their snippets
✅ Errors handle gracefully
✅ Mobile responsive (test on phone)
✅ Ready for Netlify deployment!
```

---

## 🚀 Next Steps

Once all tests pass:

1. **Deploy to Netlify**
   ```
   Drag web/ folder to netlify.com
   ```

2. **Share your live URL**
   ```
   https://your-app.netlify.app
   ```

3. **Invite users to test**
   ```
   They can sign up with email/password
   All data saved to your Firebase!
   ```

---

## 📞 Troubleshooting Quick Links

- **Firebase Console**: https://console.firebase.google.com
- **Firebase Docs**: https://firebase.google.com/docs
- **Authentication Setup**: https://firebase.google.com/docs/auth/web/start
- **Realtime Database**: https://firebase.google.com/docs/database/web/start

---

## Questions?

Check these files:
- `FIREBASE_NETLIFY_SETUP.md` - Detailed setup
- `NETLIFY_DEPLOYMENT.md` - Deployment guide
- `ARCHITECTURE.md` - How it all works

**You're ready to test! 🎉**
