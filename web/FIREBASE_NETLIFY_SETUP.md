# SnipIT Web Portal - Firebase + Netlify Setup Guide

Deploy your SnipIT web portal to Netlify with Firebase as the backend database.

## Prerequisites

- Firebase project already created (you have this set up already)
- Netlify account (free at netlify.com)
- Git/GitHub account (recommended for automated deployments)

## Step 1: Get Your Firebase Configuration

1. Go to your **Firebase Console** (console.firebase.google.com)
2. Select your SnipIT project
3. Click **Project Settings** (gear icon)
4. Scroll to "Your apps" section
5. Click on your web app, or create a new one if needed
6. Copy the Firebase config object - it looks like this:

```javascript
const firebaseConfig = {
  apiKey: "AIzaSyD...",
  authDomain: "snippit-abc123.firebaseapp.com",
  databaseURL: "https://snippit-abc123-default-rtdb.firebaseio.com",
  projectId: "snippit-abc123",
  storageBucket: "snippit-abc123.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123def456"
};
```

## Step 2: Configure Firebase in Your Web Portal

1. Open `web/js/firebase-config.js`
2. Replace the placeholder config:

```javascript
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",  // Replace with your actual key
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  databaseURL: "https://YOUR_PROJECT-default-rtdb.firebaseio.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};
```

3. Save the file

## Step 3: Set Up Firebase Rules (Important!)

Your Realtime Database needs security rules to allow users to save/read their own data:

1. Go to **Firebase Console** → Your Project → **Realtime Database**
2. Click **Rules** tab
3. Replace the rules with:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid",
        "snippets": {
          "$snippet": {
            ".validate": "newData.hasChildren(['title', 'language', 'code'])"
          }
        },
        "aiChat": {
          "$chat": {
            ".validate": "newData.hasChildren(['message'])"
          }
        },
        "beamSessions": {
          "$session": {
            ".validate": "newData.hasChildren(['deviceInfo', 'timestamp'])"
          }
        }
      }
    }
  }
}
```

4. Click **Publish**

## Step 4: Deploy to Netlify

### Option A: Direct Upload

1. Go to **netlify.com** → Sign up/Login
2. Drag and drop the `web` folder onto Netlify
3. Your portal is now live!

### Option B: Using Git (Recommended)

1. **Initialize Git in your SnipIT_1 folder**:
```bash
git init
git add web/
git commit -m "Add SnipIT web portal"
```

2. **Push to GitHub**:
```bash
git remote add origin https://github.com/yourusername/snipit.git
git push -u origin main
```

3. **Connect to Netlify**:
   - Go to netlify.com
   - Click "New site from Git"
   - Select GitHub and your repository
   - Set build command: (leave empty, just serving static files)
   - Set publish directory: `web`
   - Click "Deploy"

### Option C: Using Netlify CLI

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy from SnipIT_1 directory
cd SnipIT_1
netlify deploy --dir web --prod
```

## Step 5: Verify Firebase Connection

After deployment:

1. Open your Netlify URL in browser (e.g., `https://yourapp.netlify.app`)
2. Open browser console (F12)
3. You should see:
   - "Firebase initialized successfully"
   - "Firebase user authenticated: [anonymous-id]"
   - "Firebase connection: ONLINE"

4. Test adding a snippet
5. Check Firebase Console → Realtime Database to see your data!

## Step 6: Configure Environment Variables (Optional but Recommended)

For better security, use environment variables instead of hardcoding Firebase config:

### In Netlify:

1. Go to **Site Settings** → **Build & Deploy** → **Environment**
2. Add variables:
   ```
   REACT_APP_FIREBASE_API_KEY = "your-api-key"
   REACT_APP_FIREBASE_AUTH_DOMAIN = "your-auth-domain"
   ... (add all config values)
   ```

### In `web/js/firebase-config.js`:

```javascript
const firebaseConfig = {
  apiKey: window.__ENV__?.FIREBASE_API_KEY || "FALLBACK_KEY",
  authDomain: window.__ENV__?.FIREBASE_AUTH_DOMAIN || "FALLBACK_DOMAIN",
  // ... etc
};
```

## Troubleshooting

### "Firebase not initialized"
- Check console for errors
- Verify Firebase config is correct
- Check Firebase project is active

### "Permission denied" when saving snippets
- Check Firebase Rules are published
- Verify database rules allow writes to `users/{uid}/snippets`

### Snippets not showing up
- Check browser console for errors
- Verify Firebase Realtime Database is enabled
- Check if data is in Firebase Console → Database

### Slow loading
- Firebase may need a moment to initialize
- Check Network tab in DevTools
- Verify database region is close to users

## Features

### Automatic Sync
- Snippets auto-save to Firebase
- Works offline with localStorage fallback
- Syncs when connection restored

### Data Structure
```
users/
  {uid}/
    snippets/
      {snippetId}/
        - id
        - title
        - language
        - code
        - tags
        - created
        - updated
    aiChat/
      {messageId}/
        - message
        - response
        - timestamp
    beamSessions/
      {sessionId}/
        - deviceInfo
        - timestamp
        - snippetsSent
```

## Security Notes

- ✅ Each user only sees their own data (enforced by rules)
- ✅ Anonymous authentication enabled
- ✅ Database rules prevent unauthorized access
- ⚠️ Keep API key secure (it's somewhat public in browser code, but that's normal for Firebase)
- ⚠️ Regularly review Firebase security rules

## Next Steps

After deployment:

1. ✅ Share your Netlify URL
2. ✅ Test all features (add snippets, chat, beam station)
3. ✅ Monitor Firebase usage in console
4. ✅ Set up alerts for quota limits
5. ✅ Consider Firebase upgrade plan if needed

## Useful Links

- [Firebase Console](https://console.firebase.google.com)
- [Netlify Dashboard](https://app.netlify.com)
- [Firebase Realtime Database Docs](https://firebase.google.com/docs/database)
- [Netlify Deployment Docs](https://docs.netlify.com)

---

**Version**: 1.0  
**Last Updated**: May 2026  
**Status**: Production Ready
