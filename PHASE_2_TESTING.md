# 🧪 Phase 2 Testing Guide: Syntax Highlighting
## How to Test Syntax Highlighting for 150+ Languages

---

## ⚠️ IMPORTANT: Phase 1 Must Be Complete First!

### Phase 1 Completion Checklist
Before testing Phase 2, verify Phase 1 is done:

- ✅ App launches without crashes
- ✅ All 7 navigation tabs work (Vault, Dex, Snap, Beam, AI, XP, Profile)
- ✅ ProfileFragment shows your real user data
- ✅ Navigation icons are professional (green when active)
- ✅ DexFragment search works and returns results
- ✅ No null pointer exceptions anywhere

**If Phase 1 is NOT complete, go back to DO_THIS_NOW.md and finish it first!**

---

## 📋 What Phase 2 Delivers

Phase 2 adds **Syntax Highlighting** to the Vault:

| Feature | Before Phase 1 | After Phase 1 | After Phase 2 |
|---------|----------------|---------------|--------------|
| Snippets saved | ✅ | ✅ | ✅ |
| Code formatting | ❌ | ❌ | ✅ **NEW** |
| 150+ languages | ❌ | ❌ | ✅ **NEW** |
| Real-time preview | ❌ | ❌ | ✅ **NEW** |
| Language detection | ❌ | ❌ | ✅ **NEW** |

---

## 🔧 Phase 2 Implementation Overview

### What Gets Built:

1. **CodeHighlighter.java** - Bridge between Java and Prism.js
2. **CodeView.java** - Custom WebView for displaying highlighted code
3. **assets/highlighting/** - Prism.js library files
4. **fragment_vault_upgraded.xml** - New snippet card design
5. **SnipAdapter upgrades** - Show language + preview

### How It Works:

```
User enters code in EditSnipActivity
    ↓
CodeHighlighter converts to HTML
    ↓
Prism.js library highlights syntax
    ↓
CodeView displays colored code
    ↓
User sees beautiful, readable code ✅
```

---

## 🧪 Phase 2 Testing Strategy

### TEST 1: Verify Prism.js Installation
**What to test:** Prism library is correctly installed in assets

**Steps:**
1. Check `app/src/main/assets/highlighting/` directory exists
2. Verify these files are present:
   - `prism.js` (core library)
   - `prism.css` (styling)
   - `prism-*.js` (language packs for 150+ languages)

**Expected result:** All files present ✅

**If fails:**
- Check file sizes (Prism core should be ~200KB)
- Verify no corrupted downloads
- Re-download from prismjs.com

---

### TEST 2: Language Detection
**What to test:** App correctly detects code language

**Steps:**
1. Create new snippet with this code:
```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```
2. Save snippet
3. Check if language is auto-detected as "Java"

**Expected result:** 
- ✅ Language detected correctly
- ✅ Saved as `java` tag

**If fails:**
- Check CodeHighlighter.detectLanguage() logic
- Verify regex patterns for 15+ languages
- Check if language tag appears in UI

---

### TEST 3: Syntax Highlighting Display
**What to test:** Code displays with correct colors

**Steps:**
1. Open saved Java snippet from Test 2
2. Look at preview in snippet card
3. Verify:
   - Keywords are colored (public, class, static)
   - Strings are colored (green)
   - Comments are grayed out
   - Numbers are highlighted

**Expected result:**
- ✅ Code is colorfully highlighted
- ✅ Different syntax elements have different colors
- ✅ Text is still readable

**If fails:**
- Verify CSS is loaded correctly
- Check Prism theme is applied
- Review CodeView.java WebView settings

---

### TEST 4: Multiple Language Support
**What to test:** Different languages highlight correctly

**Create snippets in these languages and verify each highlights:**

| Language | Code to Use | Expected Colors |
|----------|------------|-----------------|
| **Python** | `print("Hello")` | Keywords blue, strings green |
| **JavaScript** | `console.log("Hi");` | Keywords blue, strings orange |
| **SQL** | `SELECT * FROM users;` | Keywords blue, strings green |
| **HTML** | `<div>Hello</div>` | Tags colored, text normal |
| **CSS** | `color: red;` | Properties blue, values orange |
| **Kotlin** | `fun main() {}` | Keywords blue, braces gray |
| **Go** | `fmt.Println("test")` | Keywords blue, functions colored |
| **Rust** | `fn main() {}` | Keywords blue, safe colors |
| **C++** | `std::cout << "Hi";` | Keywords blue, std colored |
| **PHP** | `echo "Hello";` | Keywords blue, strings green |

**Expected result:** Each language highlights with appropriate colors ✅

**If fails:**
- Verify Prism language pack is included
- Check SnipAdapter passes correct language
- Verify CodeView loads correct CSS theme

---

### TEST 5: Real-Time Highlighting While Typing
**What to test:** Syntax highlighting updates as user types

**Steps:**
1. Open EditSnipActivity
2. Type code line by line (don't paste)
3. Watch CodeView update colors in real-time

**Expected result:**
- ✅ Colors update as you type
- ✅ No lag or delay
- ✅ Syntax errors don't cause crashes

**If fails:**
- Check TextWatcher implementation
- Verify CodeHighlighter isn't blocking UI thread
- Profile performance in Android Profiler

---

### TEST 6: Large Code Snippets
**What to test:** Performance with large files

**Steps:**
1. Create snippet with 500+ lines of code
2. Open it in EditSnipActivity
3. Scroll through the code
4. Check performance

**Expected result:**
- ✅ Loads within 2 seconds
- ✅ Scrolling is smooth
- ✅ No "ANR" (Application Not Responding) errors

**If fails:**
- Implement lazy loading
- Use WebView native syntax instead
- Consider pagination for very large files

---

### TEST 7: Search Still Works
**What to test:** Vault search/filter works with new highlighting

**Steps:**
1. Create 5 snippets in different languages
2. Search for language (#Java)
3. Filter by language chip
4. Verify snippets appear with correct highlighting

**Expected result:**
- ✅ Search returns correct snippets
- ✅ Highlighted previews show
- ✅ Language tags visible on cards

**If fails:**
- Verify SnipAdapter wasn't broken by changes
- Check RecyclerView notifyDataSetChanged()
- Review search logic in VaultFragment

---

### TEST 8: Copy Code Still Works
**What to test:** "Copy to Clipboard" works with highlighted code

**Steps:**
1. Open highlighted snippet
2. Tap "Copy Code" button
3. Paste in text editor (like Notepad)
4. Verify code is copied (not HTML/colors)

**Expected result:**
- ✅ Plain text code copied
- ✅ No HTML markup
- ✅ Formatting preserved (spaces, indentation)

**If fails:**
- Verify copy logic gets plain text
- Check it's not copying HTML from CodeView
- Review ClipboardManager usage

---

### TEST 9: Beam (QR) Still Works
**What to test:** QR generation works with new snippet structure

**Steps:**
1. Open highlighted snippet
2. Tap "Beam IT" button
3. Generate QR code
4. Scan with phone camera

**Expected result:**
- ✅ QR code generates
- ✅ Can be scanned
- ✅ Original code transfers correctly

**If fails:**
- Verify QR generation uses plain text
- Check compression logic still works
- Review GZIP encoding

---

### TEST 10: Vault UI Still Works
**What to test:** VaultFragment isn't broken by changes

**Steps:**
1. Click "Vault" tab
2. Scroll through snippets
3. Search for a snippet
4. Filter by language
5. Create new snippet
6. Edit existing snippet
7. Delete a snippet

**Expected result:**
- ✅ All actions work
- ✅ No crashes
- ✅ Previews show highlighted code

**If fails:**
- Check SnipAdapter changes
- Verify RecyclerView not broken
- Review fragment lifecycle

---

## 📊 Phase 2 Testing Checklist

Use this while testing:

```
CATEGORY: Prism.js Installation
- [ ] assets/highlighting/ folder exists
- [ ] prism.js file present (200KB+)
- [ ] prism.css file present
- [ ] Language pack files present

CATEGORY: Core Functionality
- [ ] Java code highlights correctly
- [ ] Python code highlights correctly
- [ ] JavaScript code highlights correctly
- [ ] SQL code highlights correctly
- [ ] HTML code highlights correctly

CATEGORY: Performance
- [ ] Small snippets load < 1s
- [ ] Large snippets load < 3s
- [ ] Scrolling is smooth
- [ ] No ANR errors
- [ ] Real-time typing is responsive

CATEGORY: Integration
- [ ] Search still works
- [ ] Copy still works
- [ ] Beam/QR still works
- [ ] Delete still works
- [ ] Edit still works

CATEGORY: Edge Cases
- [ ] Empty snippets don't crash
- [ ] Very large files (1000+ lines)
- [ ] Code with special characters
- [ ] Mixed language snippets
- [ ] Snippets with no language set

CATEGORY: User Experience
- [ ] Colors are easy on eyes
- [ ] Text is readable
- [ ] Line numbers visible (if enabled)
- [ ] Code is properly indented
- [ ] Dark theme is applied
```

---

## 🎯 Phase 2 Success Criteria

### MUST HAVE (Mandatory)
- ✅ At least 50+ languages highlight correctly
- ✅ Performance is acceptable (< 2s load)
- ✅ Doesn't break Phase 1 features
- ✅ No crashes or ANR errors

### SHOULD HAVE (Important)
- ✅ 150+ languages supported
- ✅ Real-time highlighting while typing
- ✅ Language auto-detection works
- ✅ Copy code still works correctly
- ✅ Beam (QR) still works

### NICE TO HAVE (Optional)
- ✅ Line numbers displayed
- ✅ Multiple color themes
- ✅ Customizable font size
- ✅ Code folding support
- ✅ Search within code

---

## 🚀 Phase 2 Implementation Order

1. **Install Prism.js** - Add library to assets/ (1 hour)
2. **Create CodeHighlighter.java** - Language detection + HTML generation (2 hours)
3. **Create CodeView.java** - Custom WebView for display (1.5 hours)
4. **Update SnipAdapter** - Show previews with highlighting (1.5 hours)
5. **Test all languages** - Verify 150+ languages (2 hours)
6. **Optimize performance** - Lazy loading, caching (1 hour)
7. **Final testing** - Run full test suite (2 hours)

**Total Phase 2 Time:** 11-13 hours

---

## 🧪 Testing Tools You'll Need

### Android Studio Built-In
- **Logcat** - Check for errors
- **Android Profiler** - Performance testing
- **Device Explorer** - Verify assets/ files
- **Run** button - Test on emulator/device

### External
- **Text Editor** - Verify copied code
- **QR Code Scanner** - Test Beam feature
- **Chrome Developer Tools** - Debug WebView (if needed)

---

## ⚠️ Common Phase 2 Issues & Fixes

### Issue: Prism.js not loading
**Symptom:** Code shows no colors, all text black
**Fix:**
- Verify `assets/highlighting/prism.js` exists
- Check AndroidManifest has `android.permission.INTERNET`
- Verify WebView has JavaScript enabled

### Issue: Only some languages work
**Symptom:** Java highlights but Python doesn't
**Fix:**
- Check language pack file is included
- Verify CodeHighlighter detects language correctly
- Verify Prism has that language pack

### Issue: Performance is slow
**Symptom:** App freezes when opening large snippets
**Fix:**
- Implement lazy loading
- Use background thread for highlighting
- Reduce code preview size
- Cache highlighted HTML

### Issue: Copy doesn't work
**Symptom:** Copy button pastes HTML instead of code
**Fix:**
- Verify copy uses `snip.code` not WebView content
- Check ClipboardManager is set correctly
- Test with different apps (Gmail, etc.)

### Issue: QR codes fail
**Symptom:** Beam button doesn't work anymore
**Fix:**
- Verify QR generation still receives plain code
- Check GZIP compression still works
- Test with smaller snippets first

---

## 📈 Testing Progress Tracker

Mark off tests as you complete them:

```
Week 1:
- [ ] Prism installation complete
- [ ] Core functionality tests pass (Tests 1-3)

Week 2:
- [ ] Language support tests pass (Tests 4-5)
- [ ] Performance tests pass (Test 6)

Week 3:
- [ ] Integration tests pass (Tests 7-9)
- [ ] Phase 1 features still work (Test 10)

Week 4:
- [ ] Full test suite passes
- [ ] Edge cases handled
- [ ] Phase 2 released ✅
```

---

## 🎯 Next Steps After Phase 2

Once Phase 2 is complete and tested:
- **Phase 3:** Advanced Tagging System (many-to-many DB)
- **Phase 4:** IP Tracker & Network Tools
- **Phase 5:** Enhanced Beam with compression optimization

---

## 📞 If You Get Stuck

### Quick Debug Checklist
1. Check **Logcat** for error messages
2. Review **DIAGNOSIS.md** patterns
3. Test in **Android Studio Profiler**
4. Check **assets/highlighting/** folder
5. Verify **WebView** settings in code

### Common Log Errors
- `WebView not initialized` → Check CodeView.java
- `Prism is undefined` → Check prism.js loaded
- `ANR` → Performance issue, need async loading
- `FileNotFound` → Check assets/ folder path

---

## ✅ Phase 2 Complete When

You can consider Phase 2 complete when:
- ✅ All 10 test categories pass
- ✅ Checklist 100% complete
- ✅ No ANR or crash errors
- ✅ 150+ languages highlight
- ✅ Performance acceptable
- ✅ All Phase 1 features still work
- ✅ User can preview beautiful code

---

**Ready to test Phase 2? Make sure Phase 1 is complete first! Then come back to this guide. 🚀**
