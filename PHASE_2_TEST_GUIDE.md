# 🧪 Phase 2: Syntax-Aware Repository - Testing Guide

This guide outlines the steps to verify the successful implementation of the **Syntax Highlighting** and **High-Performance Editor** features in SnipIT.

---

## 🟢 TIER 1: THE VAULT (Syntax Rendering)
**Goal:** Verify that snippets in your repository are rendered with professional syntax highlighting.

1. **Open the Vault Tab**: Ensure you are on the main snippets list.
2. **View Code Cards**:
   - [ ] Check if the code blocks have colors (keywords, strings, comments).
   - [ ] Verify that the language tag (e.g., `JS`, `JAVA`, `PY`) is visible on each card.
   - [ ] Ensure the font is monospace (Terminal-style).
3. **Verify Highlighting Integrity**:
   - [ ] Test a **JavaScript** snippet: verify `function`, `const`, and `console.log` are colored.
   - [ ] Test a **CSS** snippet: verify selectors and properties have distinct colors.

---

## 🟠 TIER 2: THE EDITOR (Live Preview)
**Goal:** Verify the bridge between raw text editing and the Prism.js rendering engine.

1. **Open a Snippet for Editing**: Tap on any snippet in the Vault.
2. **Toggle Syntax ON**:
   - [ ] Locate the **SYNTAX ON** button (Green).
   - [ ] Click it. The editor should vanish and a professional code preview should appear.
   - [ ] Verify the button text changes to **SYNTAX OFF** (Orange).
3. **Verify Interactive Sync**:
   - [ ] Toggle back to **SYNTAX OFF**.
   - [ ] Make a change to the code.
   - [ ] Toggle **SYNTAX ON** again and verify the change is reflected in the highlighted view.

---

## 🟠 TIER 3: AI & AUTO-DETECTION
**Goal:** Verify the intelligence layer of the repository.

1. **Test Language Auto-Detect**:
   - [ ] Create a new snippet (set language to `Plain Text`).
   - [ ] Paste `def hello_world(): print("Hi")`.
   - [ ] Verify the language picker automatically switches to **Python**.
2. **Test AI-Fix Preview Sync**:
   - [ ] Keep the **SYNTAX OFF** (Preview mode) active.
   - [ ] Tap **FIX WITH AI** (The magic wand/chip).
   - [ ] Once the AI responds, verify the **highlighted preview updates automatically** without you having to toggle.

---

## 🔴 TIER 4: PERFORMANCE & EDGE CASES
**Goal:** Ensure the WebView bridge is stable.

1. **Test Large Snippets**:
   - [ ] Paste a 100+ line code block.
   - [ ] Toggle **SYNTAX ON**. Verify the highlight rendering doesn't cause a hang.
2. **Test Unknown Languages**:
   - [ ] Set language to `Plain Text`.
   - [ ] Verify the code is still readable even without color (fallback mode).

---

## 🎯 SUCCESS CRITERIA
- [ ] No crashes when toggling syntax.
- [ ] Prism.js correctly maps 150+ languages.
- [ ] Auto-detection works within 200ms of pasting.
- [ ] The "Dark-Kernel" theme is consistent in both Editor and Vault.

**If all tests pass, Phase 2 is COMPLETE! 🚀**
