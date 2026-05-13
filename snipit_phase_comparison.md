# SnipIT Web — Phase-by-Phase Comparison & Audit

> Based on actual code in `/web/index.html`, `/web/js/firebase-config.js`, and `/web/js/app.js`
> Mobile app source: `https://github.com/gyrucheia/SNIPIT_112`

---

## 🔑 Key Discovery: The Stack is Already Set

Before comparing phases, here's what's already confirmed in your code:

| Item | What it IS |
|---|---|
| Database | **Firebase Realtime Database** (NOT Firestore) |
| Auth | **Firebase Auth** — Email/Password + Register |
| Firebase Path | `users/{uid}/snippets` |
| Beam Path | `beam_sessions/{sessionId}` |
| No SQL | ✅ Confirmed — fully Firebase |
| No Biometric | ✅ Confirmed — email/password login only |
| No PIN login | ✅ The PIN in Beam Station is for **device pairing**, not app login |

---

## Phase 1: Core Infrastructure & Firebase Integration

### What the Mobile App Has ✅
- Firebase Auth (Email, Google, GitHub)
- Firebase Realtime DB — snippets stored at `users/{uid}/snippets`
- Real-time beam path at `beam_sessions/{sessionId}`
- Offline persistence enabled

### What the Web ALREADY Has ✅
- `firebase-config.js` → full Firebase init
- Auth with `signInWithEmailAndPassword` and `createUserWithEmailAndPassword`
- `onAuthStateChanged` → auto-loads vault when user logs in
- Login/Register form with tabs in `index.html`
- Green/red dot connection status in header

### What the Web is MISSING or BROKEN ⚠️
| Gap | Description |
|---|---|
| ❌ No `onValue()` real-time listener | `getSnippets()` uses `get()` (one-time fetch). If classmate adds a snippet on phone, web will NOT update automatically. Need to replace with `onValue()` |
| ❌ `loadVaultFromFirebase()` is not called on auth | The vault loads snippets once but doesn't listen in real-time |
| ⚠️ Dev-Dex tab is in the nav but the page doesn't exist | There is no `page-devdex` in the HTML |
| ⚠️ Ctrl+K search not implemented | Only per-page search exists |
| ⚠️ Sync status dot shows "No device paired" | It's for Beam, not Firebase — Firebase status should be separate |

### What to Fix
```
1. Replace getSnippets() with a real-time onValue() listener
2. Call the listener immediately after user logs in
3. Add a visible "Firebase LIVE" indicator (separate from Beam dot)
```

---

## Phase 2: The "Beam" Receiver (Hardware Handoff)

### What the Mobile App Has ✅
- `QrScanActivity.java` — generates QR codes from snippet data
- Beam payload sent to `beam_sessions/{sessionId}` via Firebase RTDB
- ZXing library for high-density QR generation

### What the Web ALREADY Has ✅
- `beam_receiver.html` — a **separate standalone page** for beam
- `listenForBeam(sessionId, callback)` in `firebase-config.js` — listens at `beam_sessions/{sessionId}`
- PIN entry numpad (6-digit) in Beam Station page
- `getSnippetByPin(pin)` function — looks up `beam_sessions/{pin}`
- Session log for connection events
- "Simulate Connect" button for testing

### What the Web is MISSING or BROKEN ⚠️
| Gap | Description |
|---|---|
| ❌ No actual webcam / jsQR integration | There is a QR image display but NO camera feed to scan QR from the phone screen |
| ❌ `beam_receiver.html` is isolated | It's a separate HTML file, not integrated into the main dashboard |
| ⚠️ GZIP/Base64 decompression not implemented | If the mobile app compresses the beam payload, the web won't decompress it |
| ⚠️ The Beam page shows a QR to scan but it's a **generated** QR, not a receiver | Confusion: Web should be the RECEIVER, not the generator |

### What to Fix
```
1. Integrate html5-qrcode or jsQR webcam scanner into the Beam Station page
2. Add the "Activate Webcam" button + camera preview with green overlay
3. Add pako.js for GZIP decompression
4. Verify with classmate: does the mobile QR use GZIP or plain text?
```

---

## Phase 3: The AI Refinement Engine

### What the Mobile App Has ✅
- AI chat via OpenRouter API (`BuildConfig.OPENROUTER_API_KEY`)
- `SnipAiFragment.java` — AI chat interface
- Context-aware: sends recent snippets to AI for context

### What the Web ALREADY Has ✅
- Full Snip-AI chat page with message bubbles
- AI history saved to Firebase at `users/{uid}/aiChat`
- `getChatHistory()` and `saveChatMessage()` functions
- Quick-chips: "Explain in Tagalog", "To Kotlin", "Find Bug"
- `api-client.js` — likely handles the AI API call

### What the Web is MISSING or BROKEN ⚠️
| Gap | Description |
|---|---|
| ⚠️ Need to verify AI is actually calling OpenRouter/Gemini | Check `api-client.js` for the actual API endpoint |
| ❌ No "Refine with AI" button in snippet editor | The AI chat is separate — there's no direct "clean this code" button on the snippet |
| ❌ No syntax highlighting in the snippet code preview | Code shows as plain text in cards |

### What to Fix
```
1. Add "✨ Refine with AI" button to the New Snip modal
2. Integrate Prism.js or Highlight.js for code blocks
3. Verify api-client.js is hitting the right AI endpoint
```

---

## Phase 4: The "Dev-Dex" (IT Intelligence Hub)

### What the Mobile App Has ✅
- `DexFragment.java` — full Dev-Dex implementation
- `handbook.json` in assets — Git, Linux, HTTP codes, Ports, Regex
- Real-time search filter
- IP Address tracker (Local + Public)

### What the Web ALREADY Has ⚠️
- `Dev-Dex` nav item exists in sidebar
- BUT **there is no `page-devdex` div** in the HTML — the page does not exist yet

### What is COMPLETELY MISSING ❌
| Missing Feature | Notes |
|---|---|
| ❌ Dev-Dex page HTML | Needs to be created from scratch |
| ❌ handbook.json for the web | Need to get/copy from the mobile app's `assets/handbook.json` |
| ❌ Real-time search filter | Needs JS filter logic |
| ❌ Tabs: Git / Linux / HTTP / Ports | Needs tab component |
| ❌ IP Address display | Show local/public IP (already shown in sidebar but not on a dedicated page) |

### What to Build
```
1. Create page-devdex div in index.html
2. Copy handbook.json from the app's assets folder
3. Build a fetch() call to load and display handbook.json
4. Add instant filter search
5. Add tab buttons for categories
```

---

## Phase 5: UI/UX & "Dark-Kernel" Aesthetic

### What the Web ALREADY Has ✅
- Full Dark-Kernel theme: `#07080d` background, `#00ffaa` green accent
- `JetBrains Mono` font for code
- `Syne` font for UI text
- Dot grid background pattern
- Responsive sidebar with animated elements
- Light mode toggle exists

### What is INCONSISTENT ⚠️
| Issue | Description |
|---|---|
| ⚠️ Emojis used in nav items | `📁 Vault`, `📡 Beam`, `👤 Profile` — inconsistent with professional/terminal aesthetic |
| ⚠️ Some hardcoded values | `192.168.1.42` IP is hardcoded, not dynamic |
| ⚠️ Nav has no Dev-Dex icon/page | The "Dev-Dex" nav item leads nowhere |
| ⚠️ The login page is functional but plain | Could match the terminal aesthetic better |
| ✅ CSS variables well organized | Easy to maintain |

---

## Phase 6: Final 1:1 Synchronization Audit

| Feature | Mobile App (Classmate) | Web (You) | Status |
|---|---|---|---|
| **Authentication** | Firebase Auth (Email, Google, GitHub) | Firebase Auth (Email only) | ⚠️ Partial |
| **Snippets Database** | Firebase RTDB `users/{uid}/snippets` | Firebase RTDB `users/{uid}/snippets` | ✅ Same path |
| **Real-time Sync** | `onValue()` listener | ❌ Uses `get()` one-time fetch | ❌ Not real-time yet |
| **QR Beam Generate** | ZXing library in app | N/A (Web is receiver) | ✅ Correct split |
| **QR Beam Receive** | ❌ Not applicable | ❌ No webcam scanner | ❌ Missing |
| **AI Chat** | OpenRouter via API | api-client.js (need to verify) | ⚠️ Check needed |
| **Dev-Dex Handbook** | handbook.json in assets | ❌ No page built yet | ❌ Missing |
| **Tags** | Chip-group in fragments | Filter chips in Vault | ✅ Both have it |
| **Search** | Action bar search | Vault search input | ⚠️ Vault only, no global |
| **Syntax Highlighting** | Built-in viewer | ❌ Not integrated | ❌ Missing |
| **XP / Badges** | None on mobile side | Dev XP page exists | ✅ Web extra feature |
| **Profile** | EditProfileActivity | Profile page exists | ✅ Both have it |

---

## 🔴 Priority Fix List (Do These First)

### Critical (Breaks the Demo)
1. **Real-time sync** → Replace `get()` with `onValue()` in vault loading
2. **Dev-Dex page** → Create the page and load `handbook.json`
3. **Webcam QR scanner** → Add `jsQR` or `html5-qrcode` to Beam Station

### Important (Makes it Impressive)
4. **Syntax highlighting** → Add Prism.js to code blocks in vault cards
5. **"Refine with AI" button** → Add to snippet editor modal
6. **Ctrl+K global search** → Filter across all snippets by title/tag/language

### Polish (Nice to Have)
7. Replace emoji nav icons with SVG icons
8. Make Local IP dynamic (use WebRTC or fetch from an API)
9. Match login page to Dark-Kernel aesthetic

---

## ❓ Must Clarify With Classmate Before Building Beam

| Question | Why It Matters |
|---|---|
| Does the mobile QR contain **plain text** or **GZIP+Base64**? | Determines if you need pako.js decompression |
| What exact **fields** does a snippet object have? | Must match exactly: `title`, `code`, `language`, `tags`? |
| Is the beam session ID the **user's UID** or a **random PIN**? | Determines how the web listens for incoming beams |
