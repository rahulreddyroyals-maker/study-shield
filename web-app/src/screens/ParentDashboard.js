// src/screens/ParentDashboard.js
import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import {
  getChildren,
  getDailyReport,
  logOut,
} from "../services/firebase";
import BottomNav from "../components/BottomNav";
import { formatDuration } from "../utils/pin";

const AVATAR_COLORS = ["av-blue", "av-pink", "av-green", "av-amber", "av-purple"];

const getInitials = (name) =>
  name.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
};

export default function ParentDashboard() {
  const navigate = useNavigate();
  const { user, parent } = useAuth();
  const [children, setChildren] = useState([]);
  const [dailyStats, setDailyStats] = useState({
    totalMinutes: 0,
    focusScore: 0,
    blockedAttempts: 0,
    rewards: 0,
  });
  const [loading, setLoading] = useState(true);
  const [aiInsight, setAiInsight] = useState("");

  useEffect(() => {
    if (!user) return;
    const unsub = getChildren(user.uid, (kids) => {
      setChildren(kids);
      setLoading(false);
    });
    return unsub;
  }, [user]);

  useEffect(() => {
    if (!user || children.length === 0) return;
    const fetchStats = async () => {
      let totalMin = 0, totalScore = 0, totalBlocked = 0;
      for (const child of children) {
        const sessions = await getDailyReport(child.id);
        sessions.forEach((s) => {
          totalMin += s.durationMinutes || 0;
          totalScore += s.focusScore || 0;
          totalBlocked += s.blockedAttempts || 0;
        });
      }
      setDailyStats({
        totalMinutes: totalMin,
        focusScore: children.length > 0 ? Math.round(totalScore / children.length) || 0 : 0,
        blockedAttempts: totalBlocked,
        rewards: children.reduce((a, c) => a + (c.rewardsEarned || 0), 0),
      });
      generateInsight(children);
    };
    fetchStats();
  }, [children]);

  const generateInsight = (kids) => {
    const activeKid = kids.find((k) => k.sessionStatus === "active");
    if (activeKid) {
      setAiInsight(`${activeKid.name} is currently in a study session. Great focus!`);
    } else {
      const hour = new Date().getHours();
      if (hour >= 17 && hour <= 20) {
        setAiInsight("Peak study time is now (5–8 PM). Start a session to maximize focus.");
      } else if (hour > 21) {
        setAiInsight("Gaming distractions typically spike after 9 PM. Enable Study Shield to keep evenings productive.");
      } else {
        setAiInsight("Regular study sessions build long-term habits. Aim for at least 60 minutes today.");
      }
    }
  };

  const handleSignOut = async () => {
    await logOut();
    navigate("/login", { replace: true });
  };

  const parentName = parent?.name?.split(" ")[0] || "Parent";

  return (
    <div className="screen">
      {/* Header */}
      <div className="screen-header">
        <div className="header-row">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div className="logo-badge">🛡️</div>
            <span className="header-title">Study Shield</span>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn-icon" onClick={() => navigate("/settings")}>⚙️</button>
            <button className="btn-icon" onClick={handleSignOut} title="Sign out">🚪</button>
          </div>
        </div>
        <h2 className="header-h2">{getGreeting()}, {parentName} 👋</h2>
        <p className="header-sub">Keep your children focused and on track</p>
      </div>

      <div className="scroll-content">
        {/* Metric cards */}
        <div className="cards-grid">
          <div className="metric-card">
            <div className="mc-icon green">🕐</div>
            <span className="mc-label">Today's study</span>
            <span className="mc-value" style={{ color: "var(--green-dark)" }}>
              {dailyStats.totalMinutes > 0 ? formatDuration(dailyStats.totalMinutes) : "—"}
            </span>
            <span className="mc-sub">across all children</span>
          </div>
          <div className="metric-card">
            <div className="mc-icon blue">🎯</div>
            <span className="mc-label">Focus score</span>
            <span className="mc-value" style={{ color: "var(--blue)" }}>
              {dailyStats.focusScore > 0 ? `${dailyStats.focusScore}/100` : "—"}
            </span>
            <span className="mc-sub">average today</span>
          </div>
          <div className="metric-card">
            <div className="mc-icon amber">🚫</div>
            <span className="mc-label">Blocked</span>
            <span className="mc-value" style={{ color: "var(--amber)" }}>
              {dailyStats.blockedAttempts}
            </span>
            <span className="mc-sub">attempts today</span>
          </div>
          <div className="metric-card">
            <div className="mc-icon purple">🏆</div>
            <span className="mc-label">Rewards</span>
            <span className="mc-value" style={{ color: "var(--purple)" }}>
              {dailyStats.rewards}
            </span>
            <span className="mc-sub">earned this week</span>
          </div>
        </div>

        {/* Children list */}
        <p className="section-label">Your children</p>
        <div style={{ padding: "0 16px", display: "flex", flexDirection: "column", gap: 10 }}>
          {loading ? (
            <div style={{ textAlign: "center", padding: "32px 0" }}>
              <div className="spinner" style={{ margin: "0 auto" }} />
            </div>
          ) : children.length === 0 ? (
            <div style={{
              textAlign: "center", padding: "32px 20px",
              background: "var(--surface)", borderRadius: "var(--radius)",
              border: "1px dashed var(--border)",
            }}>
              <p style={{ fontSize: 14, color: "var(--text-2)", marginBottom: 12 }}>
                No children added yet
              </p>
              <button className="btn-primary" style={{ maxWidth: 200 }} onClick={() => navigate("/add-child")}>
                + Add child
              </button>
            </div>
          ) : (
            children.map((child, idx) => {
              const isActive = child.sessionStatus === "active";
              return (
                <div
                  key={child.id}
                  className="child-item"
                  onClick={() => isActive
                    ? navigate(`/session/focus/${child.id}`)
                    : navigate("/session/start", { state: { childId: child.id } })
                  }
                >
                  <div className={`avatar ${AVATAR_COLORS[idx % AVATAR_COLORS.length]}`}>
                    {getInitials(child.name)}
                  </div>
                  <div style={{ flex: 1 }}>
                    <p style={{ fontSize: 14, fontWeight: 600, color: "var(--text)" }}>{child.name}</p>
                    <p style={{ fontSize: 12, color: "var(--text-3)", marginTop: 2 }}>
                      {child.grade || "Class —"} · Streak: {child.currentStreak || 0} days 🔥
                    </p>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <div className={`status-dot ${isActive ? "active" : "idle"}`} />
                    <span className={`badge-pill ${isActive ? "active" : "idle"}`}>
                      {isActive ? "Active" : "Idle"}
                    </span>
                    <span style={{ fontSize: 18, color: "var(--text-3)" }}>›</span>
                  </div>
                </div>
              );
            })
          )}

          {children.length > 0 && (
            <button
              className="btn-secondary"
              onClick={() => navigate("/add-child")}
              style={{ marginTop: 4 }}
            >
              + Add child
            </button>
          )}
        </div>

        <div className="section-divider" style={{ marginTop: 12 }} />

        {/* Quick start */}
        <div className="quick-start-banner">
          <div className="qs-text">
            <h3>Quick start</h3>
            <p>Start a study session in one tap</p>
          </div>
          <button
            className="qs-btn"
            onClick={() => navigate("/session/start")}
            disabled={children.length === 0}
          >
            ▶ Start now
          </button>
        </div>

        {/* AI insight */}
        {aiInsight && (
          <div className="ai-box">
            <div className="ai-icon-box">✨</div>
            <p><strong>AI insight: </strong>{aiInsight}</p>
          </div>
        )}
      </div>

      <BottomNav active="home" />
    </div>
  );
}
