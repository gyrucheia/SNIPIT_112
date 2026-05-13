# SnipIT Web: Sync, Deployment & Architecture

## 🔄 APP ↔ WEB Data Synchronization

### How Same Account Works

Both the **Android app** and **Web portal** share the **same Firebase project**:

```
Firebase Project: snipit-d07da
├── Authentication (FirebaseAuth)
│   └── Users log in with email/password
├── Realtime Database
│   └── users/{uid}/
│       ├── snippets/     ← Shared vault
│       ├── aiChat/       ← Chat history (WEB ONLY - see below)
│       ├── beamSessions/ ← Beam logs
│       └── profile/      ← User settings
```

### Current Sync Status ✅

| Feature | Android App | Web Portal | Synced? |
|---------|-------------|-----------|---------|
| **Snippets** | Firebase | Firebase | ✅ Full sync |
| **Authentication** | FirebaseAuth | FirebaseAuth | ✅ Same account |
| **XP/Badges** | Local Room DB | LocalStorage | ❌ Separate |
| **AI Chat History** | Firebase + Room | Firebase | ✅ Synced |

### Data Storage Differences

#### Android App (Why Local Storage?)
```
✅ Local Storage Reasons:
- Offline-first (works without internet)
- User privacy (data stays on phone)
- Faster access (no network calls)
- Battery efficient (less data sync)
- SQLite Room Database (Android best practice)

📱 Room Database Tables:
- ai_sessions → Chat conversations
- ai_messages → Individual messages (FK to sessions)
- snippets → Cached snippets
```

#### Web Portal (Why Firebase?)
```
✅ Firebase Reasons:
- Stateless (no server storage)
- Real-time sync (multiple browser tabs)
- Cloud backup (don't lose chat history)
- Can restore from any device
- Works across browsers/devices

☁️ Firebase Structure:
users/{uid}/aiChat/{chatId}
├── title (String)
├── messages[] (Array)
│   ├── user (String)
│   ├── ai (String)
│   └── timestamp (ISO)
└── lastUpdated (ISO)
```

---

## 🚀 Netlify Deployment

### Will Putting Web Folder on Netlify Affect It?

**Answer: ✅ NO - It will work perfectly!**

### How Netlify Deployment Works

```
Before (Development):
http://localhost:3000 (Web)  ←→  http://localhost:8000 (Python server)
                                      (FastAPI with CORS enabled)

After (Netlify Production):
https://yoursite.netlify.app (Web)  ←→  http://localhost:8000 (Your local server)
                                        OR Firebase only
```

### Setup for Netlify

#### Option 1: Firebase-Only (Recommended)
✅ **Best for:** Hosting web portal in the cloud permanently

```
1. Deploy web folder to Netlify (drag & drop)
2. Only uses Firebase (no local server needed)
3. Fully serverless architecture
4. Zero maintenance hosting

Required:
- Firebase config in index.html ✅ (already there)
- GitHub token in web (set in settings modal)
```

#### Option 2: With Local Python Server
⚠️ **Requires:** Your Python server to be accessible from internet

```
Problems:
- Your local machine must stay on 24/7
- Needs port forwarding/ngrok for web access
- CORS issues if server isn't configured
- Security concerns (exposing local machine)

api-client.js currently uses:
```javascript
return `${protocol}//localhost:8000`; // Won't work from Netlify!
```

Need to change to:
```javascript
// For production (Netlify)
return 'https://your-python-server.com'; // Your deployed server
```

### Required Changes for Netlify Deployment

#### 1. Environment Configuration
Create `netlify.toml`:
```toml
[build]
  command = "echo 'Static site - no build needed'"
  publish = "."

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[dev]
  # For local testing
  targetPort = 8000
```

#### 2. Update API Endpoints
In `web/js/api-client.js`:

```javascript
getServerUrl() {
  // Production (Netlify)
  if (window.location.hostname !== 'localhost') {
    return 'https://your-api-server.com'; // Your deployed Python API
  }
  // Development (local)
  const protocol = window.location.protocol;
  return `${protocol}//localhost:8000`;
}
```

#### 3. CORS Configuration (if using Python server)
In `server.py`:
```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "https://yoursite.netlify.app"  # Add your Netlify URL
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

### GitHub Token Security on Netlify

⚠️ **Security Note:** Never hardcode GitHub token in JavaScript!

#### ✅ Safe Approach:
1. Store token in Python server (`local.properties`)
2. Web calls: `GET /api/config` → server returns token
3. Web uses token only for this session

Current implementation already does this! ✅

```javascript
// web/js/api-client.js
async loadTokenFromServer() {
  const response = await fetch(`${this.serverUrl}/api/config`);
  const config = await response.json();
  this.apiKey = config.github_token; // Safe: from server only
}
```

### Deployment Checklist

- [ ] Firebase config is correct (test with demo login)
- [ ] Python server accessible from Netlify (if using server)
- [ ] CORS configured for your Netlify URL
- [ ] GitHub token in `local.properties` (not visible on Netlify)
- [ ] Test chat history persistence
- [ ] Test snippet sync across browsers
- [ ] Test authentication works on Netlify

---

## 🔐 Authentication Flow (Both Platforms)

### Android App
```
User → Firebase Auth → Authenticated ✓
          ↓
      Room Database (Local)
      Firebase Realtime DB (Snippets)
```

### Web Portal
```
User → Firebase Auth → Authenticated ✓
          ↓
      LocalStorage (XP, theme)
      Firebase Realtime DB (Snippets + Chat)
```

### Same Account Access
```
Login on Web with: user@example.com / password123
      ↓
Firebase Auth recognizes UID: abc123def
      ↓
Access users/abc123def/snippets/ (✅ SHARED with app)
      ↓
App also logged in as abc123def
      ↓
Both see same snippets! 🎉
```

---

## 📊 Data Sync Scenarios

### Scenario 1: User Saves Snippet on App
```
App → Add to Firebase/users/uid/snippets/123
   ↓
User opens Web → Firebase syncs → Snippet appears ✅
```

### Scenario 2: User Starts Chat on Web
```
Web → Chat saved to Firebase/users/uid/aiChat/456
   ↓
Chat appears in Web sidebar ✅
Note: Not visible in app (different storage) ℹ️
```

### Scenario 3: User Switches Devices
```
Device 1 (Web) → Save snippet → Firebase
    ↓
Device 2 (App) → Fetch snippets → Gets latest ✅
    ↓
Both devices in sync! 🎯
```

---

## ⚠️ Limitations to Know

### What SYNCS Between App & Web
- ✅ Snippets (code, language, tags)
- ✅ User authentication
- ✅ Device profile info
- ✅ Beam session logs

### What DOESN'T Sync
- ❌ Chat history (app uses local DB, web uses Firebase)
- ❌ XP/Badges (calculated locally)
- ❌ Cached data (different storage systems)
- ❌ Local snippets not backed up to Firebase

### Why These Differences?
- App: Offline-first, privacy-focused (local storage)
- Web: Cloud-focused, multi-device access (Firebase)
- Different architectures, same data sources

---

## 🔄 To Fully Sync Chat History (Optional Future)

To make chat history sync between app and web:

### Option A: Move App Chat to Firebase
```kotlin
// Replace Room Database with Firebase
firebaseAuth.getChatHistory(uid)
```
- Pros: Full sync, cloud backup
- Cons: Requires internet, slower on slow networks

### Option B: Add Room DB to Web
```javascript
// Use IndexedDB or local SQLite
// Not practical for web (different architecture)
```

### Current Recommendation
Keep as-is for now:
- App chats: Local (faster, private)
- Web chats: Firebase (cloud backup, share devices)
- Snippets: Always synced via Firebase ✅

---

## 📝 Summary Table

| Aspect | App | Web | Status |
|--------|-----|-----|--------|
| **Storage** | Room DB (SQLite) | Firebase + LS | Different |
| **Snippets** | Firebase | Firebase | Synced ✅ |
| **Chat** | Local DB | Firebase | Separate ℹ️ |
| **Auth** | FirebaseAuth | FirebaseAuth | Synced ✅ |
| **Deploy** | Play Store | Netlify | Independent |
| **Server** | None needed | Optional (local) | Separate |

---

## 🎯 Next Steps

1. **Test on Netlify** → Deploy web folder to Netlify
2. **Configure CORS** → Update Python server (if using)
3. **Test sync** → Save snippet on app, check web
4. **Monitor** → Check Netlify logs for errors
5. **Backup** → Regularly export Firebase data

For questions on specific setup, check `DEPLOYMENT_CHECKLIST.md`
