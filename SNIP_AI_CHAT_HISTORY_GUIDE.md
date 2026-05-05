# SnipIT Snip-AI Chat History - Complete Guide

## Overview
The Snip-AI now has full **chat history archiving**. When you click "New Chat", your previous conversation is automatically saved and appears in the history sidebar.

## How It Works

### 1. **Sending a Message**
```
User types message → Click send or press Enter
→ sendAI() creates NEW chatId if needed (using timestamp)
→ Message saved to Firebase under users/{uid}/aiChat/{chatId}
→ History sidebar refreshes to show archived chats
→ User can continue chatting with AI
```

### 2. **Starting New Chat**
```
User clicks "+ New" button
→ startNewChat() sets currentChatId = null
→ Chat area clears with welcome message
→ loadAIHistory() refreshes sidebar
→ OLD chat is now visible in history with first message as title
```

### 3. **Loading Previous Chat**
```
User clicks on chat in history sidebar
→ loadChat(id) sets currentChatId = id
→ Fetches all messages for that chat from Firebase
→ Displays full conversation thread
→ Can continue chatting in this conversation
```

### 4. **Clearing History**
```
User clicks "Clear History" button
→ Confirmation prompt appears
→ firebaseAPI.clearChatHistory() deletes all chats
→ currentChatId = null
→ Fresh new chat starts
```

## Data Structure (Firebase)

Each chat is stored at: `users/{uid}/aiChat/{chatId}`

```json
{
  "123456789": {
    "title": "Explain this in Tagalog: ...",
    "messages": [
      {
        "user": "Explain this in Tagalog: my code",
        "ai": "Sure! Here's the explanation...",
        "timestamp": "2026-05-04T10:30:00.000Z"
      },
      {
        "user": "Can you convert it to Java?",
        "ai": "Absolutely! Here's the Java version...",
        "timestamp": "2026-05-04T10:31:15.000Z"
      }
    ],
    "lastUpdated": "2026-05-04T10:31:15.000Z"
  },
  "123456790": {
    "title": "Convert this Java to Kotlin: ...",
    "messages": [...]
  }
}
```

## Key Features

✅ **Auto-Save**: Every message is automatically saved to Firebase  
✅ **Persistent History**: Chats survive page refreshes, app restarts, device changes  
✅ **Smart Titles**: First message becomes the chat title (first 40 chars)  
✅ **Chronological**: History sorted newest-first  
✅ **Click to Load**: Click any chat to restore full conversation  
✅ **Full Threading**: All messages in a chat preserved  
✅ **Date Display**: Each chat shows when it was last updated  
✅ **One-Click Clear**: Delete all history at once  

## Files Modified

- `web/js/app.js` - Removed duplicate functions, kept clean implementation
- No changes needed to Firebase API (already has getChatHistory, saveChatMessage, clearChatHistory)
- No HTML changes needed (already has history sidebar structure)

## Testing the Feature

1. **Open Snip-AI page**
2. **Send first message**: "Hello, how are you?"
   - AI responds
   - Message appears in chat
3. **Send second message**: "Tell me more"
   - AI responds
   - Still in same chat
4. **Click "+ New" button**
   - Chat area clears
   - First chat appears in history sidebar with title "Hello, how are you?..."
5. **Click on chat in history**
   - Full conversation reloads
   - You can continue chatting in that conversation
6. **Click "+ New" again, send different message**
   - Second chat is now in history
7. **Toggle between chats**
   - Click different chats to switch
8. **Click "Clear History"**
   - All chats deleted
   - Fresh start

## What Gets Saved

- ✅ All user messages
- ✅ All AI responses  
- ✅ Timestamps for each message
- ✅ Chat title (auto-generated from first message)
- ✅ Last updated time
- ❌ NOT SAVED: Unsent draft messages

## Limitations & Notes

- Chat history is per-user (tied to Firebase UID)
- History available only after login
- Clear History deletes ALL chats permanently
- Max context for AI responses: Last 5 messages (for performance)
- Chat IDs are based on timestamps (ensuring uniqueness)

## Future Enhancements Possible

- Export chat as PDF
- Share specific conversations
- Search through history
- Pin favorite chats
- Organize chats into folders
- Star important conversations
