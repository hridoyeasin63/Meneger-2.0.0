# Navigation Drawer Modernization Plan

This plan details the redesign of the app's side drawer into a modern, production-grade Material 3 Navigation Drawer with an interactive user profile header, structured action items, and an expandable developer credentials card with one-touch clipboard copy.

---

## 1. User Profile Header Section
- **Visual Design**: An elevated gradient card (`Brush.linearGradient`) displaying:
  - User avatar: Circular profile image if signed in via Google (`rememberAsyncImagePainter(user.photoUrl)`), or an elegant initials badge.
  - User details: Display name, email address, and an active "Firebase Connected" badge with a green dot indicator.
  - Guest state: Friendly guest illustration/icon with status "সাইন ইন করা নেই (Guest Mode)" and a quick-action login button.

---

## 2. Organized Data Management & Tool Tiles
- **Design Pattern**: Grouped into clean, card-like sections with subtle borders, generous touch targets (min 48dp), and descriptive subtitles:
  - **ক্লাউড ব্যাকআপ (Backup to Cloud)**: Icon badge with description "আপনার সব হিসাব ক্লাউডে নিরাপদ রাখুন".
  - **ক্লাউড রিস্টোর (Restore from Cloud)**: Icon badge with description "ক্লাউড থেকে পূর্বের হিসাব ফিরিয়ে আনুন".
  - **তাত্ক্ষণিক পুশ (Manual Push)**: Quick-upload to Firestore with animated loading indicator.
  - **সিঙ্ক রিফ্রেশ (Sync & Refresh)**: Re-fetch latest changes from Firestore.
  - **রিসাইকেল বিন (Trash Bin)**: Clean badge showing the count of deleted items (`Trash (5)`).

---

## 3. Collapsible Developer & Firebase Credentials Card
- **Header**: Tappable header "ডেভেলপার ইনফো ও ক্রেডেনশিয়ালস 🔑" with expand/collapse chevron animation.
- **Content Rows** (inside an `OutlinedCard` with smooth expansion):
  - **Package Name**: `com.aistudio.meneger2.hridoy`
  - **SHA-1 Fingerprint**: Monospace snippet with one-tap copy button (`ClipboardManager`).
  - **SHA-256 Fingerprint**: Monospace snippet with one-tap copy button.
  - **Firebase Project ID**: `master-63`.
- **User Feedback**: Instant visual checkmark and Toast ("ক্লিপবোর্ডে কপি করা হয়েছে!").

---

## 4. Modern Bottom Actions & Google Sign-In
- Prominent Material 3 filled button pinned to the bottom:
  - When signed in: Danger/destructive styled "লগআউট করুন" button.
  - When guest: High-contrast branded "গুগল দিয়ে লগইন" with progress indicator during sign-in.
- App version subtitle (`v2.0 • Meneger`) for a professional app feel.

---

## Verification Plan
1. **Compilation Check**: Run `compile_applet` to confirm all Compose M3 components compile cleanly.
2. **Visual & Interaction Check**: Open the drawer, verify expansion/collapse animation, test copy-to-clipboard on each credential row, and test sign-in/out flows.
