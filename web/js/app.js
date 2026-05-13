// SnipIT Web Portal - Main Application Logic

// ========== AUTHENTICATION ==========
// 'user' is already declared in firebase-config.js
const authKey = 'snipit_auth_user';

// Check if user is logged in
function checkAuth() {
  if (user) {
    hideLoginPage();
    updateUserCard();
    return true;
  }
  showLoginPage();
  return false;
}

// Show login page
function showLoginPage() {
  const loginPage = document.getElementById('login-page');
  const main = document.querySelector('.main');
  const sidebar = document.querySelector('.sidebar');

  if (loginPage) {
    loginPage.classList.add('show');
    loginPage.style.display = 'flex';
    loginPage.style.zIndex = '9999';
  }
  if (main) main.style.display = 'none';
  if (sidebar) sidebar.style.display = 'none';

  // Ensure initial form is shown
  const loginForm = document.getElementById('login-form');
  const signupForm = document.getElementById('signup-form');
  if (loginForm) loginForm.style.display = 'block';
  if (signupForm) signupForm.style.display = 'none';
}

// Show portal (hide login)
function hideLoginPage() {
  const loginPage = document.getElementById('login-page');
  const main = document.querySelector('.main');
  const sidebar = document.querySelector('.sidebar');

  if (loginPage) {
    loginPage.classList.remove('show');
    loginPage.style.display = 'none';
  }
  if (main) main.style.display = 'flex';
  if (sidebar) sidebar.style.display = 'flex';
}

// Switch between login/signup tabs
function switchAuthTab(tab) {
  const tabLogin = document.getElementById('tab-login');
  const tabSignup = document.getElementById('tab-signup');
  const formLogin = document.getElementById('login-form');
  const formSignup = document.getElementById('signup-form');

  if (tab === 'login') {
    tabLogin.classList.add('active');
    tabSignup.classList.remove('active');
    formLogin.style.display = 'block';
    formSignup.style.display = 'none';
    clearErrors();
  } else {
    tabSignup.classList.add('active');
    tabLogin.classList.remove('active');
    formLogin.style.display = 'none';
    formSignup.style.display = 'block';
    clearErrors();
  }
}

// Clear error/success messages
function clearErrors() {
  document.getElementById('login-error').classList.remove('show');
  document.getElementById('login-success').classList.remove('show');
  document.getElementById('login-loading').classList.remove('show');
}

// Show error message
function showAuthError(msg) {
  const el = document.getElementById('login-error');
  el.textContent = ' ' + msg;
  el.classList.add('show');
}

// Show success message
function showAuthSuccess(msg) {
  const el = document.getElementById('login-success');
  el.textContent = ' ' + msg;
  el.classList.add('show');
}

// Show loading
function showAuthLoading() {
  const el = document.getElementById('login-loading');
  if (el) {
    el.classList.add('show');
    console.log(' Loading indicator shown');

    // SAFETY: Force hide loading after 8 seconds no matter what
    setTimeout(() => {
      if (el.classList.contains('show')) {
        console.warn(' FORCING loading to hide - request took too long');
        hideAuthLoading();
        showAuthError('Request took too long. Please try again.');
      }
    }, 8000);
  }
}

// Hide loading
function hideAuthLoading() {
  const el = document.getElementById('login-loading');
  if (el) {
    el.classList.remove('show');
    console.log(' Loading indicator hidden');
  }
}

// Handle login
async function handleLogin() {
  console.log('🔵 handleLogin called');
  clearErrors();

  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  console.log('📧 Login attempt:', email);

  if (!email) {
    showAuthError('Email is required');
    return;
  }
  if (!password) {
    showAuthError('Password is required');
    return;
  }

  // Check demo credentials FIRST (instant, no loading)
  if (email === 'test@example.com' && password === 'password123') {
    console.log(' Demo credentials matched - instant login');

    // Create mock user object
    user = {
      uid: 'demo-user-001',
      email: 'test@example.com',
      displayName: 'Demo User'
    };

    showAuthSuccess('Demo login successful! Redirecting...');
    setTimeout(() => {
      console.log(' Redirecting to dashboard...');
      document.getElementById('login-email').value = '';
      document.getElementById('login-password').value = '';
      updateUserCard();
      hideLoginPage();
      clearErrors();
      goPage('dashboard');
    }, 300);
    return;
  }

  // For Firebase attempts, show loading
  showAuthLoading();

  // Check if Firebase is initialized
  if (!auth) {
    console.log(' Firebase not initialized - using demo mode suggestion');
    hideAuthLoading();
    showAuthError('Use demo: test@example.com / password123, or sign up');
    return;
  }

  // Use Firebase authentication
  try {
    console.log(' Attempting Firebase login...');

    // Set a timeout to catch hanging requests
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => {
        console.error(' Login timeout - took more than 10 seconds');
        reject(new Error('Login took too long. Check your internet connection.'));
      }, 10000);
    });

    const result = await Promise.race([
      firebaseSignInWithEmail(email, password),
      timeoutPromise
    ]);

    hideAuthLoading();
    console.log(' Firebase login result:', result.success);

    if (result.success) {
      showAuthSuccess('Login successful! Redirecting...');

      setTimeout(() => {
        document.getElementById('login-email').value = '';
        document.getElementById('login-password').value = '';
        updateUserCard();
        hideLoginPage();
        clearErrors();
        goPage('dashboard');
      }, 300);
    } else {
      let errMsg = result.error || 'Login failed';
      if (errMsg.includes('user-not-found')) errMsg = 'User not found. Create account first.';
      else if (errMsg.includes('wrong-password')) errMsg = 'Wrong password.';
      else if (errMsg.includes('invalid-email')) errMsg = 'Invalid email format.';
      showAuthError(errMsg);
    }
  } catch (err) {
    hideAuthLoading();
    console.error(' Login error:', err);
    showAuthError(err.message || 'Login failed');
  }
}

// Handle signup
async function handleSignup() {
  console.log('🔵 handleSignup called');
  clearErrors();

  const name = document.getElementById('signup-name').value.trim();
  const email = document.getElementById('signup-email').value.trim();
  const password = document.getElementById('signup-password').value;

  console.log('📧 Signup attempt:', email);

  if (!name) {
    showAuthError('Name is required');
    return;
  }
  if (!email) {
    showAuthError('Email is required');
    return;
  }
  if (!password || password.length < 6) {
    showAuthError('Password must be at least 6 characters');
    return;
  }

  // Check if Firebase is initialized
  if (!auth) {
    console.log(' Firebase not initialized - demo mode signup');

    // Create mock user object instantly
    user = {
      uid: 'demo-user-' + Date.now(),
      email: email,
      displayName: name
    };

    showAuthSuccess('Account created! Welcome aboard!');
    setTimeout(() => {
      console.log(' Redirecting to dashboard...');
      document.getElementById('signup-name').value = '';
      document.getElementById('signup-email').value = '';
      document.getElementById('signup-password').value = '';
      updateUserCard();
      hideLoginPage();
      clearErrors();
      goPage('dashboard');
    }, 300);
    return;
  }

  // For Firebase attempts, show loading
  showAuthLoading();

  // Use Firebase authentication
  try {
    console.log(' Attempting Firebase signup...');

    // Set a timeout to catch hanging requests
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => {
        console.error(' Signup timeout - took more than 10 seconds');
        reject(new Error('Signup took too long. Check your internet connection.'));
      }, 10000);
    });

    const result = await Promise.race([
      firebaseSignUpWithEmail(name, email, password),
      timeoutPromise
    ]);

    hideAuthLoading();
    console.log(' Firebase signup result:', result.success);

    if (result.success) {
      showAuthSuccess('Account created! Welcome aboard!');

      setTimeout(() => {
        document.getElementById('signup-name').value = '';
        document.getElementById('signup-email').value = '';
        document.getElementById('signup-password').value = '';
        updateUserCard();
        hideLoginPage();
        clearErrors();
        goPage('dashboard');
      }, 300);
    } else {
      let errMsg = result.error || 'Signup failed';
      if (errMsg.includes('email-already-in-use')) errMsg = 'Email already registered. Try logging in.';
      else if (errMsg.includes('weak-password')) errMsg = 'Password too weak (min 6 chars).';
      else if (errMsg.includes('invalid-email')) errMsg = 'Invalid email format.';
      showAuthError(errMsg);
    }
  } catch (err) {
    hideAuthLoading();
    console.error(' Signup error:', err);
    showAuthError(err.message || 'Signup failed');
  }
}

// Logout
function handleLogout() {
  if (confirm('Are you sure you want to logout?')) {
    firebaseSignOut();
    document.getElementById('login-email').value = '';
    document.getElementById('login-password').value = '';
    document.getElementById('signup-name').value = '';
    document.getElementById('signup-email').value = '';
    document.getElementById('signup-password').value = '';
    clearErrors();
    switchAuthTab('login');
    toast('Logged out');
  }
}

function updateUserCard() {
  if (!user) return;

  // Preference: Sync from Firebase profile
  if (firebaseAPI) {
    refreshProfile();
  } else {
    // Fallback logic
    const name = user.displayName || user.email.split('@')[0];
    const initials = name.substring(0, 2).toUpperCase();

    document.querySelectorAll('.user-avatar').forEach(av => av.textContent = initials);
    document.querySelectorAll('.user-name').forEach(el => el.textContent = name);

    const handleEl = document.getElementById('sidebar-user-handle');
    if (handleEl) handleEl.textContent = user.email || 'user@snipit.dev';
  }

  updateXPUI();

  // Sync dashboard
  if (typeof LocalStorage !== 'undefined') {
    updateDashboardStats(LocalStorage.getSnippets());
  }
}

// ========== SYNC USERNAME ACROSS ALL PAGES ==========
function syncUsernameAcrossAllPages(profileData) {
  if (!profileData) return;

  const displayName = profileData.username || profileData.displayName || 'User';
  const handle = profileData.handle || '@user';
  const avatar = (displayName || 'U').charAt(0).toUpperCase();

  // Sidebar elements
  const sidebarName = document.getElementById('sidebar-user-name');
  const sidebarHandle = document.getElementById('sidebar-user-handle');
  const sidebarAvatar = document.getElementById('sidebar-user-avatar');

  if (sidebarName) sidebarName.textContent = displayName;
  if (sidebarHandle) sidebarHandle.textContent = user ? user.email : handle;
  if (sidebarAvatar) sidebarAvatar.textContent = avatar;

  // Dashboard elements
  const dashName = document.getElementById('dash-user-name');
  if (dashName) dashName.textContent = displayName;

  const dashEmail = document.getElementById('dash-user-email');
  if (dashEmail) dashEmail.textContent = user ? user.email : '';

  // Settings email
  const settingsEmail = document.getElementById('settings-email-display');
  if (settingsEmail) settingsEmail.textContent = user ? `Signed in as ${user.email}` : '';

  // Profile page elements
  const profileName = document.getElementById('prof-name');
  const profileHandle = document.getElementById('prof-handle');
  const profileAvatar = document.getElementById('prof-avatar');

  if (profileName) profileName.textContent = displayName;
  if (profileHandle) profileHandle.textContent = handle;
  if (profileAvatar) profileAvatar.textContent = avatar;

  console.log(' Username synced across all pages:', displayName);
}

async function updateAiStatusDisplay() {
  const connStatus = document.getElementById('ai-conn-status');
  const serverStatus = document.getElementById('ai-server-status');

  if (!connStatus || !serverStatus) return;

  // Check if token is available
  if (api && api.apiKey) {
    connStatus.innerHTML = ' Online - Token Loaded';
    connStatus.style.color = 'var(--g)';
  } else {
    connStatus.innerHTML = ' Offline - Check local.properties';
    connStatus.style.color = 'var(--r)';
  }

  // Check if server is reachable
  try {
    if (api) {
      const isOnline = await api.checkConnection();
      if (isOnline) {
        serverStatus.innerHTML = '[OK] Connected to Gemini AI Engine';
        serverStatus.style.color = 'var(--g)';
      } else {
        serverStatus.innerHTML = '[WARN] AI Engine unreachable';
        serverStatus.style.color = 'var(--y)';
      }
    }
  } catch (err) {
    serverStatus.innerHTML = ' Connection failed';
    serverStatus.style.color = 'var(--r)';
  }
}

const pages = {
  dashboard: 'Dashboard',
  vault: 'Snippet Vault',
  beam: 'Beam Station',
  ai: 'Snip-AI Agent',
  devdex: 'Dev-Dex',
  xp: 'Dev-XP',
  profile: 'Profile',
  settings: 'Settings'
};

const subs = {
  dashboard: 'Welcome back',
  vault: 'Your offline code library',
  beam: 'Real-time code transfer to PC',
  ai: 'Powered by Claude Sonnet 4.6',
  devdex: 'Commands, Codes & Patterns',
  xp: 'Your developer journey',
  profile: 'Your account and devices',
  settings: 'App preferences'
};

// Page Navigation
function goPage(pageId) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  const page = document.getElementById('page-' + pageId);
  const nav = document.getElementById('nav-' + pageId);

  if (page) page.classList.add('active');
  if (nav) nav.classList.add('active');

  // Breadcrumb
  const bread = document.getElementById('breadcrumb-page');
  if (bread) bread.textContent = pageId;

  const titleEl = document.getElementById('topbar-title');
  const subEl = document.getElementById('topbar-sub');
  if (subEl) subEl.textContent = subs[pageId] || '';
  const content = document.querySelector('.content');
  if (content) content.scrollTop = 0;

  if (pageId === 'beam') generateNewQR();
  if (pageId === 'vault') syncVault();
  if (pageId === 'devdex') loadDevDex();
  if (pageId === 'settings') {
    updateAiStatusDisplay();
  }
  if (pageId === 'ai') {
    console.log('📖 AI page opened, initializing chat...');
    initializeAIPage();
  }
  if (pageId === 'xp') refreshXPScreen();
  if (pageId === 'profile') refreshProfile();
}

// ========== DEV-DEX ==========
let dexData = [];
let dexActiveTab = 'all';

async function loadDevDex() {
  if (dexData.length > 0) { renderDexEntries(); return; } // already loaded
  try {
    const res = await fetch('handbook.json');
    dexData = await res.json();
    renderDexEntries();
  } catch (err) {
    document.getElementById('dex-list').innerHTML =
      '<div style="text-align:center;padding:40px;color:var(--r);font-family:var(--mono);font-size:12px">Failed to load handbook.json</div>';
  }
}

function switchDexTab(tab, el) {
  dexActiveTab = tab;
  document.querySelectorAll('.dex-tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  document.getElementById('dex-search').value = '';
  renderDexEntries();
}

function filterDex() {
  renderDexEntries();
}

function renderDexEntries() {
  const q = (document.getElementById('dex-search')?.value || '').toLowerCase();
  const list = document.getElementById('dex-list');
  const countEl = document.getElementById('dex-count');

  const catColors = {
    'GIT': 'var(--p)',
    'LINUX': 'var(--g)',
    'HTTP': 'var(--r)',
    'PORTS': 'var(--c)',
    'REGEX': 'var(--y)'
  };

  let filtered = dexData.filter(e => {
    const matchTab = dexActiveTab === 'all' || e.category === dexActiveTab;
    const matchSearch = !q ||
      e.command.toLowerCase().includes(q) ||
      e.summary.toLowerCase().includes(q) ||
      e.documentation.toLowerCase().includes(q) ||
      e.category.toLowerCase().includes(q);
    return matchTab && matchSearch;
  });

  if (countEl) countEl.textContent = filtered.length;

  if (filtered.length === 0) {
    list.innerHTML = '<div style="text-align:center;padding:40px;color:var(--t4);font-family:var(--mono);font-size:12px">No entries found.</div>';
    return;
  }

  const color = (cat) => catColors[cat] || 'var(--g)';

  list.innerHTML = filtered.map(e => `
    <div class="dex-entry" style="border-left-color:${color(e.category)}">
      <div class="dex-cat" style="color:${color(e.category)}">${e.category}</div>
      <div class="dex-cmd">${e.command.replace(/</g,'&lt;').replace(/>/g,'&gt;')}</div>
      <div class="dex-summary">${e.summary}</div>
      <div class="dex-doc">${e.documentation}</div>
    </div>
  `).join('');
}

// Beam Station PIN Entry
let pinDigits = [];
const correctPin = '482951';

function pressPin(k) {
  if (k === 'del') { pinDigits.pop(); }
  else if (k === 'clr') { pinDigits = []; }
  else if (pinDigits.length < 6) { pinDigits.push(k); }
  updatePinDisplay();
  if (pinDigits.length === 6) validatePin();
}

function updatePinDisplay() {
  for (let i = 0; i < 6; i++) {
    const el = document.getElementById('pe' + i);
    if (pinDigits[i] !== undefined) {
      el.textContent = pinDigits[i];
      el.classList.add('active');
    } else {
      el.textContent = '_';
      el.classList.remove('active', 'success');
    }
  }
}

let isBeamSessionTrusted = false;

async function validatePin() {
  const entered = pinDigits.join('');
  if (entered.length < 6) return;

  addLog(`Verifying PIN: ${entered}...`, 'info');

  // PIN Expiry simulation: Valid for 5 minutes
  const pinTime = localStorage.getItem('snipit_pin_time');
  if (pinTime && (Date.now() - parseInt(pinTime) > 300000)) {
    addLog('PIN has expired (5m timeout).', 'err');
    toast('PIN expired');
    failPin();
    return;
  }

  if (firebaseAPI) {
    const data = await firebaseAPI.getSnippetByPin(entered);
    if (data || entered === '482951') {
      for (let i = 0; i < 6; i++) {
        document.getElementById('pe' + i).classList.add('success');
      }
      setTimeout(() => {
        isBeamSessionTrusted = true;
        addLog('Session Trusted. Anti-Gravity Auto-Run activated.', 'ok');
        simulateConnect(data ? data.code : null, data ? data.title : null, data ? data.language : null);
        addXP(15);
        pinDigits = [];
        updatePinDisplay();
      }, 600);
    } else {
      addLog('Invalid or expired PIN.', 'err');
      toast('PIN not found or expired');
      failPin();
    }
  } else {
    // Fallback
    if (entered === '482951') {
      isBeamSessionTrusted = true;
      addLog('Session Trusted. Anti-Gravity Auto-Run activated.', 'ok');
      simulateConnect();
      pinDigits = [];
      updatePinDisplay();
    } else {
      failPin();
    }
  }
}

function failPin() {
  for (let i = 0; i < 6; i++) {
    document.getElementById('pe' + i).style.borderColor = 'var(--r)';
    document.getElementById('pe' + i).style.color = 'var(--r)';
  }
  setTimeout(() => {
    pinDigits = [];
    updatePinDisplay();
    for (let i = 0; i < 6; i++) {
      document.getElementById('pe' + i).style.borderColor = '';
      document.getElementById('pe' + i).style.color = '';
    }
  }, 1000);
}

// Beam Station Connection
let connected = false;

function simulateConnect(code = null, title = null, lang = null) {
  connected = true;
  const bpulse = document.getElementById('bpulse');
  const bstatus = document.getElementById('bstatus-txt');
  const bdot = document.getElementById('beam-dot');
  const btxt = document.getElementById('beam-status-txt');

  if (bpulse) bpulse.classList.add('connected');
  if (bstatus) bstatus.innerHTML = 'Connected: <span>Mobile_Device</span> · SnipIT Remote';
  if (bdot) {
    bdot.classList.remove('offline');
    bdot.classList.add('online');
  }
  if (btxt) btxt.textContent = 'Device connected';

  addLog('Device paired successfully.', 'ok');

  const codeEl = document.getElementById('incoming-code');
  if (code && codeEl) {
    codeEl.textContent = code;
    addLog(`Snippet received: "${title || 'Untitled'}" (${lang || 'Plain'})`, 'ok');
    
    if (isBeamSessionTrusted) {
      addLog(`[Auto-Run] Injecting directly to Vault...`, 'info');
      const s = { title: title || 'Beamed Snippet', code: code, language: lang || 'Plain', tags: 'Beamed', starred: false };
      LocalStorage.addSnippet(s).then((newSnip) => {
         addLog(`[Auto-Run] Handoff complete.`, 'ok');
         toast(' Auto-Injected to Vault! +15 XP');
         loadVault(); // Refresh sidebar list
         openSnipDetail(newSnip || s); 
      });
    } else {
      toast(' Code beamed! +15 XP');
    }
    return;
  }

  // Real listener for QR/Session-based beams
  if (firebaseAPI && user) {
    const sessionId = user.uid.substring(0, 6).toUpperCase();
    if (!window._beamListenerRegistered) {
      window._beamListenerRegistered = true;
      addLog(`Listening on active_beam node for Session: ${sessionId}...`, 'info');
      firebaseAPI.listenForBeam(sessionId, (data) => {
        if (data && data.code && codeEl) {
          codeEl.textContent = data.code;
          addLog(`Incoming payload detected: "${data.title || 'Untitled'}" (${data.language || 'Plain'})`, 'ok');
          
          if (isBeamSessionTrusted) {
             addLog(`[Auto-Run] Bypassing prompts. Injecting directly to Vault...`, 'info');
             const s = { title: data.title || 'Beamed Snippet', code: data.code, language: data.language || 'Plain', tags: 'Beamed', starred: false };
             LocalStorage.addSnippet(s).then((newSnip) => {
                addLog(`[Auto-Run] Handoff complete. Active Editor populated.`, 'ok');
                toast(' Auto-Injected to Vault! +15 XP');
                loadVault(); // Refresh sidebar list
                openSnipDetail(newSnip || s); 
             });
          } else {
             toast(' Code received from phone! +15 XP');
          }
        }
      });
    }
  }
}

function disconnectBeam() {
  connected = false;
  document.getElementById('bpulse').classList.remove('connected');
  document.getElementById('bstatus-txt').textContent = 'Disconnected — waiting for device';
  document.getElementById('beam-dot').classList.remove('online');
  document.getElementById('beam-dot').classList.add('offline');
  document.getElementById('beam-status-txt').textContent = 'No device paired';
  addLog('Session ended.', '');
  toast('Device disconnected');
}

function addLog(msg, type) {
  const log = document.getElementById('session-log');
  const t = new Date().toLocaleTimeString([], { hour12: false });
  const div = document.createElement('div');
  div.className = 'log-line ' + (type || '');
  div.innerHTML = `<span class="log-ts">[${t}]</span><span>${msg}</span>`;
  log.appendChild(div);
  log.scrollTop = log.scrollHeight;
}

function injectVSCode() {
  if (!connected) { toast('Connect a device first!'); return; }
  addLog('Initializing Beam Injector Protocol...', 'info');

  const code = document.getElementById('incoming-code').textContent;
  if (!code || code.includes('No snippet received')) {
    addLog('Error: No source code found to inject.', 'err');
    return;
  }

  // Visual feedback for injection
  const btn = document.getElementById('btn-inject');
  const oldText = btn.textContent;
  btn.textContent = ' Injecting...';
  btn.disabled = true;

  setTimeout(() => {
    addLog('Streaming code to VS Code Extension via SnipIT-Link...', 'info');
    setTimeout(() => {
      addLog('Injection successful. Check your IDE editor.', 'ok');
      btn.textContent = oldText;
      btn.disabled = false;
      toast(' Injected to IDE! +20 XP');
      addXP(20);
    }, 800);
  }, 400);
}

function copyReceived() {
  const code = document.getElementById('incoming-code').textContent;
  if (!code || code.includes('No snippet received')) {
    toast('No code to copy!');
    return;
  }
  navigator.clipboard.writeText(code).then(() => {
    addLog('Copied to system clipboard.', 'ok');
    addXP(5);
    toast('Copied!');
  });
}

function saveToVault() {
  const code = document.getElementById('incoming-code').textContent;
  if (!code || code.includes('No snippet received')) {
    toast('No code to save!');
    return;
  }

  const title = prompt('Enter title for this snippet:', 'Beamed Snippet');
  if (title) {
    LocalStorage.addSnippet({
      title: title,
      code: code,
      language: 'Plain',
      tags: 'beamed'
    });
    addLog('Snippet saved to Vault: ' + title, 'ok');
    addXP(15);
    loadVault();

    // Refresh dashboard last beamed
    updateLastBeamed(title, code);

    // Clear after save
    clearBeamed();
  }
}

function clearBeamed() {
  const codeEl = document.getElementById('incoming-code');
  if (codeEl) codeEl.textContent = 'No snippet received.';
  disconnectBeam();

  // Also clear from Firebase so it doesn't reappear on reload
  if (firebaseAPI && user) {
    const sessionId = user.uid.substring(0, 6).toUpperCase();
    firebaseAPI.clearBeam(sessionId);
  }

  localStorage.removeItem('snipit_active_beam');
  addLog('Beam buffer cleared.', 'info');
  toast('Beam cleared');
}

function simulateConnectionDemo() {
  addLog('Simulating remote handshake...', 'info');
  setTimeout(() => {
    simulateConnect(
      "// Optimized Firebase Query\nfirebase.database().ref('users')\n  .orderByChild('xp')\n  .limitToLast(10)\n  .once('value')",
      "Firebase Top 10 XP",
      "JavaScript"
    );
  }, 1000);
}

function updateLastBeamed(title, code) {
  const container = document.getElementById('dash-last-beamed');
  if (!container) return;

  localStorage.setItem('snipit_last_beamed', JSON.stringify({ title, code, time: new Date().toISOString() }));

  container.innerHTML = `
    <div class="snip-card" style="margin:0; border-left:3px solid var(--c)">
      <div class="snip-header">
        <div class="snip-title">${title}</div>
        <span class="lang-badge" style="background:var(--c)15; color:var(--c)">BEAMED</span>
      </div>
      <div class="snip-code" style="max-height:60px">${code.substring(0, 100)}...</div>
      <div style="font-size:9px; color:var(--t4); margin-top:8px">Just now · Remote Transfer</div>
    </div>
  `;
}

function beamSnip(name, event, payload = null) {
  if (event) event.stopPropagation();
  goPage('beam');
  setTimeout(() => simulateConnect(payload), 500);
  toast('Beaming "' + name + '"...');
}

// AI Agent
const aiReplies = [
  { pre: 'Here is the optimized version:', code: `val auth by lazy {\n  FirebaseAuth.getInstance()\n}\nlifecycleScope.launch {\n  auth.signInAnonymously().await()\n}` },
  { pre: 'Converted to Kotlin with coroutines:', code: `suspend fun fetchUsers() =\n  withContext(Dispatchers.IO) {\n    db.collection("users")\n      .limit(20).get().await()\n  }` },
  { pre: 'Found the issue — null check missing:', code: `// Fix: add null safety\nval result = response?.body\n  ?: throw NullPointerException(\n    "Response body is null"\n  )` }
];
// AI Page Initialization
async function initializeAIPage() {
  console.log(' Initializing AI page...');

  // Check authentication
  if (!user) {
    console.warn(' User not logged in');
    const list = document.getElementById('ai-history-list');
    if (list) list.innerHTML = '<div style="text-align:center; padding:40px 20px; color:var(--t4); font-size:11px">Please login to use AI Chat.</div>';
    return;
  }

  // Check Firebase
  if (!firebaseAPI) {
    console.warn(' Firebase not initialized');
    const list = document.getElementById('ai-history-list');
    if (list) list.innerHTML = '<div style="text-align:center; padding:40px 20px; color:var(--t4); font-size:11px">Firebase loading...</div>';

    // Retry after a moment
    setTimeout(() => {
      if (firebaseAPI) loadAIHistory();
    }, 1000);
    return;
  }

  // Load history
  console.log(' Firebase ready, loading chat history...');
  await loadAIHistory();

  // Focus input if not in new chat mode
  if (currentChatId) {
    const input = document.getElementById('ai-in');
    if (input) input.focus();
  }

  console.log(' AI page initialized successfully');
}

let aiIdx = 0;

// AI Agent Logic
let currentChatId = null;

function autoGrow(element) {
  element.style.height = "5px";
  element.style.height = (element.scrollHeight) + "px";
}

function fillAI(txt) {
  const input = document.getElementById('ai-in');
  if (input) {
    input.value = txt;
    input.focus();
    autoGrow(input);
  }
}

async function sendAI() {
  const input = document.getElementById('ai-in');
  const msgs = document.getElementById('ai-msgs');
  const sendBtn = document.getElementById('ai-send-btn');
  const query = input.value.trim();

  if (!query) return;

  // Add user message to UI
  appendMessage('user', query);
  input.value = '';
  autoGrow(input);

  // Disable input while thinking
  input.disabled = true;
  sendBtn.disabled = true;

  try {
    // Create new chat session IMMEDIATELY if one doesn't exist
    if (!currentChatId) {
      currentChatId = Date.now().toString();
      console.log(' New chat session created:', currentChatId);
    }

    // Get conversation history for context (like Gemini)
    let conversationContext = [];
    if (firebaseAPI && user && currentChatId) {
      const history = await firebaseAPI.getChatHistory();
      const currentChat = history[currentChatId];
      if (currentChat && currentChat.messages) {
        // Include last 5 messages for context (not all, for performance)
        conversationContext = currentChat.messages.slice(-5).map(m => ({
          user: m.user,
          ai: m.ai
        }));
      }
    }

    // Call AI with conversation history context
    const response = await api.getAIResponseWithContext(query, conversationContext);
    const reply = response.explanation || "I'm having trouble connecting to the cloud. Check if the server is running!";

    // Add AI message to UI
    appendMessage('ai', reply);

    // Save to Firebase History
    if (firebaseAPI && user) {
      console.log(' Saving to Firebase chat:', currentChatId);
      // Use the compatibility method that now uses the new role/body schema
      await firebaseAPI.saveChatPair(currentChatId, query, reply);
      loadAIHistory(); // Refresh history sidebar
    }
  } catch (err) {
    console.error('AI Error:', err);
    appendMessage('ai', "Error connecting to Snip-AI. Please try again later.");
  } finally {
    input.disabled = false;
    sendBtn.disabled = false;
    input.focus();
  }
}

function appendMessage(role, text) {
  const msgs = document.getElementById('ai-msgs');
  const div = document.createElement('div');
  div.className = `msg-bubble msg-${role}`;

  if (role === 'ai') {
    div.innerHTML = `<div class="msg-ai-label">SNIP-AI · CLAUDE</div>${text.replace(/\n/g, '<br>')}`;
  } else {
    div.textContent = text;
  }

  msgs.appendChild(div);
  msgs.scrollTop = msgs.scrollHeight;
}

async function loadAIHistory() {
  const list = document.getElementById('ai-history-list');
  if (!list) return console.warn(' History list element not found');

  if (!firebaseAPI || !user) {
    console.warn(' Firebase API or user not initialized');
    list.innerHTML = '<div style="text-align:center; padding:40px 20px; color:var(--t4); font-size:11px">Not logged in.</div>';
    return;
  }

  try {
    console.log(' Loading chat history from Firebase...');
    const history = await firebaseAPI.getChatHistory();
    console.log(' History loaded. Total chats:', history ? Object.keys(history).length : 0);

    if (!history || Object.keys(history).length === 0) {
      console.log(' No chats found in history');
      list.innerHTML = '<div style="text-align:center; padding:40px 20px; color:var(--t4); font-size:11px">No chat history found.</div>';
      return;
    }

    // Sort history by timestamp (newest first)
    const sortedIds = Object.keys(history).sort((a, b) => b - a);
    console.log(' Sorted chat IDs:', sortedIds);

    list.innerHTML = sortedIds.map(id => {
      const chat = history[id];
      const activeClass = id === currentChatId ? 'active' : '';

      // Safe title extraction
      let title = "New Conversation";
      if (chat && chat.title) {
        title = chat.title;
      } else if (chat && chat.messages && chat.messages.length > 0) {
        const first = chat.messages[0];
        title = (first.user || first.body || "New Chat").substring(0, 30) + '...';
      }

      const date = new Date(parseInt(id)).toLocaleDateString();
      console.log(`  - ${id}: "${title}" (${date}, ${activeClass})`);

      return `
        <div class="ai-history-item ${activeClass}" onclick="loadChat('${id}')">
          <div class="ai-hist-title">${title}</div>
          <div class="ai-hist-meta">${date}</div>
        </div>
      `;
    }).join('');

    console.log(' History sidebar updated successfully');
  } catch (err) {
    console.error(' Error loading history:', err);
    list.innerHTML = '<div style="text-align:center; padding:40px 20px; color:var(--r); font-size:11px">Error loading history. Check console.</div>';
  }
}

async function loadChat(id) {
  currentChatId = id;
  const msgs = document.getElementById('ai-msgs');
  msgs.innerHTML = '';

  const history = await firebaseAPI.getChatHistory();
  const chat = history[id];

  if (chat && chat.messages) {
    chat.messages.forEach(m => {
      if (m.role) {
        // Universal schema
        appendMessage(m.role === 'assistant' ? 'ai' : m.role, m.body);
      } else {
        // Legacy schema compatibility
        if (m.user) appendMessage('user', m.user);
        if (m.ai) appendMessage('ai', m.ai);
      }
    });
  }

  loadAIHistory();
}

async function clearAIHistory() {
  if (!confirm('Are you sure you want to delete ALL chat history? This cannot be undone.')) return;

  if (firebaseAPI && user) {
    try {
      toast('Clearing history...');
      await firebaseAPI.clearChatHistory();
      currentChatId = null;
      startNewChat();
      toast('Chat history cleared');
    } catch (err) {
      console.error('Error clearing history:', err);
      toast('Failed to clear history');
    }
  }
}

function startNewChat() {
  console.log('🆕 Starting new chat. Current chat ID was:', currentChatId);

  // Set to null so next message creates a new ID
  currentChatId = null;

  // Clear messages area
  document.getElementById('ai-msgs').innerHTML = `
    <div class="msg-bubble msg-ai">
      <div class="msg-ai-label">SNIP-AI · CLAUDE</div>
      <strong>Welcome to Snip-AI Chat! </strong><br><br>
      I'm your advanced code assistant, integrated directly into SnipIT. I can help you with:
      <ul style="margin-top: 10px; margin-left: 20px;">
        <li> <strong>Code Analysis</strong> - Review and improve your snippets</li>
        <li> <strong>Bug Fixing</strong> - Identify and fix issues</li>
        <li> <strong>Refactoring</strong> - Make your code cleaner and more efficient</li>
        <li> <strong>Best Practices</strong> - Learn modern coding techniques</li>
        <li> <strong>SnipIT Integration</strong> - Help with vaults, beams, and projects</li>
      </ul>
      <br>Feel free to paste code, ask questions, or let's discuss your project. I'll remember our conversation! 
    </div>
  `;

  // Refresh history sidebar to show archived chats
  loadAIHistory();

  // Focus on input for next message
  setTimeout(() => {
    const input = document.getElementById('ai-in');
    if (input) input.focus();
  }, 100);
}

document.getElementById('ai-in')?.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendAI();
  }
});

// Gamification & Stats System
let userStats = {
  web_xp: parseInt(localStorage.getItem('snipit_user_web_xp') || '0'),
  app_xp: 0,
  xp: 0, // Total
  level: 1,
  levelName: 'Novice',
  streak: 0,
  badges: [],
  lastActivity: null
};

const LEVELS = [
  { name: 'Novice', min: 0 },
  { name: 'Syntax Learner', min: 100 },
  { name: 'Code Scout', min: 250 },
  { name: 'Snippet Master', min: 500 },
  { name: 'Logic Architect', min: 1000 },
  { name: 'Syntax Wizard', min: 2500 },
  { name: 'Code Deity', min: 5000 }
];

async function addXP(points, reason = "") {
  userStats.web_xp += points;
  localStorage.setItem('snipit_user_web_xp', userStats.web_xp);

  if (reason) console.log(`Web XP Earned: +${points} for ${reason}`);

  // Sync to Firebase
  if (firebaseAPI && user) {
    try {
      await firebaseAPI.updateStats({
        web_xp: userStats.web_xp,
        lastUpdated: new Date().toISOString()
      });
      // Fetch latest to get app_xp sum
      const latest = await firebaseAPI.getStats();
      if (latest) {
        userStats.app_xp = latest.app_xp || 0;
        userStats.web_xp = latest.web_xp || userStats.web_xp;
      }
    } catch (err) {
      console.warn('Sync failed, using local storage.');
    }
  }

  userStats.xp = Math.floor((userStats.web_xp + userStats.app_xp) / 2);

  // Dynamic Level Calculation based on total XP
  const oldLevel = userStats.level;
  userStats.level = Math.floor(Math.sqrt(userStats.xp / 10)) + 1;

  if (userStats.level > oldLevel) {
    toast(` LEVEL UP! You reached Level ${userStats.level}!`);
    addLog(`Level up! Welcome to Level ${userStats.level}.`, 'ok');

    // Milestones
    if (userStats.level === 5) toast(' Milestone: Pro Scripter! New themes unlocked.');
    if (userStats.level === 10) toast(' Milestone: Code Wizard! Master of the Vault.');
  }

  updateXPUI();
  toast(`+${points} XP Earned!  Total: ${userStats.xp} XP`);

  if (document.getElementById('page-xp').classList.contains('active')) {
    refreshXPScreen();
  }
}

function updateXPUI() {
  // Update Level
  let currentLevel = 0;
  for (let i = 0; i < LEVELS.length; i++) {
    if (userStats.xp >= LEVELS[i].min) {
      currentLevel = i + 1;
      userStats.levelName = LEVELS[i].name;
    }
  }
  userStats.level = currentLevel;

  // Sidebar XP Bar
  const xpFill = document.getElementById('sidebar-xp-fill');
  const xpLabel = document.querySelector('.xp-label');

  const currentMin = LEVELS[userStats.level - 1].min;
  const nextLevel = LEVELS[userStats.level];
  const nextMin = nextLevel ? nextLevel.min : userStats.xp;

  const progress = nextMin > currentMin ? ((userStats.xp - currentMin) / (nextMin - currentMin)) * 100 : 100;

  if (xpFill) xpFill.style.width = Math.min(100, progress) + '%';
  if (xpLabel) xpLabel.textContent = userStats.xp + ' XP';
}

function refreshXPScreen() {
  userStats.xp = Math.floor((userStats.web_xp + userStats.app_xp) / 2);
  const currentLevelIdx = userStats.level - 1;
  const nextLevel = LEVELS[userStats.level];
  const nextMin = nextLevel ? nextLevel.min : userStats.xp;
  const currentMin = LEVELS[currentLevelIdx].min;

  const xpInLevel = userStats.xp - currentMin;
  const xpNeeded = nextMin - currentMin;
  const progressPercent = xpNeeded > 0 ? (xpInLevel / xpNeeded) * 100 : 100;

  // Update Progress Card
  const lvlTxt = document.getElementById('xp-level-txt');
  const ptsTxt = document.getElementById('xp-points-txt');
  const barFill = document.getElementById('xp-bar-fill');
  const nextTxt = document.getElementById('xp-next-txt');

  if (lvlTxt) lvlTxt.textContent = `Level ${userStats.level} — ${userStats.levelName}`;
  if (ptsTxt) {
    ptsTxt.innerHTML = `<span style="color:var(--t1)">${userStats.xp} Total XP</span><br>
                        <span style="font-size:10px; color:var(--t3)">Web: ${userStats.web_xp} · App: ${userStats.app_xp}</span>`;
  }
  if (barFill) barFill.style.width = progressPercent + '%';

  if (nextTxt) {
    if (nextLevel) {
      nextTxt.textContent = `${nextMin - userStats.xp} XP to Level ${userStats.level + 1} · ${nextLevel.name}`;
    } else {
      nextTxt.textContent = "Maximum Rank Achieved! ";
    }
  }

  // Language Breakdown
  const snippets = LocalStorage.getSnippets();
  const langs = {};
  snippets.forEach(s => {
    const l = s.language || 'Unknown';
    langs[l] = (langs[l] || 0) + 1;
  });

  const langList = document.getElementById('xp-lang-list');
  if (langList) {
    const sortedLangs = Object.entries(langs).sort((a, b) => b[1] - a[1]);
    const maxCount = sortedLangs[0] ? sortedLangs[0][1] : 1;

    const colors = ['var(--p)', 'var(--r)', 'var(--c)', 'var(--y)', 'var(--g)'];

    langList.innerHTML = sortedLangs.map(([name, count], i) => `
      <div class="lang-bar">
        <div class="lb-row"><span class="lb-name">${name}</span><span class="lb-count">${count} snips</span></div>
        <div class="lb-track"><div class="lb-fill" style="width:${(count / maxCount) * 100}%; background:${colors[i % colors.length]}"></div></div>
      </div>
    `).join('') || '<div style="color:var(--t4); font-size:11px">No languages detected.</div>';
  }

  // Update Badges
  if (snippets.length >= 5) document.getElementById('badge-wizard')?.classList.add('earned');
  if (userStats.level >= 5) document.getElementById('badge-poly')?.classList.add('earned');

  // Streak rendering
  const streakRow = document.getElementById('xp-streak-row');
  if (streakRow) {
    const days = streakRow.querySelectorAll('.streak-day');
    const today = new Date().getDay(); // 0 is Sunday, 1 is Monday...
    const adjustedToday = today === 0 ? 6 : today - 1; // Map to 0-6 (M-S)

    const lastActive = localStorage.getItem('snipit_last_active_day');
    const currentStreak = parseInt(localStorage.getItem('snipit_streak') || '0');

    days.forEach((d, i) => {
      d.classList.remove('done', 'today', 'claimable');
      if (i < adjustedToday) {
        if (i >= adjustedToday - currentStreak) d.classList.add('done');
      }
      if (i === adjustedToday) {
        d.classList.add('today');
        const lastClaim = localStorage.getItem('snipit_last_claim_date');
        const todayStr = new Date().toISOString().split('T')[0];
        if (lastClaim !== todayStr) {
          d.classList.add('claimable');
          d.setAttribute('onclick', 'claimStreak()');
          d.title = 'Click to claim today\'s streak!';
        }
      }
    });
  }
}

function updateStreak() {
  const now = new Date();
  const todayStr = now.toISOString().split('T')[0];
  const lastActive = localStorage.getItem('snipit_last_active_date');

  if (lastActive === todayStr) return; // Already counted today

  let streak = parseInt(localStorage.getItem('snipit_streak') || '0');

  if (lastActive) {
    const lastDate = new Date(lastActive);
    const diff = (now - lastDate) / (1000 * 60 * 60 * 24);

    if (diff <= 1.5) { // Consecutive day (allowing some buffer for time of day)
      streak++;
    } else {
      streak = 1; // Reset streak
    }
  } else {
    streak = 1;
  }

  localStorage.setItem('snipit_streak', streak);
  localStorage.setItem('snipit_last_active_date', todayStr);
  console.log(' Streak updated:', streak);
}

async function refreshProfile() {
  if (!user || !firebaseAPI) return;

  const profile = await firebaseAPI.getProfile();
  const stats = await firebaseAPI.getStats();
  const snippets = LocalStorage.getSnippets();

  if (stats) {
    userStats.app_xp = stats.app_xp || 0;
    userStats.web_xp = stats.web_xp || userStats.web_xp;
    userStats.xp = Math.floor((userStats.app_xp + userStats.web_xp) / 2);
    userStats.level = Math.floor(Math.sqrt(userStats.xp / 10)) + 1;
    updateXPUI();
  }

  if (profile) {
    // Use the centralized sync function to update all pages with profile data
    syncUsernameAcrossAllPages(profile);

    // Update profile page specific elements
    const pBio = document.getElementById('prof-bio');
    const pAvatar = document.getElementById('prof-avatar');
    const pUsername = document.getElementById('prof-username');

    if (pBio) pBio.textContent = profile.bio || "No bio set. Click edit to customize your profile!";
    if (pAvatar) pAvatar.textContent = (profile.displayName || 'U').charAt(0).toUpperCase();
    if (pUsername) pUsername.textContent = profile.username || user.email.split('@')[0];
  } else {
    // Fallback: sync with basic user data if no profile exists in database yet
    syncUsernameAcrossAllPages({
      displayName: user.displayName || user.email.split('@')[0],
      username: user.email.split('@')[0],
      handle: '@' + user.email.split('@')[0]
    });
  }

  // Stats & Email
  const pSnips = document.getElementById('prof-stat-snips');
  const pXP = document.getElementById('prof-stat-xp');
  const pEmail = document.getElementById('prof-email');
  const dashEmail = document.getElementById('dash-user-email');

  if (pSnips) pSnips.textContent = snippets.length;
  if (pXP) pXP.textContent = userStats.xp;
  if (pEmail) pEmail.textContent = user.email;
  if (dashEmail) dashEmail.textContent = user.email;
}

// Consolidating profile logic here
async function saveProfile() {
  const username = document.getElementById('edit-prof-username').value;
  const name = document.getElementById('edit-prof-name').value;
  const handle = document.getElementById('edit-prof-handle').value;
  const bio = document.getElementById('edit-prof-bio').value;

  if (!username || !name) {
    toast(' Username and Full Name are required');
    return;
  }

  if (firebaseAPI && user) {
    toast('Saving profile...');
    try {
      await firebaseAPI.updateProfile({
        username: username,
        displayName: name,
        handle: handle.startsWith('@') ? handle : '@' + handle,
        bio: bio
      });

      // Update local cache if needed
      if (user) user.displayName = name;

      toast('Profile updated! +10 XP ');
      addXP(10);

      // Force UI refresh - sync across all pages
      await refreshProfile();
      closeModal();
    } catch (err) {
      console.error('Profile update failed:', err);
      toast('Error saving profile');
    }
  }
}

function getRank(level) {
  const ranks = ['Novice', 'Syntax Learner', 'Code Scout', 'Snippet Master', 'Logic Architect', 'Syntax Wizard', 'Code Deity'];
  return ranks[Math.min(level - 1, ranks.length - 1)] || 'Novice';
}

function updateDashboardStats(snippets, level = 1) {
  const dashCount = document.getElementById('dash-snip-count');
  const dashRank = document.getElementById('dash-rank');

  if (dashCount) dashCount.textContent = snippets.length;
  const heroCount = document.getElementById('dash-hero-snip-count');
  if (heroCount) heroCount.textContent = snippets.length;

  const statusCount = document.getElementById('status-count');
  if (statusCount) statusCount.textContent = snippets.length;

  const rank = getRank(level);
  if (dashRank) dashRank.textContent = rank;

  // New Stats Integration
  const langCount = document.getElementById('dash-lang-count');
  const langList = document.getElementById('dash-lang-list');
  const xpEarned = document.getElementById('dash-xp-earned');
  const rankInfo = document.getElementById('dash-rank-info');

  if (langCount) {
    const langs = [...new Set(snippets.map(s => s.language))];
    langCount.textContent = langs.length;
    if (langList) langList.textContent = langs.slice(0, 3).join(', ') + (langs.length > 3 ? '...' : '');
  }

  if (xpEarned) xpEarned.textContent = userStats.xp;
  if (rankInfo) rankInfo.textContent = `Level ${level} - ${rank.charAt(0) + rank.slice(1).toLowerCase()}`;

  const currentUser = typeof user !== 'undefined' ? user : null;
  if (currentUser) {
    // Sync username across all pages - sidebar, dashboard, and profile
    refreshProfile();
  }

  // Generate Activity Grid (Data Driven)
  const grid = document.getElementById('activity-grid');
  if (grid) {
    grid.innerHTML = '';
    const now = new Date();
    const snippetsByDate = snippets.reduce((acc, s) => {
      const d = s.created ? s.created.split('T')[0] : 'unknown';
      acc[d] = (acc[d] || 0) + 1;
      return acc;
    }, {});

    for (let i = 0; i < 100; i++) {
      const d = new Date(now);
      d.setDate(d.getDate() - (99 - i));
      const dateStr = d.toISOString().split('T')[0];
      const count = snippetsByDate[dateStr] || 0;

      const opacity = count > 0 ? (0.4 + Math.min(count * 0.2, 0.6)) : 0.15;
      const color = count > 0 ? 'var(--g)' : 'var(--bg3)';

      const cell = document.createElement('div');
      cell.style.width = '10px';
      cell.style.height = '10px';
      cell.style.background = color;
      cell.style.borderRadius = '2px';
      cell.style.opacity = opacity;
      cell.title = `${dateStr}: ${count} snippets`;
      grid.appendChild(cell);
    }
  }

  // Activity Grid logic remains...
}

function generateNewQR() {
  if (!user) {
    const qrImg = document.getElementById('qr-img');
    if (qrImg) qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=SNIPIT:BEAM:SESSION:GUEST&color=00ffaa&bgcolor=0d0f17&margin=10`;
    const label = document.getElementById('qr-session-id');
    if (label) label.textContent = `SESSION: GUEST`;
    return;
  }

  const sessionId = user.uid.substring(0, 6).toUpperCase();
  const qrImg = document.getElementById('qr-img');
  if (qrImg) {
    // Using SnipIT theme colors: #00ffaa (green) on #0d0f17 (dark bg)
    qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=snipit://beam/${sessionId}&color=00ffaa&bgcolor=0d0f17&margin=10`;
    addLog(`Beam session active: ${sessionId}`, 'info');

    // Set PIN generation time for expiry (5m)
    localStorage.setItem('snipit_pin_time', Date.now());

    // Update the label below QR
    const label = document.getElementById('qr-session-id');
    if (label) label.textContent = `SESSION: ${sessionId}`;

    // Start listening for this session ID
    if (firebaseAPI) {
      firebaseAPI.listenForBeam(sessionId, (data) => {
        if (data && data.code) {
          simulateConnect(data.code, data.title, data.language);
        }
      });
    }

    toast('Session QR Regenerated');
  }
}

// Beam Buttons Validation
function checkBeamStatus() {
  const codeEl = document.getElementById('incoming-code');
  const statusBeam = document.getElementById('status-beam');
  if (!codeEl) return;
  const code = codeEl.textContent;
  const isDefault = code.includes('No snippet received');

  if (statusBeam) {
    statusBeam.textContent = isDefault ? 'READY' : 'PAIRING...';
    statusBeam.style.color = isDefault ? 'var(--t3)' : 'var(--y)';
  }

  const ids = ['btn-inject', 'btn-copy', 'btn-save'];
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.disabled = isDefault;
      el.style.opacity = isDefault ? '0.5' : '1';
    }
  });
}

// Modals
const modals = {
  'new-snip': `<div class="modal-title" style="display:flex; align-items:center; gap:10px; color:var(--g)">
      <i data-lucide="plus-square" style="width:20px; height:20px"></i>
      <span>// NEW SNIP</span>
    </div>
    <div style="background:var(--bg2); padding:16px; border-radius:8px; border:1px solid var(--bd2); margin-bottom:16px;">
      <div class="form-row">
        <label class="form-label" style="font-size:9px; color:var(--t4); letter-spacing:1px">TITLE</label>
        <input class="form-input" id="snip-title" placeholder="e.g. Firebase Auth Init" style="background:var(--bg1); border-color:var(--bd2)"/>
      </div>
      <div class="form-row">
        <label class="form-label" style="font-size:9px; color:var(--t4); letter-spacing:1px">LANGUAGE</label>
        <select class="form-input" id="snip-lang" style="color:var(--p); background:var(--bg1); border-color:var(--bd2)">
          <option>Java</option><option>Kotlin</option><option>Python</option><option>JavaScript</option>
          <option>SQL</option><option>CLI / Bash</option><option>XML</option><option>C++</option>
        </select>
      </div>
      <div class="form-row">
        <label class="form-label" style="font-size:9px; color:var(--t4); letter-spacing:1px">TAGS</label>
        <input class="form-input" id="snip-tags" placeholder="#Java,#Firebase,#Backend" style="background:var(--bg1); border-color:var(--bd2)"/>
      </div>
      <div class="form-row" style="margin-bottom:0">
        <label class="form-label" style="font-size:9px; color:var(--t4); letter-spacing:1px">CODE</label>
        <textarea class="form-textarea" id="snip-code" rows="8" placeholder="Paste your code here..." style="background:var(--bg1); border-color:var(--bd2); font-family:var(--mono); font-size:12px"></textarea>
      </div>
    </div>
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
      <button class="form-btn" onclick="saveNewSnip()" style="background:var(--g); color:var(--bg0); font-weight:800; border:none; height:42px;">Commit to Vault</button>
      <button class="form-btn cancel" onclick="closeModal()" style="border:1px solid var(--bd2); height:42px;">Cancel</button>
    </div>`,
  'snip-detail': `<div class="modal-title">Firebase Auth Init</div>
    <div style="display:flex;gap:8px;margin-bottom:12px">
      <span style="font-family:var(--mono);font-size:10px;background:var(--p)20;color:var(--p);border:0.5px solid var(--p)40;padding:3px 9px;border-radius:20px">Java</span>
      <span style="font-family:var(--mono);font-size:10px;color:var(--t3)">Saved Mar 27 · Used 14×</span>
    </div>
    <div class="form-row"><textarea class="form-textarea" rows="7">FirebaseApp.initializeApp(this);
FirebaseAuth auth = FirebaseAuth.getInstance();
auth.signInAnonymously()
  .addOnSuccessListener(result -> {
    Log.d("Auth", "Signed in!");
  });</textarea></div>
    <div style="display:flex;gap:8px">
      <button class="form-btn" onclick="closeModal();beamSnip('Firebase Auth')"> Beam IT</button>
      <button class="form-btn cancel" onclick="closeModal()"> Fix with AI</button>
    </div>
    <button class="form-btn cancel" onclick="closeModal();toast('Copied!')"> Copy Code</button>`,
  'edit-profile': `<div class="modal-title">Edit Profile</div>
    <div class="form-row"><label class="form-label">Username</label><input class="form-input" id="edit-prof-username" placeholder="Choose a unique username"/></div>
    <div class="form-row"><label class="form-label">Full Name</label><input class="form-input" id="edit-prof-name" placeholder="Your Name"/></div>
    <div class="form-row"><label class="form-label">Handle</label><input class="form-input" id="edit-prof-handle" placeholder="@username"/></div>
    <div class="form-row"><label class="form-label">Bio</label><input class="form-input" id="edit-prof-bio" placeholder="Tell us about yourself..."/></div>
    <button class="form-btn" onclick="saveProfile()">Save Changes</button>
    <button class="form-btn cancel" onclick="closeModal()">Cancel</button>`,
  'ai-settings': `<div class="modal-title"> AI Settings</div>
    <div class="form-row">
      <label class="form-label">GitHub Models Token</label>
      <p style="font-size:0.85rem;color:#8b949e;margin:0 0 8px">
        <strong> Auto-loaded from local.properties</strong> — Token is automatically pulled from your Android build config if available.
      </p>
      <p style="font-size:0.8rem;color:#6e7681;margin:0 0 12px">
        Or manually override here with a <a href="https://github.com/settings/tokens/new" target="_blank" style="color:#58a6ff">GitHub PAT (models scope)</a>
      </p>
      <input class="form-input" id="github-token" type="password" placeholder="ghp_xxxxxxxxxxxxxxxxxxxx (optional override)"/>
      <small id="token-status" style="color:#6e7681;margin-top:8px;display:block"></small>
    </div>
    <button class="form-btn" onclick="saveAiSettings()"> Save Override (Optional)</button>
    <button class="form-btn cancel" onclick="closeModal()">Cancel</button>
    <button class="form-btn cancel" onclick="clearAiToken()" style="margin-top:8px"> Clear Manual Override</button>`
};

async function openModal(name) {
  document.getElementById('modal-inner').innerHTML = modals[name] || '<p>Modal not found</p>';
  document.getElementById('modal-bg').classList.add('open');
  lucide.createIcons(); // Refresh icons in the modal content

  // Pre-fill Edit Profile
  if (name === 'edit-profile') {
    const profile = await firebaseAPI.getProfile();
    if (profile) {
      document.getElementById('edit-prof-username').value = profile.username || '';
      document.getElementById('edit-prof-name').value = profile.displayName || '';
      document.getElementById('edit-prof-handle').value = profile.handle || '';
      document.getElementById('edit-prof-bio').value = profile.bio || '';
    } else {
      document.getElementById('edit-prof-username').value = user.email.split('@')[0];
      document.getElementById('edit-prof-name').value = user.displayName || user.email.split('@')[0];
      document.getElementById('edit-prof-handle').value = '@' + user.email.split('@')[0];
    }
  }
}

// Handled by consolidated saveProfile above

async function claimStreak() {
  const streak = parseInt(localStorage.getItem('snipit_streak') || '0');
  const lastClaim = localStorage.getItem('snipit_last_claim_date');
  const today = new Date().toISOString().split('T')[0];

  if (lastClaim === today) {
    toast('Already claimed today! ');
    return;
  }

  // Update streak if not already updated today
  updateStreak();

  localStorage.setItem('snipit_last_claim_date', today);
  addXP(20);
  toast(' Streak Claimed! +20 XP');

  if (document.getElementById('page-xp').classList.contains('active')) {
    refreshXPScreen();
  }
}

function closeModal() {
  document.getElementById('modal-bg').classList.remove('open');
}

function saveNewSnip() {
  const title = document.getElementById('snip-title')?.value;
  const lang = document.getElementById('snip-lang')?.value;
  const tags = document.getElementById('snip-tags')?.value;
  const code = document.getElementById('snip-code')?.value;

  if (title && code) {
    LocalStorage.addSnippet({ title, language: lang, tags, code });
    closeModal();
    loadVault();
  }
}

// AI Settings handlers
function saveAiSettings() {
  const token = document.getElementById('github-token')?.value?.trim();
  const statusEl = document.getElementById('token-status');

  if (!token) {
    if (statusEl) statusEl.textContent = ' Token cannot be empty';
    return;
  }

  if (!token.startsWith('ghp_')) {
    if (statusEl) statusEl.textContent = ' Token should start with ghp_';
    return;
  }

  updateGitHubToken(token);
  if (statusEl) statusEl.textContent = ' Token saved! AI will use real responses now.';
  setTimeout(() => {
    closeModal();
    toast(' GitHub Models token configured!');
  }, 1500);
}

function clearAiToken() {
  if (confirm('Remove GitHub token? AI will go offline.')) {
    localStorage.removeItem('github_models_token');
    api.setGitHubToken('');
    updateServerStatus(false);
    closeModal();
    toast('Token cleared');
  }
}

function openAiSettings() {
  openModal('ai-settings');
  setTimeout(() => {
    const token = localStorage.getItem('github_models_token');
    const input = document.getElementById('github-token');
    if (input && token) {
      input.value = token;
    }
  }, 100);
}

function updateAiStatusDisplay() {
  const statusEl = document.getElementById('ai-status-text');
  if (!statusEl) return;

  const token = localStorage.getItem('github_models_token');
  if (token) {
    // Check if it was auto-loaded from server
    if (token.includes('github_pat') || token.startsWith('ghp_')) {
      statusEl.textContent = ' Auto-loaded (local.properties)';
    } else {
      statusEl.textContent = ' Configured (Manual)';
    }
    statusEl.style.color = 'var(--g)';
  } else {
    statusEl.textContent = ' Not configured';
    statusEl.style.color = 'var(--r)';
  }
}

// Toast Notifications
function toast(msg) {
  const el = document.getElementById('toast-el');
  el.textContent = msg;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2400);
}

// Vault Loading & Folders
let currentVaultFolder = 'all';
let currentVaultView = 'grid'; // 'grid' or 'list'
let vaultSortBy = 'recent'; // recent, oldest, alphabetical, mostUsed, starred
const vaultUsageTracker = JSON.parse(localStorage.getItem('snipit_vault_usage') || '{}');

function setVaultFolder(folder, el) {
  currentVaultFolder = folder;
  document.querySelectorAll('#vault-folders .nav-item').forEach(i => i.classList.remove('active'));
  if (el) el.classList.add('active');
  loadVault();
}

function setVaultView(viewMode, el) {
  currentVaultView = viewMode;
  document.querySelectorAll('.vault-view-btn').forEach(btn => btn.classList.remove('active'));
  if (el) el.classList.add('active');

  const vaultList = document.getElementById('vault-list');
  if (vaultList) {
    if (viewMode === 'list') {
      vaultList.style.gridTemplateColumns = '1fr';
    } else {
      vaultList.style.gridTemplateColumns = 'repeat(auto-fill, minmax(320px, 1fr))';
    }
  }
  loadVault();
}

function getVaultSortBy() {
  const selector = document.getElementById('vault-sort-sel');
  if (selector) {
    vaultSortBy = selector.value;
  }
  return vaultSortBy;
}

function sortSnippets(snippets) {
  const sortType = getVaultSortBy();
  const sorted = [...snippets];

  switch (sortType) {
    case 'oldest':
      sorted.sort((a, b) => new Date(a.created || 0) - new Date(b.created || 0));
      break;
    case 'alphabetical':
      sorted.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
      break;
    case 'mostUsed':
      sorted.sort((a, b) => (vaultUsageTracker[b.id] || 0) - (vaultUsageTracker[a.id] || 0));
      break;
    case 'starred':
      sorted.sort((a, b) => (b.starred ? 1 : 0) - (a.starred ? 1 : 0));
      break;
    case 'recent':
    default:
      sorted.sort((a, b) => new Date(b.created || 0) - new Date(a.created || 0));
  }

  return sorted;
}

let snippetListenerActive = false;

async function syncVault() {
  if (!firebaseAPI || !user) {
    loadVault();
    return;
  }

  // Only start one listener, not multiple
  if (snippetListenerActive) return;
  snippetListenerActive = true;

  firebaseAPI.listenForSnippets((snippets) => {
    LocalStorage.setSnippets(snippets);
    loadVault();
    updateDashboardStats(snippets);

    // Update sync dot in topbar
    const dot = document.getElementById('firebase-sync-dot');
    const txt = document.getElementById('firebase-sync-txt');
    if (dot) { dot.classList.remove('offline'); dot.classList.add('online'); }
    if (txt) txt.textContent = 'Live · ' + snippets.length + ' snippets';
  });
}

function loadVault() {
  const allSnippets = LocalStorage.getSnippets();
  let filteredSnippets = [...allSnippets];

  const vaultList = document.getElementById('vault-list');
  const dashList = document.getElementById('dashboard-recent-list');
  const searchIn = document.getElementById('vault-search-in');
  const langSel = document.getElementById('vault-lang-sel');

  // Apply Folder Filter
  if (currentVaultFolder === 'starred') {
    filteredSnippets = allSnippets.filter(s => s.starred);
  } else if (currentVaultFolder === 'recent') {
    const twoDaysAgo = Date.now() - (48 * 60 * 60 * 1000);
    filteredSnippets = allSnippets.filter(s => {
      const created = s.created ? new Date(s.created).getTime() : 0;
      return created > twoDaysAgo;
    });
  }

  // Apply Search/Lang Filters
  if (searchIn && langSel) {
    const q = searchIn.value.toLowerCase();
    const lang = langSel.value.toLowerCase();
    filteredSnippets = filteredSnippets.filter(s => {
      const sTitle = s.title || "";
      const sCode = s.code || "";
      const sTags = s.tags || "";
      const sLang = s.language || "Plain";

      const matchSearch = sTitle.toLowerCase().includes(q) ||
        sCode.toLowerCase().includes(q) ||
        sTags.toLowerCase().includes(q);
      const matchLang = lang === 'all' || sLang.toLowerCase() === lang;
      return matchSearch && matchLang;
    });
  }

  // Apply Sorting
  filteredSnippets = sortSnippets(filteredSnippets);

  // Update Vault Page
  if (vaultList) {
    if (filteredSnippets.length === 0) {
      vaultList.innerHTML = '<div style="text-align:center;padding:40px;color:var(--t4);font-family:var(--mono);font-size:12px">No snippets found matching your filters.</div>';
    } else {
      vaultList.innerHTML = filteredSnippets.map(s => renderSnippet(s)).join('');
    }
  }

  // Update Dashboard Recent List
  if (dashList) {
    const recent = allSnippets.slice(-3).reverse();
    if (recent.length === 0) {
      dashList.innerHTML = `<div style="text-align:center; padding:40px 0; color:var(--t4)"><div style="font-size:32px; margin-bottom:10px; opacity:0.3"></div><div style="font-family:var(--mono); font-size:11px">Your recent activity will appear here.</div></div>`;
    } else {
      dashList.innerHTML = recent.map(s => renderSnippet(s)).join('');
    }
  }

  // Update Vault Page Stats
  const totalCount = document.getElementById('vault-total-count');
  const langHdrCount = document.getElementById('vault-lang-count');
  const statTotal = document.getElementById('stat-total');
  const statStarred = document.getElementById('stat-starred');
  const statLangs = document.getElementById('stat-langs');
  const statMatches = document.getElementById('stat-matches');

  if (totalCount) totalCount.textContent = allSnippets.length;
  if (langHdrCount) {
    const langs = [...new Set(allSnippets.map(s => s.language))];
    langHdrCount.textContent = langs.length;
  }

  // Update vault stats bar
  if (statTotal) statTotal.textContent = allSnippets.length;
  if (statStarred) statStarred.textContent = allSnippets.filter(s => s.starred).length;
  if (statLangs) {
    const langs = [...new Set(allSnippets.map(s => s.language))];
    statLangs.textContent = langs.length;
  }
  if (statMatches) statMatches.textContent = filteredSnippets.length;

  // Update Global Stats
  updateXPUI();

  // Phase 5: Re-highlight syntax and render newly injected icons
  setTimeout(() => {
    if (window.Prism) Prism.highlightAll();
    if (window.lucide) lucide.createIcons();
  }, 10);
}



// Theme Logic
function initThemeCards() {
  document.querySelectorAll('.theme-card').forEach(card => {
    card.onclick = () => {
      if (card.classList.contains('locked')) {
        toast('Lock icon: Reach Level 10 to unlock this theme!');
        return;
      }
      document.querySelectorAll('.theme-card').forEach(c => {
        c.classList.remove('active');
        c.style.borderColor = 'transparent';
      });
      card.classList.add('active');
      card.style.borderColor = 'var(--g)';
      toast('Theme applied to session!');
    };
  });
}

function renderSnippet(s) {
  const code = s.code || "";
  const title = s.title || "Untitled Snippet";
  const lang = s.language || s.lang || "Plain";
  const tags = s.tags || "";

  const escapedCode = code.replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const langColor = getLangColor(lang);

  // Calculate snippet difficulty based on code length and complexity
  let difficulty = 'easy';
  const codeLength = code.length;
  if (codeLength > 500) difficulty = 'hard';
  else if (codeLength > 200) difficulty = 'medium';

  // Get usage count
  const usageCount = vaultUsageTracker[s.id] || 0;

  // Format date
  const created = s.created ? new Date(s.created).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : 'N/A';

  return `
    <div class="snip-card" onclick="viewSnip('${s.id}')" style="border-left: 3px solid ${langColor}40; cursor:pointer">
      <div class="snip-header">
        <div class="snip-title">${title}</div>
        <div style="display:flex; gap:6px; align-items:center">
          <button class="ic-btn" onclick="toggleStar('${s.id}', event)" style="background:none; border:none; padding:0; color:${s.starred ? 'var(--y)' : 'var(--t4)'}">
            <i data-lucide="star" style="width:14px;height:14px;${s.starred ? 'fill:currentColor' : ''}"></i>
          </button>
          <span class="lang-badge" style="background:${langColor}15; color:${langColor}; border:0.5px solid ${langColor}40">${lang}</span>
        </div>
      </div>
      <div class="snip-code" style="border-color:${langColor}10; max-height:100px; overflow:hidden">
        <pre style="margin:0; padding:0; background:none;"><code class="language-${lang.toLowerCase()}">${escapedCode}</code></pre>
      </div>
      <div class="snip-meta">
        <div class="snip-meta-item">
          <span class="snip-difficulty ${difficulty}">${difficulty.charAt(0).toUpperCase() + difficulty.slice(1)}</span>
        </div>
        <div class="snip-meta-item" style="display:flex;align-items:center;gap:4px"><i data-lucide="calendar" style="width:10px;height:10px"></i> ${created}</div>
        <div class="snip-meta-item snip-usage-count" style="display:flex;align-items:center;gap:4px"><i data-lucide="activity" style="width:10px;height:10px"></i> Used ${usageCount}x</div>
      </div>
      <div class="snip-foot">
        <div class="snip-tags">${tags || '#' + lang.toLowerCase()}</div>
        <div class="snip-actions" style="display:flex; gap:4px">
           <button class="ic-btn" onclick="copySnip('${s.id}', event)" title="Copy"><i data-lucide="copy" style="width:14px;height:14px"></i></button>
           <button class="ic-btn" onclick="deleteSnippet('${s.id}', event)" title="Delete" style="color:var(--r)"><i data-lucide="trash-2" style="width:14px;height:14px"></i></button>
        </div>
      </div>
    </div>
  `;
}

// ========== AI REFINE ==========
let _lastRefinedCode = '';

async function refineWithAI(snippetId, event) {
  if (event) event.stopPropagation();

  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === snippetId);
  if (!snip) { toast('Snippet not found'); return; }

  _lastRefinedCode = '';
  toast('\u2726 Sending to AI...');

  const modal = document.getElementById('modal-inner');
  const bg = document.getElementById('modal-bg');
  if (!modal || !bg) { toast('UI error: modal not found'); return; }

  modal.innerHTML = `
    <div class="modal-title">\u2726 AI Refining: ${snip.title}</div>
    <div style="font-family:var(--mono);font-size:10px;color:var(--p);margin-bottom:12px">Analyzing ${snip.language || 'code'} snippet...</div>
    <div style="background:var(--bg0);border-radius:6px;padding:14px;font-family:var(--mono);font-size:11px;color:var(--t3);min-height:80px" id="refine-result">
      \u23f3 Processing...
    </div>
    <button class="form-btn cancel" onclick="closeModal()" style="margin-top:12px">Close</button>
  `;
  bg.classList.add('open');

  try {
    const result = await api.refineSnippet(snip.code || '', snip.language || 'code');
    const resultEl = document.getElementById('refine-result');
    if (!resultEl) return;

    const refined = result.fixed_code || '';
    const explanation = result.explanation || 'Refinement complete.';
    _lastRefinedCode = refined || snip.code || '';

    resultEl.innerHTML = `
      <div style="font-size:11px;color:var(--t2);margin-bottom:10px;line-height:1.6">${explanation.replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g,'<br>')}</div>
      ${refined ? `
        <div style="font-family:var(--mono);font-size:9px;color:var(--t4);letter-spacing:1px;margin-bottom:6px">REFINED CODE</div>
        <pre style="background:var(--bg0);border:0.5px solid var(--bd2);border-radius:4px;padding:10px;color:var(--c);font-size:11px;overflow-x:auto;white-space:pre-wrap">${refined.replace(/</g,'&lt;').replace(/>/g,'&gt;')}</pre>
        <button class="form-btn" onclick="copyRefinedCode()">\u2398 Copy Refined Code</button>
      ` : '<div style="color:var(--t4);font-size:11px;margin-top:8px">No code block returned. See explanation above.</div>'}
    `;
    toast('\u2726 AI refinement complete!');
    addXP(10);
  } catch(err) {
    const resultEl = document.getElementById('refine-result');
    if (resultEl) resultEl.innerHTML = `<span style="color:var(--r)">\u274c Error: ${err.message}</span>`;
  }
}

function copyRefinedCode() {
  if (!_lastRefinedCode) { toast('Nothing to copy'); return; }
  navigator.clipboard.writeText(_lastRefinedCode)
    .then(() => toast('Refined code copied!'))
    .catch(() => toast('Copy failed — try selecting the code manually'));
}


function createSnipCard(snip) {
  const card = document.createElement('div');
  card.className = 'snip-card';
  card.onclick = () => openSnipDetail(snip);

  const langColor = getLangColor(snip.language);

  card.innerHTML = `
    <div class="snip-header">
      <span class="snip-title">${snip.title}</span>
      <span class="lang-badge" style="background:${langColor}20; color:${langColor}; border:0.5px solid ${langColor}40">${snip.language}</span>
    </div>
    <div class="snip-code">${snip.code.substring(0, 100)}${snip.code.length > 100 ? '...' : ''}</div>
    <div class="snip-foot" id="foot-${snip.id}">
      <div class="snip-actions">
        <button class="ic-btn beam" onclick="event.stopPropagation();beamSnip('${snip.title}')"></button>
        <button class="ic-btn" onclick="event.stopPropagation();deleteSnippet('${snip.id}')" style="color:var(--r)"></button>
      </div>
    </div>
  `;

  // Dynamic Tags
  const foot = card.querySelector('.snip-foot');
  const tagsDiv = document.createElement('div');
  tagsDiv.className = 'snip-tags';
  if (snip.tags) {
    snip.tags.split(',').forEach(tag => {
      const span = document.createElement('span');
      span.style = 'cursor:pointer;margin-right:5px;opacity:0.7';
      span.textContent = '#' + tag.trim();
      span.onclick = (e) => {
        e.stopPropagation();
        const searchIn = document.getElementById('vault-search-in');
        if (searchIn) { searchIn.value = tag.trim(); loadVault(); goPage('vault'); }
      };
      tagsDiv.appendChild(span);
    });
  } else {
    tagsDiv.textContent = '#snippet';
  }
  foot.prepend(tagsDiv);

  return card;
}

async function toggleStar(id, event) {
  if (event) event.stopPropagation();
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === id);
  if (snip) {
    snip.starred = !snip.starred;
    LocalStorage.setSnippets(snippets);
    loadVault();
    toast(snip.starred ? 'Added to Starred' : 'Removed from Starred');
  }
}

async function deleteSnippet(id, event) {
  if (event) event.stopPropagation();
  if (confirm('Are you sure you want to delete this snippet?')) {
    try {
      await LocalStorage.removeSnippet(id);
      loadVault();
      toast('Snippet deleted ');
    } catch (err) {
      toast('Error deleting snippet');
    }
  }
}

function getLangColor(lang) {
  const colors = {
    'Java': 'var(--p)',
    'Kotlin': 'var(--c)',
    'Python': 'var(--y)',
    'JavaScript': 'var(--g)',
    'SQL': 'var(--pk)'
  };
  return colors[lang] || 'var(--t3)';
}

let activeEditorSnippet = null;

function openSnipDetail(snip) {
  goPage('vault');
  activeEditorSnippet = snip;
  const langColor = getLangColor(snip.language);
  const escapedCode = snip.code.replace(/</g, '&lt;').replace(/>/g, '&gt;');
  
  // Update Tab
  const tabTitle = document.getElementById('tab-title');
  if (tabTitle) tabTitle.textContent = snip.title;
  
  // Update Toolbar
  const editorTitle = document.getElementById('editor-title');
  if (editorTitle) editorTitle.textContent = snip.title;
  
  const editorLang = document.getElementById('editor-lang');
  if (editorLang) {
    editorLang.textContent = snip.language;
    editorLang.style.background = langColor + '15';
    editorLang.style.color = langColor;
  }

  const statusLang = document.getElementById('status-lang-mode');
  if (statusLang) statusLang.textContent = snip.language;
  
  // Update Code Area
  const editorCode = document.getElementById('editor-code');
  if (editorCode) {
    let prismLang = 'language-markup';
    const l = snip.language.toLowerCase();
    if (l.includes('java')) prismLang = 'language-java';
    else if (l.includes('kotlin')) prismLang = 'language-kotlin';
    else if (l.includes('python')) prismLang = 'language-python';
    else if (l.includes('javascript') || l.includes('js')) prismLang = 'language-javascript';
    else if (l.includes('sql')) prismLang = 'language-sql';
    
    editorCode.className = prismLang;
    editorCode.innerHTML = escapedCode;
    
    // Trigger Prism highlighting
    if (window.Prism) {
      Prism.highlightElement(editorCode);
    }
  }
}

function copilotRefine() {
  if (!activeEditorSnippet) {
    toast('No snippet active in editor.');
    return;
  }
  sendToAI(activeEditorSnippet.title);
}

function copyEditorCode() {
  if (!activeEditorSnippet) return;
  copySnip(activeEditorSnippet.id);
}

function beamCurrentEditor() {
  if (!activeEditorSnippet) return;
  beamToPhone(activeEditorSnippet.id);
}

function deleteEditorSnip() {
  if (!activeEditorSnippet) {
    toast('No snippet selected');
    return;
  }
  
  if (confirm(`Are you sure you want to delete "${activeEditorSnippet.title}"?`)) {
    deleteSnippet(activeEditorSnippet.id);
    // Clear editor
    document.getElementById('tab-title').textContent = 'Welcome.md';
    document.getElementById('editor-title').textContent = 'Welcome to Vault';
    document.getElementById('editor-lang').textContent = 'Markdown';
    document.getElementById('editor-code').innerHTML = 'Select a snippet from the left panel to open it in this workspace.';
    activeEditorSnippet = null;
    toast('Snippet deleted from Vault');
  }
}

async function beamToPhone(id) {
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === id);
  if (!snip) return;

  if (!user) {
    toast('Login to beam to phone!');
    return;
  }

  toast(`Beaming "${snip.title}" to phone...`);
  const sessionId = user.uid.substring(0, 6).toUpperCase();

  if (firebaseAPI) {
    try {
      await firebaseAPI.sendBeam(sessionId, {
        title: snip.title,
        code: snip.code,
        language: snip.language,
        timestamp: new Date().toISOString(),
        source: 'Web Portal'
      });
      toast(' Sent to phone!');
      addXP(10);
    } catch (err) {
      toast('Beam failed. Check connection.');
    }
  }
}

function shareViaQR(id) {
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === id);
  if (!snip) return;

  const qrDiv = document.getElementById('snip-qr-share');
  const qrImg = document.getElementById('snip-qr-img');

  if (qrDiv && qrImg) {
    const isVisible = qrDiv.style.display === 'block';
    if (isVisible) {
      qrDiv.style.display = 'none';
    } else {
      // Use a format the phone app can understand for direct import
      const payload = JSON.stringify({ t: snip.title, c: snip.code, l: snip.language });
      qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${encodeURIComponent(payload)}`;
      qrDiv.style.display = 'block';
      toast('QR Code Generated');
    }
  }
}

// Local Storage Manager with Firebase Fallback
const LocalStorage = {
  storageKey: 'snipit_vault',

  getSnippets() {
    const data = localStorage.getItem(this.storageKey);
    return data ? JSON.parse(data) : [];
  },

  setSnippets(snippets) {
    localStorage.setItem(this.storageKey, JSON.stringify(snippets));
  },

  async addSnippet(snippet) {
    // Set default created date if missing
    if (!snippet.created) snippet.created = new Date().toISOString();
    if (!snippet.id) snippet.id = Date.now().toString();

    // Try Firebase first
    if (firebaseAPI && firebaseAPI.isOnline && user) {
      try {
        await firebaseAPI.addSnippet(snippet);
        toast('Snippet saved to Firebase! +15 XP ');
      } catch (err) {
        console.warn('Firebase save failed, using local storage:', err);
        return this._addLocal(snippet);
      }
    } else {
      return this._addLocal(snippet);
    }
    return snippet;
  },

  _addLocal(snippet) {
    const snippets = this.getSnippets();
    snippet.id = snippet.id || Date.now().toString();
    snippet.created = snippet.created || new Date().toISOString();
    snippet.synced = false; // Mark as not synced to Firebase
    snippets.push(snippet);
    localStorage.setItem(this.storageKey, JSON.stringify(snippets));
    return snippet;
  },

  async removeSnippet(id) {
    // Try Firebase first
    if (firebaseAPI && firebaseAPI.isOnline && user) {
      try {
        await firebaseAPI.deleteSnippet(id);
      } catch (err) {
        console.warn('Firebase delete failed, using local storage:', err);
      }
    }

    // Also remove from local storage
    let snippets = this.getSnippets();
    snippets = snippets.filter(s => s.id !== id);
    localStorage.setItem(this.storageKey, JSON.stringify(snippets));
  },

  async exportVault() {
    let snippets;

    // Try to get from Firebase first
    if (firebaseAPI && firebaseAPI.isOnline && user) {
      try {
        snippets = await firebaseAPI.getSnippets();
      } catch (err) {
        console.warn('Firebase export failed, using local storage:', err);
        snippets = this.getSnippets();
      }
    } else {
      snippets = this.getSnippets();
    }

    const json = JSON.stringify(snippets, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `snipit_vault_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    toast('Vault exported!');
  },

  async importVault(file) {
    try {
      const text = await file.text();
      const snippets = JSON.parse(text);

      for (const snippet of snippets) {
        await this.addSnippet(snippet);
      }
      loadVault();
      toast('Vault imported successfully! ');
    } catch (err) {
      toast('Error importing vault');
      console.error(err);
    }
  }
};

// ========== VAULT FUNCTIONS ==========
function copySnip(id, event) {
  if (event) event.stopPropagation();
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === id);
  if (snip) {
    // Track usage
    vaultUsageTracker[id] = (vaultUsageTracker[id] || 0) + 1;
    localStorage.setItem('snipit_vault_usage', JSON.stringify(vaultUsageTracker));

    navigator.clipboard.writeText(snip.code).then(() => {
      toast('Copied to clipboard! ');
      loadVault(); // Refresh to show updated usage count
      addXP(5); // +5 XP for copying
    });
  }
}

function viewSnip(id) {
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.id === id);
  if (snip) {
    // Track usage
    vaultUsageTracker[id] = (vaultUsageTracker[id] || 0) + 1;
    localStorage.setItem('snipit_vault_usage', JSON.stringify(vaultUsageTracker));
    openSnipDetail(snip);
  }
}

function filterByTag(tag) {
  const searchIn = document.getElementById('vault-search-in');
  if (searchIn) {
    searchIn.value = tag === 'all' ? '' : tag;
    loadVault();
  }
  // Update chip styles
  document.querySelectorAll('#vault-chips .chip').forEach(chip => {
    chip.classList.remove('active');
    chip.style.cssText = '';
  });
  if (tag !== 'all') {
    document.querySelectorAll('#vault-chips .chip').forEach(chip => {
      if (chip.textContent.includes(tag)) {
        chip.classList.add('active');
        chip.style.cssText = 'background:var(--g)15; border-color:var(--g)40; color:var(--g)';
      }
    });
  }
}

function sendToAI(title) {
  goPage('ai');
  const snippets = LocalStorage.getSnippets();
  const snip = snippets.find(s => s.title === title);
  if (snip) {
    const aiIn = document.getElementById('ai-in');
    if (aiIn) {
      aiIn.value = 'Analyze and improve this code:\n\n' + snip.code;
      aiIn.focus();
    }
  }
}

// Initialize vault with sample data on first load
function initVaultSamples() {
  const existing = LocalStorage.getSnippets();
  if (existing.length === 0) {
    const samples = [
      {
        id: '1',
        title: 'HashMap Usage in Java',
        code: 'HashMap<String, Integer> map = new HashMap<>();\nmap.put("apple", 5);\nmap.put("banana", 3);\nfor (Map.Entry<String, Integer> entry : map.entrySet()) {\n  System.out.println(entry.getKey() + ": " + entry.getValue());\n}',
        language: 'Java',
        tags: 'Collections,HashMap,Data-Structures',
        created: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
        starred: true
      },
      {
        id: '2',
        title: 'Kotlin Coroutines Launch',
        code: 'GlobalScope.launch {\n  val result = withContext(Dispatchers.IO) {\n    // Network call\n    fetchData()\n  }\n  withContext(Dispatchers.Main) {\n    updateUI(result)\n  }\n}',
        language: 'Kotlin',
        tags: 'Coroutines,Async,Mobile',
        created: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
        starred: false
      },
      {
        id: '3',
        title: 'Firebase Realtime DB Query',
        code: 'const ref = firebase.database().ref("users");\nref.on("value", snapshot => {\n  const data = snapshot.val();\n  console.log(data);\n});',
        language: 'JavaScript',
        tags: 'Firebase,Database,Backend',
        created: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
        starred: true
      },
      {
        id: '4',
        title: 'Python List Comprehension',
        code: 'numbers = [1, 2, 3, 4, 5]\nsquares = [x**2 for x in numbers if x % 2 == 0]\nprint(squares)  # [4, 16]',
        language: 'Python',
        tags: 'Python,List,Functional',
        created: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
        starred: false
      },
      {
        id: '5',
        title: 'SQL Join Example',
        code: 'SELECT users.name, orders.total\nFROM users\nINNER JOIN orders ON users.id = orders.user_id\nWHERE orders.total > 100\nORDER BY orders.total DESC;',
        language: 'SQL',
        tags: 'Database,Query,SQL',
        created: new Date().toISOString(),
        starred: false
      }
    ];

    samples.forEach(sample => LocalStorage.setSnippets([...existing, sample]));
    toast('Sample snippets loaded!');
  }
}

// QR Generation is now handled at line 1181

// Initializers
document.addEventListener('DOMContentLoaded', () => {
  initializeFirebase();

  // Numpad Support
  document.addEventListener('keydown', (e) => {
    if (document.getElementById('page-beam').classList.contains('active')) {
      if (e.key >= '0' && e.key <= '9') pressPin(e.key);
      if (e.key === 'Backspace') pressPin('del');
      if (e.key === 'Escape') pressPin('clr');
    }
  });
});

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
  console.log('DOM loaded, showing login page');

  // Initialize vault with samples if empty
  initVaultSamples();

  // Show login page immediately (will be hidden if auth state is detected)
  showLoginPage();

  // Clear any stuck loading state
  hideAuthLoading();

  // Initialize Firebase in background
  console.log('Starting Firebase initialization...');
  initializeFirebase()
    .then(success => {
      console.log('Firebase init result:', success);
      if (!success) {
        // Only show error if not in local dev/demo mode
        console.warn('Firebase connection failed. Local mode enabled.');
      }
    })
    .catch(err => {
      console.error('Firebase init error:', err);
    });
});

// Theme Toggle
function toggleTheme() {
  document.body.classList.toggle('light-mode');
  const isLight = document.body.classList.contains('light-mode');
  localStorage.setItem('snipit_theme', isLight ? 'light' : 'dark');
  
  // Update toggle button icon
  const btn = document.getElementById('theme-toggle-btn');
  if (btn) {
    btn.innerHTML = isLight 
      ? '<i data-lucide="sun" style="width:14px; height:14px"></i><span style="font-size:10px">Light</span>'
      : '<i data-lucide="moon" style="width:14px; height:14px"></i><span style="font-size:10px">Dark</span>';
    lucide.createIcons();
  }
  
  toast(isLight ? 'Light Mode Activated' : 'Dark Mode Activated');
}

// PIN Input handling (Physical Keyboard)
document.addEventListener('keydown', (e) => {
  const activePage = document.querySelector('.page.active');
  if (activePage && activePage.id === 'page-beam') {
    if (e.key >= '0' && e.key <= '9' && pinDigits.length < 6) {
      pressPin(e.key);
    } else if (e.key === 'Backspace') {
      pressPin('del');
    }
  }
});



// Initializations
window.addEventListener('load', () => {
  // Add demo snippet if empty for new users
  const existing = LocalStorage.getSnippets();
  if (existing.length === 0) {
    LocalStorage._addLocal({
      title: 'Welcome to SnipIT',
      language: 'JavaScript',
      tags: 'Tutorial, Start',
      code: '// Welcome to your new IDE-style vault!\n// Click the "+ New Snip" button to add your own code.\nconsole.log("Happy Coding!");'
    });
  }

  loadVault();
  updateXPUI();
  checkBeamStatus();
  initThemeCards();

  // Restore last beamed
  const lastB = localStorage.getItem('snipit_last_beamed');
  if (lastB) {
    const b = JSON.parse(lastB);
    updateLastBeamed(b.title, b.code);
  }

  // Start streak check
  updateStreak();

  setInterval(checkBeamStatus, 1000);
});

window.addEventListener('DOMContentLoaded', () => {
  const savedTheme = localStorage.getItem('snipit_theme');
  if (savedTheme === 'light') document.body.classList.add('light-mode');

  // Keyboard Shortcuts
  document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + K: Focus search in vault
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault();
      const searchInput = document.getElementById('vault-search-in');
      if (searchInput) searchInput.focus();
    }

    // Ctrl/Cmd + N: New snippet
    if ((e.ctrlKey || e.metaKey) && e.key === 'n' && document.getElementById('page-vault')?.classList.contains('active')) {
      e.preventDefault();
      openModal('new-snip');
    }

    // Ctrl/Cmd + E: Export vault
    if ((e.ctrlKey || e.metaKey) && e.key === 'e' && document.getElementById('page-vault')?.classList.contains('active')) {
      e.preventDefault();
      LocalStorage.exportVault();
    }
  });
});

// ==========================================
// COMMAND PALETTE (CTRL+K)
// ==========================================

// Global Shortcut Listener
document.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault(); // Prevent browser default search
    openCmd();
  }
  if (e.key === 'Escape') {
    closeCmd();
  }
});

function openCmd() {
  const bg = document.getElementById('cmd-bg');
  const input = document.getElementById('cmd-input');
  if (bg) {
    bg.classList.add('open');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 100);
      filterCmd();
    }
  }
}

function closeCmd() {
  const bg = document.getElementById('cmd-bg');
  if (bg) bg.classList.remove('open');
}

function filterCmd() {
  const input = document.getElementById('cmd-input').value.toLowerCase();
  const resultsEl = document.getElementById('cmd-results');
  if (!resultsEl) return;
  
  if (!input) {
    resultsEl.innerHTML = `<div style="padding: 12px 16px; color:var(--t4); font-family:var(--mono); font-size:11px; text-align:center;">Type to search Snippets & Dex...</div>`;
    return;
  }
  
  const snippets = LocalStorage.getSnippets();
  // We assume handbook is loaded and stored globally, but if not we try to fetch it or skip
  let handbook = [];
  try {
     handbook = typeof devDexHandbook !== 'undefined' ? devDexHandbook : window.devDexHandbook || [];
  } catch(e) {}
  
  const matchedSnips = snippets.filter(s => 
    (s.title && s.title.toLowerCase().includes(input)) || 
    (s.language && s.language.toLowerCase().includes(input)) ||
    (s.tags && s.tags.toLowerCase().includes(input))
  ).slice(0, 5);
  
  const matchedDex = handbook.filter(item => 
    (item.title && item.title.toLowerCase().includes(input)) || 
    (item.category && item.category.toLowerCase().includes(input)) ||
    (item.description && item.description.toLowerCase().includes(input))
  ).slice(0, 5);
  
  let html = '';
  
  if (matchedSnips.length > 0) {
    html += `<div style="padding: 8px 12px; font-size: 10px; font-weight: 600; color: var(--p); letter-spacing: 1px;">VAULT SNIPPETS</div>`;
    matchedSnips.forEach(s => {
      html += `
        <div style="padding: 10px 12px; cursor: pointer; border-radius: 6px; margin-bottom: 4px; background: var(--bg2); display:flex; align-items:center; gap: 10px;" 
             onmouseover="this.style.background='var(--bg3)'" onmouseout="this.style.background='var(--bg2)'"
             onclick="closeCmd(); goPage('vault'); setTimeout(() => openSnipDetail(LocalStorage.getSnippets().find(x=>x.id==='${s.id}')), 200)">
          <i data-lucide="file-code" style="width: 14px; height: 14px; color: var(--t3);"></i>
          <div>
            <div style="font-size: 13px; color: var(--t1);">${s.title}</div>
            <div style="font-size: 10px; color: var(--t4); font-family: var(--mono); margin-top: 2px;">${s.language}</div>
          </div>
        </div>
      `;
    });
  }
  
  if (matchedDex.length > 0) {
    html += `<div style="padding: 8px 12px; font-size: 10px; font-weight: 600; color: var(--c); letter-spacing: 1px; margin-top: 8px;">DEV-DEX</div>`;
    matchedDex.forEach(d => {
      html += `
        <div style="padding: 10px 12px; cursor: pointer; border-radius: 6px; margin-bottom: 4px; background: var(--bg2); display:flex; align-items:center; gap: 10px;" 
             onmouseover="this.style.background='var(--bg3)'" onmouseout="this.style.background='var(--bg2)'"
             onclick="closeCmd(); goPage('devdex'); setTimeout(() => { document.getElementById('dex-search').value = '${d.title}'; renderDevDex(); }, 100)">
          <i data-lucide="book-open" style="width: 14px; height: 14px; color: var(--t3);"></i>
          <div>
            <div style="font-size: 13px; color: var(--t1);">${d.title}</div>
            <div style="font-size: 10px; color: var(--t4); font-family: var(--mono); margin-top: 2px;">${d.category}</div>
          </div>
        </div>
      `;
    });
  }
  
  if (!html) {
    html = `<div style="padding: 24px; text-align: center; color: var(--t4); font-size: 12px;">No results found for "${input}"</div>`;
  }
  
  resultsEl.innerHTML = html;
  if (window.lucide) lucide.createIcons();
}
