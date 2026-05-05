// ============================================
// FIREBASE CONFIGURATION - SETUP INSTRUCTIONS
// ============================================
//
// STEP 1: Get your Firebase config from:
//   https://console.firebase.google.com
//   → Project Settings → Your Apps → Web Config
//
// STEP 2: Fill in the values below (between the quotes):
//
// STEP 3: Uncomment the configuration at the bottom
// ============================================

// Your Firebase Configuration
// Get these values from Firebase Console → Project Settings
const firebaseConfig = {
  apiKey: "AIzaSyD_YOUR_API_KEY_HERE",           // Copy from Firebase console
  authDomain: "your-project.firebaseapp.com",    // your-project = your Firebase project ID
  databaseURL: "https://your-project-default-rtdb.firebaseio.com",  // Replace "your-project"
  projectId: "your-project-id",                   // Your Firebase project ID
  storageBucket: "your-project.appspot.com",      // Replace "your-project"
  messagingSenderId: "123456789012",              // Numeric ID from Firebase
  appId: "1:123456789012:web:abc123def456ghi"    // Full app ID from Firebase
};

// EXAMPLE (for reference only - don't use these values):
/*
const firebaseConfig = {
  apiKey: "AIzaSyD_abc123XYZ_def456-example",
  authDomain: "snippit-tutorial.firebaseapp.com",
  databaseURL: "https://snippit-tutorial-default-rtdb.firebaseio.com",
  projectId: "snippit-tutorial",
  storageBucket: "snippit-tutorial.appspot.com",
  messagingSenderId: "987654321098",
  appId: "1:987654321098:web:xyz789abc012def"
};
*/

// ============================================
// INSTALLATION INSTRUCTIONS:
// ============================================
//
// 1. Go to: https://console.firebase.google.com
// 2. Click on your "SNIPPIT" project
// 3. Click the gear icon (⚙️) = Project Settings
// 4. Scroll down to "Your Apps" section
// 5. Click on the web icon (</>) next to your app name
// 6. You'll see a code block with firebaseConfig
// 7. Copy all the values and paste them above
// 8. Save this file
// 9. Deploy to Netlify
//
// ============================================
// TEST YOUR CONFIG:
// ============================================
//
// 1. Open your site in browser
// 2. Press F12 (open DevTools)
// 3. Go to Console tab
// 4. Look for: "Firebase initialized successfully"
// 5. If you see errors, check your config values
//
// ============================================
