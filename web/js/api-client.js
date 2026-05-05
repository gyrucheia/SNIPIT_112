// SnipIT Web Portal - API Client
// Uses GitHub Models API for real AI (same as Android app)

class SnipITAPI {
  constructor() {
    this.isOnline = false;
    this.githubToken = localStorage.getItem('github_models_token') || '';
    this.apiKey = this.githubToken;
    this.serverUrl = this.getServerUrl();
    this.tokenLoaded = false;
  }

  getServerUrl() {
    // Always use localhost:8000 for API (Python server)
    // The web portal might be on a different server (Live Server, etc)
    const protocol = window.location.protocol;
    return `${protocol}//localhost:8000`;
  }

  async loadTokenFromServer() {
    if (this.tokenLoaded) return;
    
    try {
      console.log('🔄 Attempting to load token from server:', this.serverUrl);
      const response = await fetch(`${this.serverUrl}/api/config`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });
      
      if (response.ok) {
        const config = await response.json();
        console.log('✓ Config response:', config);
        if (config.github_token) {
          this.apiKey = config.github_token;
          this.githubToken = config.github_token;
          localStorage.setItem('github_models_token', config.github_token);
          this.isOnline = true;
          console.log('✓ Token loaded! Starts with:', config.github_token.substring(0, 20) + '...');
        }
      } else {
        console.warn('Server returned status:', response.status);
      }
    } catch (err) {
      console.warn('⚠️ Server not reachable:', err.message);
      this.isOnline = !!this.apiKey;
    }
    
    this.tokenLoaded = true;
  }

  async checkConnection() {
    // Ensure token is loaded from server first
    await this.loadTokenFromServer();
    this.isOnline = !!this.apiKey;
    console.log('✓ Connection check - Token available:', !!this.apiKey);
    return this.isOnline;
  }

  setGitHubToken(token) {
    this.apiKey = token;
    this.githubToken = token;
    localStorage.setItem('github_models_token', token);
    this.isOnline = !!token;
  }

  // GitHub Models API call (same as Android app)
  async getAIResponse(userMessage) {
    // Ensure token is loaded from server
    await this.loadTokenFromServer();
    
    if (!this.apiKey) {
      console.error('❌ No API key available after loading attempt');
      return {
        fixed_code: '',
        explanation: '❌ No GitHub token found. Make sure OPENROUTER_API_KEY is set in local.properties.'
      };
    }

    try {
      console.log('🤖 Sending AI request with token:', this.apiKey.substring(0, 20) + '...');
      
      const response = await fetch('https://models.inference.ai.azure.com/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`
        },
        body: JSON.stringify({
          model: 'gpt-4o-mini',
          messages: [
            {
              role: 'system',
              content: `You are Snip-AI, the dedicated assistant for the SnipIT app and web portal. Help users fix, explain, and optimize code snippets. Be concise.`
            },
            {
              role: 'user',
              content: userMessage
            }
          ],
          temperature: 0.7,
          top_p: 1,
          max_tokens: 1024
        })
      });

      console.log('📡 API Response status:', response.status);
      
      if (!response.ok) {
        const error = await response.json();
        console.error('❌ GitHub API error:', error);
        
        if (response.status === 401) {
          return {
            fixed_code: '',
            explanation: '❌ Token error (401). Your GitHub PAT might be invalid or expired.'
          };
        }
        
        return {
          fixed_code: '',
          explanation: `❌ API Error (${response.status}): ${error.error?.message || error.message || 'Unknown error'}`
        };
      }

      const data = await response.json();
      console.log('✓ Raw API response:', data);
      
      const content = data.choices?.[0]?.message?.content || '';
      console.log('📝 Extracted content:', content);
      
      // Parse code blocks from response
      const codeMatch = content.match(/```(?:[\w]*\n)?([\s\S]*?)```/);
      const fixedCode = codeMatch ? codeMatch[1].trim() : '';
      const explanation = content.replace(/```[\s\S]*?```/g, '').trim() || content;

      console.log('✓ Parsed code:', fixedCode);
      console.log('✓ Parsed explanation:', explanation);

      return {
        fixed_code: fixedCode,
        explanation: explanation || 'Code processed by SnipIT AI'
      };
    } catch (err) {
      console.error('⚠️ AI request error:', err);
      return {
        fixed_code: '',
        explanation: `⚠️ Error: ${err.message}`
      };
    }
  }

  // Legacy method for compatibility
  async analyzeCode(code, language = 'java') {
    const prompt = `Analyze and improve this ${language} code:\n\`\`\`\n${code}\n\`\`\``;
    return await this.getAIResponse(prompt);
  }

  // New method for context-aware conversations (like Gemini chat history)
  async getAIResponseWithContext(userMessage, conversationHistory = []) {
    // Ensure token is loaded from server
    await this.loadTokenFromServer();
    
    if (!this.apiKey) {
      console.error('❌ No API key available after loading attempt');
      return {
        fixed_code: '',
        explanation: '❌ No GitHub token found. Make sure OPENROUTER_API_KEY is set in local.properties.'
      };
    }

    try {
      console.log('🤖 Sending AI request with conversation context:', this.apiKey.substring(0, 20) + '...');
      
      // Build messages array with system prompt and conversation history
      const messages = [
        {
          role: 'system',
          content: `You are Snip-AI, the dedicated assistant for the SnipIT app and web portal. Help users fix, explain, and optimize code snippets. Be concise. Remember previous messages in this conversation for context.`
        }
      ];

      // Add conversation history to maintain context (like Gemini)
      if (conversationHistory && conversationHistory.length > 0) {
        conversationHistory.forEach(msg => {
          messages.push({
            role: 'user',
            content: msg.user
          });
          messages.push({
            role: 'assistant',
            content: msg.ai
          });
        });
      }

      console.log('📊 Full message history being sent:', messages.length, 'messages');

      // Add current user message
      messages.push({
        role: 'user',
        content: userMessage
      });

      const response = await fetch('https://models.inference.ai.azure.com/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`
        },
        body: JSON.stringify({
          model: 'gpt-4o-mini',
          messages: messages,
          temperature: 0.7,
          top_p: 1,
          max_tokens: 1024
        })
      });

      console.log('🔵 API Response received:', response.status);

      console.log('📡 API Response status:', response.status);
      
      if (!response.ok) {
        const error = await response.json();
        console.error('❌ GitHub API error:', error);
        
        if (response.status === 401) {
          return {
            fixed_code: '',
            explanation: '❌ Token error (401). Your GitHub PAT might be invalid or expired.'
          };
        }
        
        return {
          fixed_code: '',
          explanation: `❌ API Error (${response.status}): ${error.error?.message || error.message || 'Unknown error'}`
        };
      }

      const data = await response.json();
      console.log('✓ Raw API response:', data);
      
      const content = data.choices?.[0]?.message?.content || '';
      console.log('📝 Extracted content:', content);
      
      // Parse code blocks from response
      const codeMatch = content.match(/```(?:[\w]*\n)?([\s\S]*?)```/);
      const fixedCode = codeMatch ? codeMatch[1].trim() : '';
      const explanation = content.replace(/```[\s\S]*?```/g, '').trim() || content;

      console.log('✓ Parsed code:', fixedCode);
      console.log('✓ Parsed explanation:', explanation);

      return {
        fixed_code: fixedCode,
        explanation: explanation || 'Code processed by SnipIT AI'
      };
    } catch (err) {
      console.error('⚠️ AI request error:', err);
      return {
        fixed_code: '',
        explanation: `⚠️ Error: ${err.message}`
      };
    }
  }
}

// Initialize global API client
const api = new SnipITAPI();

// Check server status on load
window.addEventListener('load', async () => {
  const online = await api.checkConnection();
  updateServerStatus(online);
});

function updateServerStatus(isOnline) {
  const dot = document.getElementById('beam-dot');
  const statusText = document.getElementById('beam-status-txt');
  
  if (isOnline) {
    dot.classList.remove('offline');
    dot.classList.add('online');
    statusText.textContent = 'AI online';
  } else {
    dot.classList.remove('online');
    dot.classList.add('offline');
    statusText.textContent = 'AI offline (no token)';
  }
  
  // Also update settings display if it exists
  if (typeof updateAiStatusDisplay === 'function') {
    updateAiStatusDisplay();
  }
}

// Settings helper to manage GitHub token
function updateGitHubToken(token) {
  api.setGitHubToken(token);
  updateServerStatus(api.isOnline);
}

// Periodically refresh token from server (in case local.properties changed)
setInterval(async () => {
  api.tokenLoaded = false; // Force re-check
  await api.loadTokenFromServer();
  updateServerStatus(api.isOnline);
}, 5000); // Check every 5 seconds
