// src/utils/pin.js

// Simple SHA-256 hash using Web Crypto API
export const hashPin = async (pin) => {
  const msgBuffer = new TextEncoder().encode(pin + "study_shield_salt_2024");
  const hashBuffer = await crypto.subtle.digest("SHA-256", msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
};

export const verifyPin = async (inputPin, storedHash) => {
  const inputHash = await hashPin(inputPin);
  return inputHash === storedHash;
};

// XP and leveling
export const XP_PER_MINUTE = 2;
export const LEVEL_THRESHOLDS = [
  0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200, 4000,
  5000, 6200, 7600, 9200, 11000, 13000, 15500, 18500, 22000, 26000,
];

export const getLevel = (xp) => {
  let level = 1;
  for (let i = 0; i < LEVEL_THRESHOLDS.length; i++) {
    if (xp >= LEVEL_THRESHOLDS[i]) level = i + 1;
    else break;
  }
  return Math.min(level, 50);
};

export const getLevelTitle = (level) => {
  if (level <= 4) return "Beginner";
  if (level <= 9) return "Learner";
  if (level <= 19) return "Scholar";
  if (level <= 49) return "Achiever";
  return "Master";
};

export const getXpForNextLevel = (xp) => {
  const level = getLevel(xp);
  const nextThreshold = LEVEL_THRESHOLDS[level] || LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
  const currThreshold = LEVEL_THRESHOLDS[level - 1] || 0;
  return { current: xp - currThreshold, needed: nextThreshold - currThreshold };
};

// Focus score calculation
export const calcFocusScore = (durationMinutes, blockedAttempts) => {
  const baseScore = 100;
  const penalty = Math.min(blockedAttempts * 3, 40);
  return Math.max(60, baseScore - penalty);
};

// Badge definitions
export const BADGES = [
  { id: "focus_starter", name: "Focus Starter", desc: "Complete first session", icon: "⭐", check: (stats) => stats.totalSessions >= 1 },
  { id: "homework_hero", name: "Homework Hero", desc: "Complete 5 sessions", icon: "📚", check: (stats) => stats.totalSessions >= 5 },
  { id: "streak_7", name: "7-Day Streak", desc: "Study 7 days in a row", icon: "🔥", check: (stats) => stats.currentStreak >= 7 },
  { id: "early_bird", name: "Early Bird", desc: "Study before 8 AM", icon: "🌅", check: (stats) => stats.hasEarlySession },
  { id: "focus_champion", name: "Focus Champion", desc: "Score 90+ for a week", icon: "🏆", check: (stats) => stats.avgWeeklyScore >= 90 },
  { id: "science_explorer", name: "Science Explorer", desc: "10 science sessions", icon: "🔬", check: (stats) => stats.scienceSessions >= 10 },
  { id: "reading_warrior", name: "Reading Warrior", desc: "5 reading sessions", icon: "📖", check: (stats) => stats.readingSessions >= 5 },
  { id: "math_master", name: "Math Master", desc: "10 math sessions", icon: "🧮", check: (stats) => stats.mathSessions >= 10 },
  { id: "night_owl", name: "Night Owl", desc: "Study after 9 PM", icon: "🦉", check: (stats) => stats.hasNightSession },
  { id: "marathon", name: "Study Marathon", desc: "Study 4+ hours in a day", icon: "🏃", check: (stats) => stats.maxDailyMinutes >= 240 },
];

// Format duration
export const formatDuration = (minutes) => {
  if (minutes < 60) return `${minutes}m`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
};

export const formatTimer = (totalSeconds) => {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) {
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  }
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
};
