# 🚀 Study Shield — Build APK on GitHub (No Android Studio needed)
## Your Firebase is already live at studyshield-ai.web.app ✅

---

## Why GitHub Actions?
- GitHub's cloud servers build your Android APK for FREE
- You push code → 5 minutes later → downloadable APK
- No Android Studio, no local Java, no nothing
- Also auto-deploys your web app to Firebase on every push

---

## STEP 1: Create GitHub Repository (5 min)

1. Go to https://github.com → Sign in (or create free account)
2. Click **New repository** (green button top right)
3. Name: `study-shield`
4. Set to **Private** (important — keeps your Firebase keys safe)
5. Click **Create repository**
6. Copy the repo URL shown (e.g. `https://github.com/YOUR_NAME/study-shield.git`)

---

## STEP 2: Add Your Firebase Secrets (3 min)

Your Firebase config must be stored as **GitHub Secrets** (never in code).

1. In your GitHub repo → click **Settings** tab
2. Left sidebar → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add each of these:

| Secret Name | Where to find it |
|---|---|
| `FIREBASE_API_KEY` | Firebase Console → Project Settings → Your apps → Web → apiKey |
| `FIREBASE_AUTH_DOMAIN` | same → authDomain |
| `FIREBASE_PROJECT_ID` | same → projectId (yours is: `studyshield-ai`) |
| `FIREBASE_STORAGE_BUCKET` | same → storageBucket |
| `FIREBASE_MESSAGING_SENDER_ID` | same → messagingSenderId |
| `FIREBASE_APP_ID` | same → appId |
| `FIREBASE_MEASUREMENT_ID` | same → measurementId |
| `GOOGLE_SERVICES_JSON` | Download from Firebase Console → Project Settings → Android app → google-services.json → paste entire file content |
| `FIREBASE_SERVICE_ACCOUNT` | See below ↓ |

### Getting FIREBASE_SERVICE_ACCOUNT:
1. Firebase Console → Project Settings → **Service accounts** tab
2. Click **Generate new private key** → Download JSON file
3. Open the file → copy ALL the content → paste as the secret value

---

## STEP 3: Push Code to GitHub (5 min)

On your computer, open **Command Prompt** or **Terminal**:

```bash
# Navigate to your project folder
cd path\to\study-shield-deploy

# Initialize git
git init
git add .
git commit -m "Initial commit - Study Shield v1.0"

# Add your GitHub repo as remote (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/study-shield.git

# Push to GitHub — this TRIGGERS the build automatically!
git push -u origin main
```

---

## STEP 4: Download Your APK (5 min after push)

1. Go to your GitHub repo → Click **Actions** tab
2. You'll see "Build Study Shield Android APK" workflow running
3. Wait for the green ✅ (about 4-6 minutes)
4. Click on the completed workflow run
5. Scroll down to **Artifacts** section
6. Click **study-shield-debug** → downloads a ZIP
7. Extract ZIP → you get `app-debug.apk`

---

## STEP 5: Install APK on Child's Phone (2 min)

**Method A: Direct USB transfer**
1. Connect child's phone to computer via USB
2. Copy `app-debug.apk` to the phone's Downloads folder
3. On phone → Open file manager → tap the APK → Install

**Method B: WhatsApp / Google Drive**
1. Upload `app-debug.apk` to your Google Drive
2. Share the link with yourself → open on child's phone → Download → Install

**Enable "Install from unknown sources" on the phone first:**
- Android 8+: Settings → Apps → Special app access → Install unknown apps → Your browser → Allow
- Older Android: Settings → Security → Unknown sources → Enable

---

## STEP 6: Link Child's Device to Parent App

1. Open Study Shield app on child's phone
2. Complete the 4-step permission setup wizard
3. Note the **Device ID** shown (format: `SS-ABCD`)
4. In parent web app (studyshield-ai.web.app) → Add child → paste the Device ID
5. Done! Start a session from parent app → child's phone enters study mode instantly

---

## How the Auto-Build Works

Every time you make any code change and push to GitHub:
```
You push code
    → GitHub Actions triggers
    → Cloud server runs: ./gradlew assembleDebug
    → APK is built and uploaded as artifact
    → Web app is rebuilt and deployed to studyshield-ai.web.app
```

---

## Project Folder Structure to Push

```
study-shield-deploy/         ← Root of your GitHub repo
├── .github/
│   └── workflows/
│       └── build.yml        ← The magic — tells GitHub what to build
├── web-app/                 ← React PWA (parent dashboard)
│   ├── src/
│   ├── public/
│   └── package.json
├── android-agent/           ← Android app (student device)
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/studyshield/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle/wrapper/
└── GITHUB_SETUP_GUIDE.md
```

---

## FAQ

**Q: Is GitHub free?**
A: Yes. GitHub Actions gives 2,000 free build minutes/month. Each build takes ~5 min, so that's 400 builds free.

**Q: Will the APK work on all Android phones?**
A: Yes — Android 7.0 and above (covers 99% of phones in use today).

**Q: Do I need to rebuild APK when I make changes?**
A: Only if you change Android code. Web app changes auto-deploy to Firebase without needing a new APK.

**Q: Can I publish to Google Play Store later?**
A: Yes! Change the workflow to `assembleRelease`, add your signing keystore as a secret, and the workflow will produce a signed release APK ready for Play Store upload.

**Q: What if the build fails?**
A: Click on the failed workflow → expand the failed step → read the error → the most common issue is a missing `google-services.json` secret.

---

*Study Shield v1.0.0 · studyshield-ai.web.app*
