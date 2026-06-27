// src/screens/AchievementsScreen.js
import React, { useState, useEffect } from "react";
import { useAuth } from "../hooks/useAuth";
import { getChildren, getWeeklyReport } from "../services/firebase";
import {
  getLevel, getLevelTitle, getXpForNextLevel,
  BADGES, formatDuration,
} from "../utils/pin";
import BottomNav from "../components/BottomNav";

const AVATAR_COLORS = ["av-blue", "av-pink", "av-green", "av-amber", "av-purple"];
const getInitials = (n) => n.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();

export default function AchievementsScreen() {
  const { user } = useAuth();
  const [children, setChildren] = useState([]);
  const [selectedChild, setSelectedChild] = useState(null);
  const [weekSessions, setWeekSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const unsub = getChildren(user.uid, (kids) => {
      setChildren(kids);
      if (kids.length > 0 && !selectedChild) setSelectedChild(kids[0]);
      setLoading(false);
    });
    return unsub;
  }, [user]);

  useEffect(() => {
    if (!selectedChild) return;
    getWeeklyReport(selectedChild.id).then(setWeekSessions);
  }, [selectedChild]);

  if (loading) {
    return (
      <div className="screen" style={{ alignItems: "center", justifyContent: "center" }}>
        <div className="spinner" />
      </div>
    );
  }

  const child = selectedChild;
  const xp = child?.xp || 0;
  const level = getLevel(xp);
  const levelTitle = getLevelTitle(level);
  const { current: xpCurrent, needed: xpNeeded } = getXpForNextLevel(xp);
  const xpPct = xpNeeded > 0 ? Math.round((xpCurrent / xpNeeded) * 100) : 100;
  const streak = child?.currentStreak || 0;
  const totalSessions = child?.totalSessions || weekSessions.length;

  // Compute stats for badge checking
  const avgWeeklyScore = weekSessions.length
    ? Math.round(weekSessions.reduce((a, s) => a + (s.focusScore || 0), 0) / weekSessions.length)
    : 0;
  const childStats = {
    totalSessions,
    currentStreak: streak,
    hasEarlySession: weekSessions.some((s) => {
      const h = s.startTime?.toDate?.()?.getHours() || 10;
      return h < 8;
    }),
    hasNightSession: weekSessions.some((s) => {
      const h = s.startTime?.toDate?.()?.getHours() || 10;
      return h >= 21;
    }),
    avgWeeklyScore,
    scienceSessions: 0,
    readingSessions: 0,
    mathSessions: 0,
    maxDailyMinutes: weekSessions.reduce((a, s) => Math.max(a, s.durationMinutes || 0), 0),
  };

  const earnedBadgeIds = new Set(child?.badges || []);
  // Auto-check new badges
  const allBadges = BADGES.map((badge) => ({
    ...badge,
    earned: earnedBadgeIds.has(badge.id) || badge.check(childStats),
  }));

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div className="logo-badge">🏆</div>
            <span className="header-title">Achievements</span>
          </div>
        </div>
      </div>

      <div className="scroll-content">
        {/* Child selector */}
        {children.length > 1 && (
          <div style={{ padding: "12px 16px 0", display: "flex", gap: 8 }}>
            {children.map((c, idx) => (
              <button
                key={c.id}
                onClick={() => setSelectedChild(c)}
                style={{
                  display: "flex", alignItems: "center", gap: 8,
                  padding: "8px 14px", borderRadius: 20, fontSize: 13, fontWeight: 500,
                  border: `1.5px solid ${selectedChild?.id === c.id ? "var(--green)" : "var(--border)"}`,
                  background: selectedChild?.id === c.id ? "var(--green-light)" : "var(--surface)",
                  cursor: "pointer",
                }}
              >
                <div className={`avatar ${AVATAR_COLORS[idx % AVATAR_COLORS.length]}`}
                  style={{ width: 24, height: 24, fontSize: 10 }}>
                  {getInitials(c.name)}
                </div>
                {c.name.split(" ")[0]}
              </button>
            ))}
          </div>
        )}

        {child ? (
          <>
            {/* Level card */}
            <div className="level-card" style={{ marginTop: 16 }}>
              <div className="level-badge-circle">
                <span style={{ color: "white", fontSize: 20, fontWeight: 800, lineHeight: 1 }}>{level}</span>
                <span style={{ color: "rgba(255,255,255,0.7)", fontSize: 9, marginTop: 2 }}>LEVEL</span>
              </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ color: "white", fontSize: 16, fontWeight: 700 }}>{levelTitle}</h3>
                <p style={{ color: "#94A3B8", fontSize: 12 }}>{child.name}</p>
                <div className="xp-bar-bg">
                  <div className="xp-bar-fill" style={{ width: `${xpPct}%` }} />
                </div>
                <p style={{ color: "#94A3B8", fontSize: 10, marginTop: 4 }}>
                  {xpCurrent.toLocaleString()} / {xpNeeded.toLocaleString()} XP · {xpPct}% to Level {level + 1}
                </p>
              </div>
            </div>

            {/* Streak */}
            {streak > 0 && (
              <div style={{ padding: "0 16px 0", marginTop: 4 }}>
                <div style={{
                  background: "#FFF7ED", borderRadius: "var(--radius)", padding: "12px 14px",
                  border: "1px solid #FED7AA", display: "flex", alignItems: "center", gap: 10,
                }}>
                  <span style={{ fontSize: 28 }}>🔥</span>
                  <div>
                    <p style={{ fontSize: 14, fontWeight: 700, color: "#92400E" }}>
                      {streak}-day streak — keep it up!
                    </p>
                    <p style={{ fontSize: 11, color: "#B45309" }}>
                      Study today to maintain your streak
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Stats row */}
            <div style={{
              display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 10,
              padding: "12px 16px 0",
            }}>
              {[
                { label: "Sessions", value: totalSessions },
                { label: "Total XP", value: xp.toLocaleString() },
                { label: "Badges", value: allBadges.filter((b) => b.earned).length + "/" + allBadges.length },
              ].map((s) => (
                <div key={s.label} style={{
                  background: "var(--surface)", borderRadius: "var(--radius)",
                  border: "1px solid var(--border)", padding: 12, textAlign: "center",
                }}>
                  <p style={{ fontSize: 18, fontWeight: 700, color: "var(--text)" }}>{s.value}</p>
                  <p style={{ fontSize: 11, color: "var(--text-3)", marginTop: 2 }}>{s.label}</p>
                </div>
              ))}
            </div>

            {/* Badges */}
            <p className="section-label">Badges</p>
            <div style={{
              display: "grid", gridTemplateColumns: "repeat(2,1fr)", gap: 12,
              padding: "0 16px 16px",
            }}>
              {allBadges.map((badge) => (
                <div key={badge.id} className={`badge-card ${badge.earned ? "earned" : "locked"}`}>
                  <span className="badge-emoji">{badge.icon}</span>
                  <p className="badge-name">{badge.name}</p>
                  <p className="badge-desc">{badge.desc}</p>
                  {badge.earned && (
                    <span style={{
                      fontSize: 10, fontWeight: 600,
                      background: "var(--green-light)", color: "var(--green-dark)",
                      padding: "2px 8px", borderRadius: 20, marginTop: 4,
                    }}>Earned ✓</span>
                  )}
                </div>
              ))}
            </div>

            {/* Level progression */}
            <div style={{
              margin: "0 16px 16px",
              background: "var(--surface)", borderRadius: "var(--radius)",
              border: "1px solid var(--border)", padding: 16,
            }}>
              <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Level progression</p>
              {[
                [1, "Beginner", "🌱"],
                [5, "Learner", "📗"],
                [10, "Scholar", "🎓"],
                [20, "Achiever", "🏅"],
                [50, "Master", "👑"],
              ].map(([lvl, title, icon]) => (
                <div key={lvl} style={{
                  display: "flex", alignItems: "center", gap: 12,
                  padding: "8px 0", borderBottom: "1px solid var(--border)",
                }}>
                  <span style={{ fontSize: 22 }}>{icon}</span>
                  <div style={{ flex: 1 }}>
                    <p style={{ fontSize: 13, fontWeight: 600, color: level >= lvl ? "var(--green-dark)" : "var(--text)" }}>
                      Level {lvl} · {title}
                    </p>
                  </div>
                  {level >= lvl
                    ? <span style={{ color: "var(--green)", fontSize: 18 }}>✓</span>
                    : <span style={{ fontSize: 11, color: "var(--text-3)" }}>Locked</span>
                  }
                </div>
              ))}
            </div>
          </>
        ) : (
          <div style={{ textAlign: "center", padding: "48px 24px" }}>
            <p style={{ fontSize: 48 }}>👶</p>
            <p style={{ fontSize: 14, color: "var(--text-2)", marginTop: 12 }}>
              Add a child to see their achievements
            </p>
          </div>
        )}
      </div>

      <BottomNav active="achievements" />
    </div>
  );
}
