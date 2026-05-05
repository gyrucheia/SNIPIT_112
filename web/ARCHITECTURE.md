# SnipIT Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     NETLIFY (Frontend)                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              SnipIT Web Portal                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │   │
│  │  │   Dashboard  │  │    Vault     │  │ Beam Station│  │   │
│  │  └──────────────┘  └──────────────┘  └─────────────┘  │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │   │
│  │  │  AI Chat     │  │   Profile    │  │  Settings   │  │   │
│  │  └──────────────┘  └──────────────┘  └─────────────┘  │   │
│  │                                                         │   │
│  │  ┌─────────────────────────────────────────────────┐  │   │
│  │  │      API Client + Firebase Client             │  │   │
│  │  └─────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │ HTTPS
               │
        ┌──────▼────────┐
        │  FIREBASE API  │
        └──────┬────────┘
               │
        ┌──────▼───────────────────────────────┐
        │   FIREBASE REALTIME DATABASE          │
        │  ┌─────────────────────────────────┐ │
        │  │ users/                          │ │
        │  │  └─ {uid}/                      │ │
        │  │     ├─ snippets/                │ │
        │  │     ├─ aiChat/                  │ │
        │  │     └─ beamSessions/            │ │
        │  └─────────────────────────────────┘ │
        └──────────────────────────────────────┘
```

## Data Flow

### Saving a Snippet

```
User Types Snippet
      ↓
Click "Save"
      ↓
app.js → saveNewSnip()
      ↓
firebase-config.js → addSnippet()
      ↓
Firebase API
      ↓
Realtime Database Stores: users/{uid}/snippets/{id}
      ↓
LocalStorage Updated (backup)
      ↓
Toast: "Snippet saved!"
```

### Loading Snippets

```
Page Loads
      ↓
firebase-config.js → initializeFirebase()
      ↓
Firebase User Auth (anonymous)
      ↓
firebase-config.js → getSnippets()
      ↓
Realtime Database Query: users/{uid}/snippets
      ↓
Data Downloaded
      ↓
app.js → loadVault()
      ↓
Display in UI
```

### Offline Mode

```
No Internet Connection
      ↓
Firebase request fails
      ↓
Fallback to LocalStorage
      ↓
User can still work locally
      ↓
Changes marked as "synced: false"
      ↓
Connection Restored
      ↓
Auto-sync to Firebase
```

## Component Architecture

```
index.html
    │
    ├── firebase-config.js
    │   ├── Firebase SDK (CDN)
    │   ├── Initialize Firebase
    │   ├── User Authentication
    │   └── CRUD Operations
    │
    ├── api-client.js
    │   ├── Legacy API support
    │   ├── HTTP requests
    │   └── Fallback handling
    │
    └── app.js
        ├── UI Navigation (goPage)
        ├── Beam Station (PIN, QR)
        ├── AI Chat (sendAI)
        ├── Snippet Management (saveNewSnip)
        ├── Local Storage (fallback)
        └── Utilities (toast, modals)
```

## File Flow

```
Netlify
  └── web/
      ├── index.html (loaded first)
      │   ├── Loads firebase-config.js
      │   ├── Loads api-client.js
      │   ├── Loads app.js
      │   └── Renders HTML
      │
      ├── js/firebase-config.js
      │   └── Initializes Firebase connection
      │
      ├── js/api-client.js
      │   └── (Fallback for local development)
      │
      └── js/app.js
          └── Runs portal logic
```

## User Authentication Flow

```
User Visits Portal
      ↓
firebase-config.js checks user state
      ↓
Is user authenticated?
      ├─ YES → Load existing data
      └─ NO → Sign in anonymously
           ↓
      Firebase generates anonymous uid
           ↓
      User can now use portal
           ↓
      Data stored at: users/{uid}/...
```

## Deployment Pipeline

```
Local Development
      ↓
Update firebase-config.js
      ↓
Test locally
      ↓
Git Push (or drag-drop to Netlify)
      ↓
Netlify receives files
      ↓
Static site served globally on CDN
      ↓
Users access from netlify.app URL
      ↓
Portal connects to Firebase
      ↓
Live!
```

## Feature Integration Points

```
┌─────────────────────────────────────────┐
│           UI Components                 │
├─────────────────────────────────────────┤
│                                         │
│  Dashboard ─────┬─ Firebase (read)     │
│                 │                      │
│  Vault ─────────┼─ Firebase (CRUD)     │
│                 │  LocalStorage (backup)
│                 │                      │
│  Beam Station ──┼─ Log to Firebase     │
│                 │  Simulate locally    │
│                 │                      │
│  AI Chat ───────┼─ Firebase (save)     │
│                 │  API Client (send)   │
│                 │                      │
│  Profile ───────┼─ Firebase (read)     │
│                 │  LocalStorage        │
│                 │                      │
│  Settings ──────┼─ LocalStorage        │
│                 │  Firebase (prefs)    │
│                 │                      │
└─────────────────────────────────────────┘
```

## Real-time Sync Architecture

```
Browser A (User 1)
      ↓
Changes snippet
      ↓
Writes to Firebase
      ↓
Firebase Realtime DB
      ├─ Stores data
      └─ Broadcasts change
           ↓
Browser B (User 1 on another device)
      ├─ Receives update
      └─ Refresh automatically
```

## Security Model

```
Firebase Rules
├── Authenticate: users must sign in (anonymous OK)
├── Authorization: users/{$uid} only accessible to $uid
├── Validation: check data structure
└── Rate limiting: (via Firebase quotas)
```

## Performance Layers

```
┌─ Netlify CDN (Global delivery)
├─ Client-side caching (index.html)
├─ Firebase real-time sync (no polling)
├─ LocalStorage fallback (instant)
└─ JavaScript minification (future)
```

## Error Handling Flow

```
Error Occurs
      ↓
Try Firebase
      ├─ Success → Done
      └─ Fail → Try LocalStorage
             ├─ Success → Show offline mode
             └─ Fail → Show error toast
```

## Deployment Destinations

### During Development
```
Local Machine
   ↓
file:///C:/Users/.../SnipIT_1/web/index.html
   ↓
Connects to Firebase (from anywhere)
```

### Production
```
GitHub Repository (optional)
   ↓
Netlify (automatic deploy)
   ↓
CDN Distribution (global)
   ↓
User Browser
   ↓
https://yourapp.netlify.app
   ↓
Connects to Firebase (from anywhere)
```

## Technology Stack

```
Frontend
├── HTML5 (structure)
├── CSS3 (modern styling)
└── Vanilla JavaScript (no dependencies)

Backend
└── Firebase
    ├── Authentication (anonymous)
    ├── Realtime Database (data storage)
    └── Hosting optional (using Netlify instead)

Hosting
└── Netlify
    ├── CDN global distribution
    ├── Automatic HTTPS
    └── Custom domain support

APIs
└── Firebase SDK (via CDN)
    └── No additional backend needed!
```

## Scalability

```
1-10 Users
├─ Firebase Free Tier ✓
└─ Netlify Free Tier ✓

10-100 Users
├─ Firebase may need upgrade
├─ Netlify still free ✓
└─ Consider monitoring

100+ Users
├─ Firebase Paid Plan
├─ Netlify Paid Plan (optional)
└─ Monitor costs and performance
```

---

This architecture keeps everything simple, secure, and scalable!
