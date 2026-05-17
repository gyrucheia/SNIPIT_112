// SnipIT Web Portal - API Client
// GitHub Models API (same backend as Android app)

// OPENROUTER_API_KEY is now loaded from js/config.js (gitignored) or localStorage

class SnipITAPI {
  constructor() {
    this.apiKey = (typeof OPENROUTER_API_KEY !== 'undefined') ? OPENROUTER_API_KEY : '';
    this.isOnline = true;
    this.endpoint = 'https://models.inference.ai.azure.com/chat/completions';
    this.model = 'gpt-4o-mini';
  }

  async checkConnection() {
    this.isOnline = !!this.apiKey;
    return this.isOnline;
  }

  setGitHubToken(token) {
    this.apiKey = token || OPENROUTER_API_KEY;
    this.isOnline = !!this.apiKey;
  }

  // Core AI call — used by chat and refine
  async callAI(messages) {
    if (!this.apiKey) {
      return { fixed_code: '', explanation: '❌ No API key configured.' };
    }

    try {
      const response = await fetch(this.endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`
        },
        body: JSON.stringify({
          model: this.model,
          messages: messages,
          temperature: 0.7,
          max_tokens: 1024
        })
      });

      if (!response.ok) {
        const err = await response.json();
        const msg = err.error?.message || 'Unknown API error';
        if (response.status === 401) return { fixed_code: '', explanation: '❌ Token invalid or expired. Check your GitHub PAT.' };
        return { fixed_code: '', explanation: `❌ API Error (${response.status}): ${msg}` };
      }

      const data = await response.json();
      const content = data.choices?.[0]?.message?.content || '';

      // Extract code block if present
      const codeMatch = content.match(/```(?:[\w]*\n)?([\s\S]*?)```/);
      const fixedCode = codeMatch ? codeMatch[1].trim() : '';
      const explanation = content.replace(/```[\s\S]*?```/g, '').trim() || content;

      return { fixed_code: fixedCode, explanation };
    } catch (err) {
      return { fixed_code: '', explanation: `⚠️ Network error: ${err.message}` };
    }
  }

  // Simple one-shot AI response
  async getAIResponse(userMessage) {
    return this.callAI([
      {
        role: 'system',
        content: 'You are Snip-AI, the dedicated assistant for the SnipIT app. Help users fix, explain, and optimize code snippets. Be concise and practical.'
      },
      { role: 'user', content: userMessage }
    ]);
  }

  // Chat with conversation history
  async getAIResponseWithContext(userMessage, conversationHistory = []) {
    const messages = [
      {
        role: 'system',
        content: 'You are Snip-AI, the dedicated assistant for the SnipIT app. Help users fix, explain, and optimize code snippets. Be concise. Remember previous messages in this conversation.'
      }
    ];

    conversationHistory.forEach(msg => {
      if (msg.role && msg.body) {
        // New universal schema
        messages.push({ role: msg.role === 'user' ? 'user' : 'assistant', content: msg.body });
      } else if (msg.user || msg.ai) {
        // Legacy schema
        if (msg.user) messages.push({ role: 'user', content: msg.user });
        if (msg.ai) messages.push({ role: 'assistant', content: msg.ai });
      }
    });

    messages.push({ role: 'user', content: userMessage });
    return this.callAI(messages);
  }

  // Refine a snippet — called from vault cards
  async refineSnippet(code, language) {
    return this.getAIResponse(
      `Refine and clean this ${language} code snippet. Fix indentation, improve readability, and add a brief comment if helpful. Return the improved code in a code block.\n\`\`\`${language}\n${code}\n\`\`\``
    );
  }

  // Legacy compatibility
  async analyzeCode(code, language = 'java') {
    return this.getAIResponse(`Analyze and improve this ${language} code:\n\`\`\`\n${code}\n\`\`\``);
  }

  // Fetch token from local server (local.properties) - only on localhost
  async loadTokenFromServer() {
    // Safety: Only attempt to hit the local Python server if we are on localhost
    if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
      return false;
    }

    try {
      const response = await fetch('/api/config');
      if (response.ok) {
        const data = await response.json();
        if (data.github_token) {
          this.setGitHubToken(data.github_token);
          console.log('✓ AI Token auto-loaded from local.properties via server');
          return true;
        }
      }
    } catch (err) {
      // Silent fail on cloud
    }
    return false;
  }
}

// Initialize global API client
const api = new SnipITAPI();

// Update status dot once on load
window.addEventListener('load', () => {
  updateServerStatus(api.isOnline);
});

function updateServerStatus(isOnline) {
  const dot = document.getElementById('beam-dot');
  const statusText = document.getElementById('beam-status-txt');

  if (dot) {
    dot.classList.toggle('online', isOnline);
    dot.classList.toggle('offline', !isOnline);
  }
  if (statusText) statusText.textContent = isOnline ? 'AI online' : 'AI offline';

  if (typeof updateAiStatusDisplay === 'function') updateAiStatusDisplay();
}

function updateGitHubToken(token) {
  api.setGitHubToken(token);
  updateServerStatus(api.isOnline);
}
