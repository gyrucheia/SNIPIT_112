# SnipIT Web Portal - Quick Start Guide

## 1. Start the Server

Open PowerShell/Terminal in the SnipIT_1 directory:

```powershell
python server.py
```

You should see:
```
🚀 Starting SnipIT Server...
📡 API available at: http://localhost:8000/analyze
🌐 Web Portal at: http://localhost:8000
INFO:     Uvicorn running on http://0.0.0.0:8000
```

## 2. Open the Web Portal

Open your browser and go to:
```
http://localhost:8000
```

## 3. Features You Can Try

### Dashboard
- See snippet statistics
- View recent activity

### Snippet Vault
- Click "+ New Snip" to add a code snippet
- Snippets are saved in browser storage
- Search by language

### Beam Station
- Click "Simulate Connect" to demo the connection
- Enter PIN: **482951** to pair device
- See real-time code transfer

### Snip-AI Chat
- Ask questions about code
- Get AI-powered suggestions
- Responses come from backend API (or local fallback)

### Dev-XP
- Track your coding achievements
- Earn badges and XP

## 4. Local Storage

All snippets are stored locally in your browser:
- They persist between sessions
- No server upload needed for basic functionality
- Click "Export" in settings to backup

## 5. API Integration

The web portal automatically communicates with `server.py`:

- **Code Analysis**: Send code to `/analyze` endpoint
- **AI Responses**: Get AI-powered refactoring suggestions
- **Fallback Mode**: Works offline with local responses

### Test the API

```bash
# Send a test request
curl -X POST http://localhost:8000/analyze -H "Content-Type: application/json" -d "{\"code\": \"ping\", \"language\": \"system\"}"
```

## 6. File Structure

```
SnipIT_1/
├── server.py              # FastAPI backend
└── web/
    ├── index.html         # Main portal
    ├── README.md          # Full documentation
    ├── QUICKSTART.md      # This file
    └── js/
        ├── api-client.js  # API communication
        └── app.js         # Application logic
```

## 7. Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| "Cannot GET /" | Server not running. Run `python server.py` |
| Snippets not saving | Browser blocking localStorage. Check privacy settings |
| API calls failing | Server offline. Check terminal for errors |
| Port 8000 in use | Change port in `server.py`: `port=8001` |

## 8. Next Steps

- ✅ Server running
- ✅ Web portal accessible
- 🔄 Try adding snippets
- 🔄 Test Beam Station connection
- 🔄 Ask AI questions
- 📱 Connect mobile SnipIT app (when ready)

## Tips

- **Local Development**: Portal works entirely in browser for snippet management
- **Backup**: Export vault regularly from settings
- **Performance**: Snippets in localStorage, no server uploads needed
- **Offline**: Most features work without internet connection

---

**Need help?** Check `README.md` for detailed documentation.
