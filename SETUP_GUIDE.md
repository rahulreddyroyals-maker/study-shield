# Study Shield — Complete Setup Guide
## Powered by School Connect ERP

---

## 📁 Project Structure

```
study-shield/
├── web-app/              ← React PWA (Parent dashboard)
│   ├── src/
│   │   ├── screens/      ← All 9 screens
│   │   ├── services/     ← firebase.js (all Firestore helpers)
│   │   ├── hooks/        ← useAuth.js
│   │   ├── components/   ← BottomNav
│   │   └── utils/        ← pin.js (hashing, XP, badges)
│   └── package.json
├── android-app/          ← Android agent (app blocking)
│   └── app/src/main/
│       ├── java/com/studyshield/
│       │   ├── services/  ← AppMonitorService, FCM
│       │   ├── receivers/ ← Boot, DeviceAdmin
│       │   ├── ui/        ← MainActivity, Setup, Blocked
│       │   └── utils/     ← DeviceId, AppBlocker, SessionState
│       └── AndroidManifest.xml
└── firestore.rules        ← Security rules
```

---

## 🔥 Step 1: Firebase Setup (15 minutes)

1. Go to https://console.firebase.google.com
2. Click **Create a project** → Name: `study-shield`
3. Enable **Google Analytics** → Continue

### Authentication
4. Go to **Build → Authentication → Get started**
5. Enable **Phone** (for OTP login)
6. Enable **Google** (for Google sign-in)
7. Add your domain to authorized domains (for production)

### Firestore Database
8. Go to **Build → Firestore Database → Create database**
9. Start in **test mode** (change to production rules later)
10. Select region: `asia-south1` (Mumbai — best for India)

### Get Your Config
11. Go to **Project Settings → Your apps → Add app → Web**
12. Register app, copy the `firebaseConfig` object
13. Paste it into `web-app/src/services/firebase.js` replacing the placeholder values

### Firebase Cloud Messaging
14. Go to **Project Settings → Cloud Messaging**
15. Copy the **Web Push certificate (VAPID key)**
16. Paste it in `firebase.js` → `requestNotificationPermission()` → `vapidKey`

### Deploy Security Rules
```bash
npm install -g firebase-tools
firebase login
firebase init firestore
# Select your project
firebase deploy --only firestore:rules
```

---

## 🌐 Step 2: Web App Setup (10 minutes)

```bash
cd web-app
npm install
npm start          # Development server at http://localhost:3000
npm run build      # Production build
```

### Deploy to Firebase Hosting
```bash
firebase init hosting
# Public directory: build
# Single page app: Yes
firebase deploy --only hosting
```

Your app will be live at: `https://study-shield-XXXXX.web.app`

---

## 📱 Step 3: Android App Setup (30 minutes)

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- JDK 17+
- Android device running Android 7+ (API 24+)

### Firebase Android Setup
1. Go to Firebase Console → Add app → Android
2. Package name: `com.studyshield`
3. Download `google-services.json`
4. Place it in `android-app/app/google-services.json`

### Build & Install
```bash
cd android-app
# Open in Android Studio, or:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Required Permissions (grant manually on device)
The app's **Setup Wizard** will walk through these:

| Permission | Why | How to Grant |
|-----------|-----|--------------|
| Usage Access | Detect which app is in foreground | Settings → Digital Wellbeing → Usage access |
| Overlay | Show blocking screen | Settings → Apps → Study Shield → Appear on top |
| Accessibility | Secondary blocking layer | Settings → Accessibility → Study Shield |
| Device Admin (optional) | Prevent uninstall | Prompted in app |

---

## 🔗 Step 4: Link Parent + Child Device (2 minutes)

1. Parent opens Study Shield web app → Login with OTP
2. Tap **+ Add child** → Enter child's name and class
3. On the **child's Android phone**, open Study Shield → note the **Device ID** (format: `SS-ABCD`)
4. Back in parent app → Enter the Device ID in the "Android device ID" field
5. Tap **Add child** — linking is complete!

---

## 🚀 Step 5: First Study Session

1. **Parent**: Open web app → Home → Tap **Start Study Shield**
2. Select child → Select duration → Tap **Start Study Shield**
3. **Firestore** pushes session data to the child's device in real-time (~1 second)
4. **Android agent** receives the update and activates blocking
5. If the child opens Instagram/YouTube → **Block screen appears instantly**
6. **Parent**: Tap **Stop session** → Enter PIN → Session ends, Firestore updated

---

## 💡 Architecture Flow

```
Parent Web App (React PWA)
        │
        │  Writes session to Firestore
        ▼
   Firebase Firestore
        │
        │  Realtime listener (onSnapshot)
        ▼
 Android Agent (AppMonitorService)
        │
        │  Polls UsageStats every 500ms
        ▼
  BlockedActivity (full-screen overlay)
        │
        │  Reports blocked attempts to Firestore
        ▼
   Parent sees live count in Reports
```

---

## 🔧 Customization

### Change Allowed Apps
Edit `DEFAULT_ALLOWED_APPS` in `src/services/firebase.js`:
```js
{ id: "my_app", name: "My App", packageName: "com.myapp.android", icon: "book" }
```

### Change Reward Rules
In `RewardsScreen.js`, edit `DEFAULT_REWARDS` array.

### Change PIN Default
In `PinUnlock.js`, `SetupActivity.kt` references default "1234" — change as needed.

### School Connect ERP Integration
To trigger sessions automatically from homework assignments:
1. In your School Connect ERP webhook, POST to a Firebase Cloud Function
2. The function creates a session document in Firestore
3. The Android agent picks it up automatically

---

## 🔐 Security Notes

- Parent PIN is hashed with SHA-256 + salt before storing in Firestore
- Firestore rules prevent children from reading other families' data
- Device admin prevents app uninstall during sessions
- Boot receiver ensures monitoring restarts after phone reboot

---

## 📊 Pricing

| Plan | Features | Price |
|------|----------|-------|
| Free | 1 child, basic reports | ₹0 |
| Premium | Unlimited children, AI reports, rewards | ₹99/month |
| School | Bulk licensing | ₹20–50/student/month |

To implement billing: integrate **Razorpay** or **Google Play Billing** and update the `plan` field on the parent's Firestore document.

---

## 🐛 Troubleshooting

**App not blocking on Android?**
→ Check Usage Access permission is granted
→ Check the device ID is correctly entered in parent app
→ Ensure Firestore rules allow the device to read child documents

**OTP not received?**
→ Ensure phone auth is enabled in Firebase Console
→ Check the phone number format: must be `+91XXXXXXXXXX`

**Firebase config errors?**
→ Double-check all values in `firebase.js` match your project console

---

*Study Shield v1.0.0 · Built on Firebase + React + Kotlin*
*Designed as an add-on for School Connect ERP*
