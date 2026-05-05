# Deployment Checklist - SnipIT Web Portal to Netlify

## Pre-Deployment Checklist

### Firebase Setup
- [ ] Firebase project created (SNIPPIT)
- [ ] Realtime Database enabled
- [ ] Web app created in Firebase
- [ ] Firebase config copied from Project Settings
- [ ] `firebase-config.js` updated with real credentials
- [ ] Database rules configured and published (see FIREBASE_NETLIFY_SETUP.md)
- [ ] Anonymous authentication enabled

### Code Ready
- [ ] All code in `web/` folder
- [ ] No console errors when running locally
- [ ] Test snippets can be saved/loaded
- [ ] Firebase connection shows "ONLINE"
- [ ] Try Beam Station simulation (PIN: 482951)
- [ ] AI chat responds

### Security Check
- [ ] No hardcoded secrets in version control
- [ ] Firebase rules prevent unauthorized access
- [ ] Database restricted to authenticated users only
- [ ] API key restrictions set in Firebase Console

### Netlify Preparation
- [ ] Netlify account created (netlify.com)
- [ ] GitHub repository created (optional but recommended)
- [ ] All files in `web/` ready to deploy
- [ ] No large files (>5MB) in folder

## Deployment Steps

### Step 1: Final Testing Locally
```bash
# Make sure everything works before deployment
cd c:\Users\Chuchay\StudioProjects\SnipIT_1\web
# Open index.html in browser
# Test: Add snippet, Beam Station, AI Chat
```

### Step 2: Configure Firebase
1. Open `web/js/firebase-config.js`
2. Update the `firebaseConfig` object with your Firebase credentials:
   ```
   apiKey: "AIzaSyD..." (from Firebase Console)
   authDomain: "your-project.firebaseapp.com"
   databaseURL: "https://your-project-default-rtdb.firebaseio.com"
   projectId: "your-project"
   storageBucket: "your-project.appspot.com"
   messagingSenderId: "..."
   appId: "..."
   ```

### Step 3: Deploy to Netlify

**Option A: Direct Upload (Fastest)**
1. Go to https://netlify.com
2. Drag & drop your `web` folder
3. Done! Get your Netlify URL

**Option B: Git + Netlify (Recommended)**
1. Initialize Git:
   ```bash
   cd SnipIT_1
   git init
   git add web/
   git commit -m "Add SnipIT web portal"
   ```

2. Push to GitHub:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/snipit.git
   git branch -M main
   git push -u origin main
   ```

3. Connect to Netlify:
   - Go to netlify.com → "New site from Git"
   - Select GitHub and your repository
   - Set publish directory to `web`
   - Click "Deploy"

**Option C: Netlify CLI (For developers)**
```bash
npm install -g netlify-cli
cd SnipIT_1
netlify deploy --dir web --prod
```

### Step 4: Verify Deployment
1. Open your Netlify URL in browser
2. Open DevTools (F12)
3. Check Console tab for:
   ```
   ✓ Firebase initialized successfully
   ✓ Firebase user authenticated: [uid]
   ✓ Firebase connection: ONLINE
   ```
4. Test main features:
   - [ ] Add a new snippet
   - [ ] Check Firebase Console to see data
   - [ ] Verify data persists on page refresh
   - [ ] Test Beam Station
   - [ ] Test AI Chat

### Step 5: Share Your Portal
Your SnipIT Web Portal is now live! Share the Netlify URL with users.

Example: `https://snipit-portal.netlify.app`

## Post-Deployment Checklist

### Monitoring
- [ ] Check Netlify analytics dashboard
- [ ] Monitor Firebase usage (console.firebase.google.com)
- [ ] Set up Firebase alerts for quota limits
- [ ] Track error logs if issues arise

### Optimization
- [ ] Enable Netlify CDN for faster loading
- [ ] Set up Netlify Analytics
- [ ] Enable automatic deployments on Git push
- [ ] Configure custom domain (if desired)

### Maintenance
- [ ] Regularly backup Firebase data
- [ ] Monitor Firebase costs
- [ ] Update dependencies when needed
- [ ] Test functionality monthly

## Troubleshooting Guide

### "Firebase not initialized"
**Solution:**
- Check `firebase-config.js` has correct values
- Verify values copied from Firebase console correctly
- Open Console tab (F12) to see exact error

### "Cannot save snippets - Permission denied"
**Solution:**
- Go to Firebase Console → Realtime Database → Rules
- Verify rules are published (should say "Published" in green)
- Rules should allow `.read` and `.write` for authenticated users

### "Portal very slow"
**Solution:**
- Firebase initialization takes a moment
- Check network tab for slow requests
- Verify database region in Firebase

### "Data not showing in Firebase Console"
**Solution:**
- Refresh the Database page
- Check you're looking in correct path: `users/[uid]/snippets/`
- Verify rules allow this user to write

## Rollback Instructions

If something goes wrong after deployment:

### Using Git
```bash
git revert HEAD
git push origin main
# Netlify will automatically redeploy
```

### Using Netlify UI
1. Go to Netlify Dashboard
2. Site Settings → Deploys
3. Click "Publish deploy" on previous version

## Success Criteria

Your deployment is successful when:

✅ Portal loads without errors
✅ Firebase shows "ONLINE" status
✅ Can add new snippets
✅ Data appears in Firebase Console
✅ Data persists after page refresh
✅ All pages are accessible
✅ No console errors
✅ Beam Station simulation works
✅ AI Chat works

## Support Resources

- Firebase Docs: https://firebase.google.com/docs
- Netlify Docs: https://docs.netlify.com
- Console Errors: Press F12 → Console tab for details
- Check FIREBASE_NETLIFY_SETUP.md for detailed setup

---

**Deployment Ready?** Follow steps 1-5 above in order.
