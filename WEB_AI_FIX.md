# SnipIT Web AI Fix - Now Works Like the App! 🤖

## Problem
The **web version** wasn't functioning well with AI because it was trying to use a local Python server (`http://localhost:8000`) with only dummy responses. The **Android app** works perfectly because it uses the **real GitHub Models API**.

## Solution
Updated the web version to use the **same GitHub Models API** that the Android app uses, providing real AI responses.

---

## What Changed

### 1. **api-client.js** - Now uses GitHub Models API
- Replaced local server calls with direct GitHub Models API (Azure hosted)
- Validates GitHub PAT tokens
- Provides meaningful error messages
- Falls back gracefully if no token is configured

### 2. **app.js** - Added AI Token Management
- Added `ai-settings` modal for token configuration
- Added `saveAiSettings()` function to validate and save tokens
- Added `clearAiToken()` function to remove tokens
- Added `updateAiStatusDisplay()` to show AI status in Settings
- Settings page now shows AI configuration status

### 3. **index.html** - Added AI Settings UI
- New AI Settings card in the Settings page
- "Configure AI Token" button
- Status display showing whether AI is online/offline

---

## How to Enable AI on Web

### Step 1: Create a GitHub Personal Access Token
1. Go to: https://github.com/settings/tokens/new
2. **Scopes needed**: Check **`models`** (under "AI Model Inferences")
3. Click "Generate token"
4. **Copy the token** (starts with `ghp_`)

### Step 2: Add Token to SnipIT Web
1. Open SnipIT Web Portal
2. Go to **Settings** (⚙ icon)
3. Click **"Configure AI Token"**
4. Paste your GitHub token
5. Click **"✓ Save Token"**

### Step 3: Use AI Features
- AI should now show "✓ Configured (Ready)" in Settings
- Use AI features throughout the app for real responses

---

## Technical Details

### API Endpoint
- **Service**: Azure-hosted GitHub Models API
- **URL**: `https://models.inference.ai.azure.com/chat/completions`
- **Model**: `gpt-4o` (GPT-4 Omni)
- **Authentication**: Bearer token (GitHub PAT)

### Token Storage
- Tokens are stored in browser's `localStorage` under key `github_models_token`
- Tokens are **private to your browser** (not sent to any server)
- You can clear tokens anytime from Settings

### Error Handling
- **401 Unauthorized**: Token is invalid or doesn't have "models" scope
- **Network Error**: Check your internet connection
- **Empty Response**: Try again, service might be temporarily unavailable

---

## Security Notes
✅ **Tokens stay in your browser** - never sent elsewhere
✅ **Use a separate token** - don't use your main GitHub token
✅ **Can revoke anytime** - go to https://github.com/settings/tokens to manage
✅ **Local storage only** - if you clear browser data, token is removed

---

## Comparison: Web vs Android

| Feature | Android App | Web Portal |
|---------|------------|-----------|
| AI Engine | GitHub Models API | GitHub Models API ✓ |
| Token Storage | `local.properties` | Browser localStorage ✓ |
| Real Responses | ✓ Yes | ✓ Yes (now!) |
| Configuration | At build time | In Settings ✓ |

---

## Troubleshooting

**Q: AI still says "offline"**
- Make sure you saved a valid GitHub token in Settings
- Check that your token has the "models" scope

**Q: Getting "Invalid GitHub token" error**
- Regenerate your token at https://github.com/settings/tokens
- Make sure "models" scope is checked
- Verify you copied the entire token

**Q: No internet connection**
- Check your WiFi/internet
- The GitHub Models API requires internet access

---

## Files Modified
- `web/js/api-client.js` - API client rewrite
- `web/js/app.js` - Added AI settings handlers
- `web/index.html` - Added AI Settings UI section

## Backward Compatibility
The local server (`server.py`) still works but won't provide real AI responses. All web AI now goes through GitHub Models API.
