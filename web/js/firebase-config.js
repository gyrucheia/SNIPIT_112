// SnipIT Web Portal - Firebase Configuration & Integration

// Firebase Configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyAa-l7kxBmUaKie0pBMolEVeiZ52AsT6N0",
  authDomain: "snipit-d07da.firebaseapp.com",
  databaseURL: "https://snipit-d07da-default-rtdb.firebaseio.com",
  projectId: "snipit-d07da",
  storageBucket: "snipit-d07da.firebasestorage.app",
  messagingSenderId: "494406383763",
  appId: "1:494406383763:web:0e87527ab2926f5720b566",
  measurementId: "G-2CRE4XWG8P"
};

// Initialize Firebase
let db, auth, user = null;
let firebaseAPI = null;
let authFunctions = {};

async function initializeFirebase() {
  try {
    console.log('🔥 Starting Firebase initialization...');

    // Import Firebase modules
    console.log('📦 Importing Firebase modules from CDN...');
    const firebaseApp = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-app.js');
    const firebaseDB = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
    const firebaseAuth = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-auth.js');

    console.log('✅ Firebase modules imported successfully');

    // Initialize Firebase
    console.log('⚙️  Initializing Firebase app with config...');
    const app = firebaseApp.initializeApp(firebaseConfig);
    db = firebaseDB.getDatabase(app);
    auth = firebaseAuth.getAuth(app);

    // Store auth functions for later use
    authFunctions = {
      signInWithEmailAndPassword: firebaseAuth.signInWithEmailAndPassword,
      createUserWithEmailAndPassword: firebaseAuth.createUserWithEmailAndPassword,
      updateProfile: firebaseAuth.updateProfile,
      signOut: firebaseAuth.signOut
    };

    console.log('✅ Firebase app initialized');
    console.log('✅ Database connected:', !!db);
    console.log('✅ Auth initialized:', !!auth);

    // Initialize Firebase API FIRST before listening for auth
    firebaseAPI = new SnipITFirebase();

    // Check authentication state
    firebaseAuth.onAuthStateChanged(auth, (currentUser) => {
      user = currentUser;
      if (user) {
        console.log('✅ Firebase user authenticated:', user.uid);
        if (typeof hideLoginPage === 'function') hideLoginPage();
        try {
          if (typeof updateUserCard === 'function') updateUserCard();
        } catch(e) {
          console.warn('updateUserCard error (non-fatal):', e.message);
        }
        if (typeof syncVault === 'function') syncVault();
      } else {
        console.log('ℹ️  No user logged in');
      }
    });

    // Refresh UI if user is already logged in
    if (user && typeof updateUserCard === 'function') {
      updateUserCard();
    }

    console.log('🎉 Firebase fully initialized successfully');
    return true;
  } catch (err) {
    console.error('❌ Firebase initialization error:', err);
    console.error('📍 Error message:', err.message);
    console.error('📍 Error code:', err.code);

    // Show error but don't block UI - allow demo mode
    if (typeof showAuthError !== 'undefined') {
      showAuthError('Firebase offline - using demo mode');
    }
    return false;
  }
}

// Firebase Authentication Functions
async function firebaseSignInWithEmail(email, password) {
  try {
    if (!auth) {
      console.error('Auth not initialized');
      return { success: false, error: 'Firebase not initialized' };
    }

    console.log('Attempting sign in with:', email);
    const userCredential = await authFunctions.signInWithEmailAndPassword(auth, email, password);
    console.log('Signed in successfully:', userCredential.user.uid);
    return { success: true, user: userCredential.user };
  } catch (err) {
    console.error('Login error:', err);
    return { success: false, error: err.message };
  }
}

async function firebaseSignUpWithEmail(name, email, password) {
  try {
    if (!auth) {
      console.error('Auth not initialized');
      return { success: false, error: 'Firebase not initialized' };
    }

    console.log('Attempting sign up with:', email);
    const userCredential = await authFunctions.createUserWithEmailAndPassword(auth, email, password);

    // Update user profile with name
    await authFunctions.updateProfile(userCredential.user, { displayName: name });

    console.log('Account created successfully:', userCredential.user.uid);
    return { success: true, user: userCredential.user };
  } catch (err) {
    console.error('Signup error:', err);
    return { success: false, error: err.message };
  }
}

async function firebaseSignOut() {
  try {
    if (!auth) {
      console.error('Auth not initialized');
      return;
    }

    await authFunctions.signOut(auth);
    console.log('Signed out');
    showLoginPage();
  } catch (err) {
    console.error('Sign out error:', err);
  }
}

// SnipIT Firestore API
class SnipITFirebase {
  constructor() {
    this.isOnline = false;
    this.checkConnection();
  }

  async checkConnection() {
    try {
      if (db && user) {
        this.isOnline = true;
        console.log('Firebase connection: ONLINE');
        return true;
      }
    } catch (err) {
      console.warn('Firebase offline:', err.message);
      this.isOnline = false;
    }
    return false;
  }

  // Get all snippets for current user
  async getSnippets() {
    try {
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const snippetsRef = ref(db, `users/${user.uid}/snippets`);
      const snapshot = await get(snippetsRef);

      if (snapshot.exists()) {
        return Object.values(snapshot.val());
      }
      return [];
    } catch (err) {
      console.error('Error fetching snippets:', err);
      return [];
    }
  }
  listenForSnippets(callback) {
    import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js')
      .then(({ ref, onValue }) => {
        const snippetsRef = ref(db, `users/${user.uid}/snippets`);

        onValue(snippetsRef, (snapshot) => {
          if (snapshot.exists()) {
            // Use Object.entries to preserve Firebase push keys as snippet.id
            // Critical: without this, deleteSnippet uses wrong ID and fails
            const snippets = Object.entries(snapshot.val()).map(([firebaseKey, value]) => ({
              ...value,
              id: firebaseKey
            }));
            callback(snippets);
          } else {
            callback([]);
          }
        });
      });
  }

  // Add new snippet
  async addSnippet(snippet) {
    try {
      const { ref, push, set } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');

      const newSnippetRef = push(ref(db, `users/${user.uid}/snippets`));
      await set(newSnippetRef, {
        ...snippet,
        id: newSnippetRef.key,
        created: new Date().toISOString(),
        updated: new Date().toISOString()
      });

      console.log('Snippet saved:', newSnippetRef.key);
      return newSnippetRef.key;
    } catch (err) {
      console.error('Error adding snippet:', err);
      throw err;
    }
  }

  // Update snippet
  async updateSnippet(id, snippet) {
    try {
      const { ref, set } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');

      await set(ref(db, `users/${user.uid}/snippets/${id}`), {
        ...snippet,
        id,
        updated: new Date().toISOString()
      });

      console.log('Snippet updated:', id);
    } catch (err) {
      console.error('Error updating snippet:', err);
      throw err;
    }
  }

  // Delete snippet
  async deleteSnippet(id) {
    try {
      const { ref, remove } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');

      await remove(ref(db, `users/${user.uid}/snippets/${id}`));
      console.log('Snippet deleted:', id);
    } catch (err) {
      console.error('Error deleting snippet:', err);
      throw err;
    }
  }

  // Save beam connection log
  async logBeamSession(deviceInfo) {
    try {
      const { ref, push, set } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');

      const logRef = push(ref(db, `users/${user.uid}/beamSessions`));
      await set(logRef, {
        deviceInfo,
        timestamp: new Date().toISOString(),
        snippetsSent: 0
      });

      return logRef.key;
    } catch (err) {
      console.error('Error logging beam session:', err);
    }
  }

  // Update user stats (XP, level, etc.)
  async updateStats(stats) {
    try {
      const { ref, update } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await update(ref(db, `users/${user.uid}/stats`), stats);
      console.log('Stats updated in Firebase');
    } catch (err) {
      console.error('Error updating stats:', err);
    }
  }

  // Get user stats
  async getStats() {
    try {
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const snapshot = await get(ref(db, `users/${user.uid}/stats`));
      return snapshot.exists() ? snapshot.val() : null;
    } catch (err) {
      console.error('Error getting stats:', err);
      return null;
    }
  }

  // Update user profile
  async updateProfile(profileData) {
    try {
      const { ref, update } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await update(ref(db, `users/${user.uid}/profile`), profileData);
      console.log('Profile updated in Firebase');
    } catch (err) {
      console.error('Error updating profile:', err);
    }
  }

  // Get user profile
  async getProfile() {
    try {
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const snapshot = await get(ref(db, `users/${user.uid}/profile`));
      return snapshot.exists() ? snapshot.val() : null;
    } catch (err) {
      console.error('Error getting profile:', err);
      return null;
    }
  }

  // Save AI chat message (Universal format for App & Web sync)
  async saveChatMessage(chatId, role, content, title = null) {
    try {
      if (!user || !user.uid) return null;

      const { ref, get, set } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const chatRef = ref(db, `users/${user.uid}/aiChat/${chatId}`);
      const snapshot = await get(chatRef);

      let chatData = snapshot.exists() ? snapshot.val() : { 
        messages: [], 
        title: title || content.substring(0, 40),
        createdAt: new Date().toISOString()
      };

      if (!chatData.messages) chatData.messages = [];
      
      chatData.messages.push({ 
        role: role === 'ai' ? 'assistant' : role, 
        body: content, 
        timestamp: new Date().toISOString() 
      });
      
      chatData.lastUpdated = new Date().toISOString();

      await set(chatRef, chatData);
      return chatId;
    } catch (err) {
      console.error('❌ Error saving chat:', err);
      throw err;
    }
  }

  // Compatibility method for old web calls
  async saveChatPair(chatId, userMsg, aiMsg) {
    await this.saveChatMessage(chatId, 'user', userMsg);
    return await this.saveChatMessage(chatId, 'assistant', aiMsg);
  }

  // Get chat history
  async getChatHistory() {
    try {
      if (!user || !user.uid) {
        console.warn('⚠️ User not authenticated, returning empty history');
        return {};
      }

      console.log('📚 Fetching chat history from Firebase...');
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const historyRef = ref(db, `users/${user.uid}/aiChat`);
      const snapshot = await get(historyRef);

      if (snapshot.exists()) {
        const history = snapshot.val();
        console.log('✅ Chat history retrieved. Total chats:', Object.keys(history).length);
        return history;
      } else {
        console.log('ℹ️ No chat history exists yet');
        return {};
      }
    } catch (err) {
      console.error('❌ Error fetching chat history:', err);
      return {};
    }
  }

  // Clear chat history
  async clearChatHistory() {
    try {
      const { ref, remove } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await remove(ref(db, `users/${user.uid}/aiChat`));
    } catch (err) {
      console.error('Error clearing chat history:', err);
    }
  }

  // Send a beam payload to a specific session
  async sendBeam(sessionId, data) {
    try {
      const { ref, set } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const sessionRef = ref(db, `beam_sessions/${sessionId}`);
      await set(sessionRef, data);
      console.log('✅ Beam sent to session:', sessionId);
    } catch (err) {
      console.error('❌ Error sending beam:', err);
      throw err;
    }
  }

  // Listen for incoming beam payloads for a specific session ID (e.g. UID)
  async listenForBeam(sessionId, callback) {
    try {
      const { ref, onValue } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      // Use 'beam_sessions' to match the Android app
      const sessionRef = ref(db, `beam_sessions/${sessionId}`);
      onValue(sessionRef, (snapshot) => {
        if (snapshot.exists()) {
          callback(snapshot.val());
        }
      });
    } catch (err) {
      console.error('Error listening for beam:', err);
    }
  }

  // Clear all chat history
  async clearChatHistory() {
    try {
      if (!user || !user.uid) return;
      const { ref, remove } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await remove(ref(db, `users/${user.uid}/aiChat`));
      console.log('✅ Chat history cleared in Firebase');
    } catch (err) {
      console.error('❌ Error clearing chat history:', err);
    }
  }

  // Clear a beam session
  async clearBeam(sessionId) {
    try {
      const { ref, remove } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await remove(ref(db, `beam_sessions/${sessionId}`));
      console.log('✅ Beam cleared in Firebase');
    } catch (err) {
      console.error('❌ Error clearing beam:', err);
    }
  }

  // Listen for Cloud-Relay Active Beam (Phase 2)
  async listenForActiveBeam(callback) {
    try {
      if (!user) return;
      const { ref, onValue } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const beamRef = ref(db, `users/${user.uid}/active_beam`);
      onValue(beamRef, (snapshot) => {
        if (snapshot.exists()) {
          callback(snapshot.val());
        }
      });
    } catch (err) {
      console.error('Error listening for active beam:', err);
    }
  }

  // Clear Cloud-Relay Active Beam
  async clearActiveBeam() {
    try {
      if (!user) return;
      const { ref, remove } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      await remove(ref(db, `users/${user.uid}/active_beam`));
      console.log('✅ Active beam cleared');
    } catch (err) {
      console.error('❌ Error clearing active beam:', err);
    }
  }

  // Get Security PIN for Vault (Phase 1)
  async getSecurityPin() {
    try {
      if (!user) return null;
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const snapshot = await get(ref(db, `users/${user.uid}/profile/pin`));
      return snapshot.exists() ? snapshot.val() : null;
    } catch (err) {
      console.error('Error fetching security PIN:', err);
      return null;
    }
  }

  // Look up a snippet by its 6-digit PIN
  async getSnippetByPin(pin) {
    try {
      const { ref, get } = await import('https://www.gstatic.com/firebasejs/10.5.0/firebase-database.js');
      const pinRef = ref(db, `beam_sessions/${pin}`);
      const snapshot = await get(pinRef);
      if (snapshot.exists()) {
        return snapshot.val();
      }
      return null;
    } catch (err) {
      console.error('Error fetching by PIN:', err);
      return null;
    }
  }

  // Listen for real-time snippet updates

  // Export user vault to local file
  async exportVault() {
    try {
      const snippets = await this.getSnippets();
      const json = JSON.stringify(snippets, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `snipit_vault_${new Date().toISOString().split('T')[0]}.json`;
      a.click();
      console.log('Vault exported');
    } catch (err) {
      console.error('Error exporting vault:', err);
    }
  }

  // Import vault from local file
  async importVault(file) {
    try {
      const text = await file.text();
      const snippets = JSON.parse(text);

      for (const snippet of snippets) {
        await this.addSnippet(snippet);
      }

      console.log(`Imported ${snippets.length} snippets`);
      return snippets.length;
    } catch (err) {
      console.error('Error importing vault:', err);
      throw err;
    }
  }
}

// Global Firebase instance will be initialized in initializeFirebase()

function updateServerStatus(isOnline) {
  const dot = document.getElementById('beam-dot');
  const statusText = document.getElementById('beam-status-txt');

  if (isOnline) {
    dot?.classList.remove('offline');
    dot?.classList.add('online');
    statusText.textContent = 'Firebase online';
  } else {
    dot?.classList.remove('online');
    dot?.classList.add('offline');
    statusText.textContent = 'Local mode (offline)';
  }
}

// Fallback to local storage if Firebase unavailable
async function loadVaultFromFirebase() {
  try {
    if (firebaseAPI.isOnline && user) {
      const snippets = await firebaseAPI.getSnippets();
      LocalStorage.setSnippets(snippets);
      loadVault();
    }
  } catch (err) {
    console.error('Error loading from Firebase:', err);
  }
}
