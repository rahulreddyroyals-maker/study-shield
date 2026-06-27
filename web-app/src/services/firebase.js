// src/services/firebase.js
// ─────────────────────────────────────────────────────────────────────────────
// SETUP INSTRUCTIONS:
//  1. Go to https://console.firebase.google.com
//  2. Create a project named "study-shield"
//  3. Enable Authentication → Phone (for OTP) and Google
//  4. Create Firestore Database (start in test mode, then apply security rules)
//  5. Replace the firebaseConfig values below with your project's config
//  6. Enable Firebase Cloud Messaging for push notifications
// ─────────────────────────────────────────────────────────────────────────────

import { initializeApp } from "firebase/app";
import {
  getAuth,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
} from "firebase/auth";
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  setDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  getDocs,
  onSnapshot,
  serverTimestamp,
  Timestamp,
} from "firebase/firestore";
import { getMessaging, getToken, onMessage } from "firebase/messaging";

// ── Replace with your Firebase project config ─────────────────────────────────
const firebaseConfig = {
 apiKey: "AIzaSyAXF9kKKZXl2xjHP4iaw4n3V0A7cSp82jU",
  authDomain: "studyshield-ai.firebaseapp.com",
  projectId: "studyshield-ai",
  storageBucket: "studyshield-ai.firebasestorage.app",
  messagingSenderId: "752241275854",
  appId: "1:752241275854:web:3b9b8fab4fec57d9b6719f",
  measurementId: "G-F03ZCQJ0NS"
};
// ─────────────────────────────────────────────────────────────────────────────

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);

let messaging = null;
try {
  messaging = getMessaging(app);
} catch (e) {
  console.warn("Firebase Messaging not supported in this environment");
}
export { messaging };

// ── Auth helpers ──────────────────────────────────────────────────────────────

export const setupRecaptcha = (containerId) => {
  return new RecaptchaVerifier(auth, containerId, {
    size: "invisible",
    callback: () => {},
  });
};

export const sendOTP = async (phoneNumber, appVerifier) => {
  return await signInWithPhoneNumber(auth, phoneNumber, appVerifier);
};

export const signInWithGoogle = async () => {
  const provider = new GoogleAuthProvider();
  return await signInWithPopup(auth, provider);
};

export const logOut = () => signOut(auth);

export const onAuthChange = (callback) => onAuthStateChanged(auth, callback);

// ── Firestore helpers ─────────────────────────────────────────────────────────

// Parents
export const createParent = async (uid, data) => {
  await setDoc(doc(db, "parents", uid), {
    ...data,
    createdAt: serverTimestamp(),
    plan: "free",
  });
};

export const getParent = async (uid) => {
  const snap = await getDoc(doc(db, "parents", uid));
  return snap.exists() ? { id: snap.id, ...snap.data() } : null;
};

export const updateParent = async (uid, data) => {
  await updateDoc(doc(db, "parents", uid), data);
};

// Children
export const addChild = async (parentId, childData) => {
  return await addDoc(collection(db, "children"), {
    parentId,
    ...childData,
    createdAt: serverTimestamp(),
    level: 1,
    xp: 0,
    totalStudyMinutes: 0,
    currentStreak: 0,
    longestStreak: 0,
    focusScore: 0,
    badges: [],
  });
};

export const getChildren = (parentId, callback) => {
  const q = query(collection(db, "children"), where("parentId", "==", parentId));
  return onSnapshot(q, (snap) => {
    const children = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
    callback(children);
  });
};

export const updateChild = async (childId, data) => {
  await updateDoc(doc(db, "children", childId), data);
};

export const deleteChild = async (childId) => {
  await deleteDoc(doc(db, "children", childId));
};

// Sessions
export const startSession = async (childId, parentId, sessionData) => {
  const sessionRef = await addDoc(collection(db, "sessions"), {
    childId,
    parentId,
    ...sessionData,
    status: "active",
    startTime: serverTimestamp(),
    endTime: null,
    blockedAttempts: 0,
    appsAttempted: {},
    focusScore: null,
  });

  // Push session to child's device via Firestore realtime
  await updateDoc(doc(db, "children", childId), {
    activeSessionId: sessionRef.id,
    sessionStatus: "active",
    sessionStartTime: serverTimestamp(),
    sessionDurationMinutes: sessionData.durationMinutes,
    allowedApps: sessionData.allowedApps,
  });

  return sessionRef.id;
};

export const endSession = async (sessionId, childId, stats) => {
  const focusScore = Math.max(
    0,
    Math.min(100, 100 - Math.floor(stats.blockedAttempts / 2))
  );

  await updateDoc(doc(db, "sessions", sessionId), {
    status: "completed",
    endTime: serverTimestamp(),
    ...stats,
    focusScore,
  });

  await updateDoc(doc(db, "children", childId), {
    activeSessionId: null,
    sessionStatus: "idle",
    lastSessionScore: focusScore,
  });

  return focusScore;
};

export const getActiveSession = (childId, callback) => {
  const q = query(
    collection(db, "sessions"),
    where("childId", "==", childId),
    where("status", "==", "active"),
    limit(1)
  );
  return onSnapshot(q, (snap) => {
    if (!snap.empty) {
      callback({ id: snap.docs[0].id, ...snap.docs[0].data() });
    } else {
      callback(null);
    }
  });
};

export const incrementBlockedAttempt = async (sessionId, appName) => {
  const sessionRef = doc(db, "sessions", sessionId);
  const snap = await getDoc(sessionRef);
  if (snap.exists()) {
    const data = snap.data();
    const appsAttempted = data.appsAttempted || {};
    appsAttempted[appName] = (appsAttempted[appName] || 0) + 1;
    await updateDoc(sessionRef, {
      blockedAttempts: (data.blockedAttempts || 0) + 1,
      appsAttempted,
    });
  }
};

// Reports
export const getWeeklyReport = async (childId) => {
  const sevenDaysAgo = Timestamp.fromDate(
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  );
  const q = query(
    collection(db, "sessions"),
    where("childId", "==", childId),
    where("status", "==", "completed"),
    where("startTime", ">=", sevenDaysAgo),
    orderBy("startTime", "desc")
  );
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
};

export const getDailyReport = async (childId) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const q = query(
    collection(db, "sessions"),
    where("childId", "==", childId),
    where("status", "==", "completed"),
    where("startTime", ">=", Timestamp.fromDate(today)),
    orderBy("startTime", "desc")
  );
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
};

// Rewards
export const getRewards = (parentId, callback) => {
  const q = query(collection(db, "rewards"), where("parentId", "==", parentId));
  return onSnapshot(q, (snap) => {
    callback(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
  });
};

export const addReward = async (parentId, rewardData) => {
  return await addDoc(collection(db, "rewards"), {
    parentId,
    ...rewardData,
    createdAt: serverTimestamp(),
    active: true,
  });
};

export const claimReward = async (rewardId, childId) => {
  await addDoc(collection(db, "rewardClaims"), {
    rewardId,
    childId,
    claimedAt: serverTimestamp(),
    status: "pending",
  });
};

// PIN Management
export const setParentPin = async (parentId, hashedPin) => {
  await updateDoc(doc(db, "parents", parentId), {
    pinHash: hashedPin,
    pinSetAt: serverTimestamp(),
  });
};

export const verifyParentPin = async (parentId, hashedPin) => {
  const parent = await getParent(parentId);
  return parent?.pinHash === hashedPin;
};

// FCM Push Notifications
export const requestNotificationPermission = async () => {
  if (!messaging) return null;
  try {
    const token = await getToken(messaging, {
      vapidKey: "BBONECKkdtplkMKKxgO2sPXBhTuf1Y4LoRiMtCWhy204hd1Zw2-8EENmUEC1Z32-jPrclKzdPc3f1W5aTJ9ppNo",
    });
    return token;
  } catch (e) {
    console.warn("Notification permission denied");
    return null;
  }
};

export const onForegroundMessage = (callback) => {
  if (!messaging) return () => {};
  return onMessage(messaging, callback);
};

// Allowed apps config
export const DEFAULT_ALLOWED_APPS = [
  { id: "school_erp", name: "School ERP", packageName: "com.schoolconnect.erp", icon: "school" },
  { id: "study_buddy", name: "Study Buddy", packageName: "com.vidyamitra.app", icon: "book" },
  { id: "skillhub", name: "SkillHub", packageName: "com.skillhub.school", icon: "layers" },
  { id: "calculator", name: "Calculator", packageName: "com.android.calculator2", icon: "calculator" },
  { id: "phone", name: "Phone", packageName: "com.android.dialer", icon: "phone" },
  { id: "messages", name: "Messages", packageName: "com.android.messaging", icon: "message" },
  { id: "notes", name: "Notes", packageName: "com.google.android.keep", icon: "file-text" },
  { id: "dictionary", name: "Dictionary", packageName: "com.dictionary.offline", icon: "book-2" },
  { id: "pdf_reader", name: "PDF Reader", packageName: "com.adobe.reader", icon: "file-pdf" },
  { id: "camera", name: "Camera", packageName: "com.android.camera", icon: "camera" },
];

export const BLOCKED_APPS = [
  { id: "instagram", name: "Instagram", packageName: "com.instagram.android" },
  { id: "youtube", name: "YouTube", packageName: "com.google.android.youtube" },
  { id: "facebook", name: "Facebook", packageName: "com.facebook.katana" },
  { id: "snapchat", name: "Snapchat", packageName: "com.snapchat.android" },
  { id: "bgmi", name: "BGMI", packageName: "com.pubg.imobile" },
  { id: "freefire", name: "Free Fire", packageName: "com.dts.freefireth" },
  { id: "tiktok", name: "TikTok", packageName: "com.zhiliaoapp.musically" },
  { id: "twitter", name: "X (Twitter)", packageName: "com.twitter.android" },
  { id: "spotify", name: "Spotify", packageName: "com.spotify.music" },
  { id: "netflix", name: "Netflix", packageName: "com.netflix.mediaclient" },
];
