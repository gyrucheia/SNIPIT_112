# SnipIT Web Portal

A modern web interface for the SnipIT code snippet management application. Built with vanilla HTML/CSS/JavaScript and integrated with FastAPI backend.

## Features

- **Dashboard**: Overview of your snippets, XP, and activity
- **Snippet Vault**: Store and manage code snippets across multiple languages
- **Beam Station**: Real-time code transfer from mobile to PC
- **AI Agent**: Chat-based code assistant for refactoring and debugging
- **Dev-XP System**: Gamification with badges and streaks
- **Profile Management**: User account and device management

## Getting Started

### Prerequisites

- Python 3.8+
- FastAPI and Uvicorn (installed via requirements)
- Modern web browser (Chrome, Firefox, Safari, Edge)

### Installation

1. **Start the backend server**:
```bash
cd c:\Users\Chuchay\StudioProjects\SnipIT_1
python server.py
```

The server will start on `http://localhost:8000`

2. **Open the web portal**:
   - Navigate to `http://localhost:8000` in your browser
   - Or directly access `http://localhost:8000/index.html`

### Project Structure

```
web/
├── index.html           # Main portal HTML
├── js/
│   ├── api-client.js   # FastAPI communication layer
│   └── app.js          # Core application logic
├── assets/             # Future: images, icons
└── data/               # Future: local data cache
```

## Features

### 1. **Dashboard**
- Quick stats on snippet count, languages, XP, and beams
- Recent snippets preview
- AI chat history

### 2. **Snippet Vault**
- Create new snippets with title, language, code, and tags
- Search and filter by language
- Copy to clipboard
- Beam to phone
- Send to AI for analysis

### 3. **Beam Station**
- QR code scanning for mobile connection
- PIN-based authentication (demo PIN: 482951)
- Real-time code transfer display
- Session logging
- VS Code integration ready

### 4. **Snip-AI Chat**
- Interactive chat interface
- Code analysis and refactoring
- Fallback to local AI responses when offline
- Chat history tracking

### 5. **Local Storage**
- Snippets stored in browser's localStorage
- Export vault as JSON
- Offline functionality support

## API Integration

The portal communicates with the FastAPI backend at:
- Base URL: `http://localhost:8000`
- Endpoint: `/analyze` (POST)

### Request Format
```json
{
  "code": "your code or prompt here",
  "language": "java|kotlin|python|prompt|system"
}
```

### Response Format
```json
{
  "fixed_code": "processed code",
  "tags": "#tag1,#tag2",
  "explanation": "explanation text"
}
```

## Development

### Adding New Features

1. **New Pages**: Edit `index.html` and add a new `<div class="page">`
2. **API Calls**: Use the `api` global object (initialized in `api-client.js`)
3. **Storage**: Use `LocalStorage` object methods in `app.js`

### Example API Call
```javascript
try {
  const result = await api.analyzeCode(code, 'java');
  console.log(result.explanation);
} catch (err) {
  console.error('API error:', err);
}
```

### Example Storage Usage
```javascript
// Add snippet
LocalStorage.addSnippet({
  title: "My Snippet",
  language: "Kotlin",
  tags: "#kotlin,#android",
  code: "val message = 'Hello'"
});

// Get all snippets
const snippets = LocalStorage.getSnippets();

// Export vault
LocalStorage.exportVault();
```

## Browser Support

- Chrome/Chromium 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Keyboard Shortcuts

- `Enter` in AI textarea: Send message (Shift+Enter for newline)
- `Ctrl/Cmd + /`: Search (future feature)

## Troubleshooting

### Server Won't Start
```bash
# Check if port 8000 is in use
netstat -ano | findstr :8000

# Kill process on Windows
taskkill /PID <PID> /F
```

### Web Portal Won't Load
1. Check if server is running: `http://localhost:8000/api/health`
2. Check browser console (F12) for errors
3. Clear browser cache: `Ctrl+Shift+Delete`

### API Not Responding
1. Ensure `server.py` is running
2. Check terminal for error messages
3. Verify CORS headers are enabled
4. Check firewall settings

## Future Enhancements

- [ ] Firebase Realtime DB integration
- [ ] OCR (Optical Character Recognition) for screenshot snippets
- [ ] Advanced search with regex support
- [ ] Snippet sharing/collaboration
- [ ] Dark/Light theme switcher
- [ ] Snippet versioning and history
- [ ] Integration with VS Code extension
- [ ] Mobile app responsive design

## Performance Notes

- Portal is optimized for modern browsers
- Snippets stored in localStorage (typically 5-10MB limit)
- Recommend exporting vault regularly for backup

## License

SnipIT (c) 2026 - Educational Project

## Support

For issues or feature requests, please check the main SnipIT repository documentation.

---

**Version**: 2.0.0  
**Last Updated**: May 2026  
**Status**: Active Development
