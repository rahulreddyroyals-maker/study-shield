// src/screens/ReportsScreen.js
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { getChildren, getWeeklyReport, getDailyReport } from "../services/firebase";
import { formatDuration } from "../utils/pin";
import BottomNav from "../components/BottomNav";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const AVATAR_COLORS = ["av-blue", "av-pink", "av-green", "av-amber", "av-purple"];
const getInitials = (n) => n.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();

export default function ReportsScreen() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [children, setChildren] = useState([]);
  const [selectedChild, setSelectedChild] = useState(null);
  const [weekSessions, setWeekSessions] = useState([]);
  const [todaySessions, setTodaySessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    const unsub = getChildren(user.uid, (kids) => {
      setChildren(kids);
      if (kids.length > 0 && !selectedChild) setSelectedChild(kids[0]);
    });
    return unsub;
  }, [user]);

  useEffect(() => {
    if (!selectedChild) return;
    setLoading(true);
    Promise.all([
      getWeeklyReport(selectedChild.id),
      getDailyReport(selectedChild.id),
    ]).then(([weekly, daily]) => {
      setWeekSessions(weekly);
      setTodaySessions(daily);
      setLoading(false);
    });
  }, [selectedChild]);

  // Aggregate stats
  const totalWeekMinutes = weekSessions.reduce((a, s) => a + (s.durationMinutes || 0), 0);
  const totalBlocked = weekSessions.reduce((a, s) => a + (s.blockedAttempts || 0), 0);
  const avgScore = weekSessions.length
    ? Math.round(weekSessions.reduce((a, s) => a + (s.focusScore || 0), 0) / weekSessions.length)
    : 0;
  const todayMinutes = todaySessions.reduce((a, s) => a + (s.durationMinutes || 0), 0);

  // Most attempted app
  const appCounts = {};
  weekSessions.forEach((s) => {
    Object.entries(s.appsAttempted || {}).forEach(([app, count]) => {
      appCounts[app] = (appCounts[app] || 0) + count;
    });
  });
  const mostAttempted = Object.entries(appCounts).sort((a, b) => b[1] - a[1])[0]?.[0] || "—";

  // Build day-by-day bar data (last 7 days)
  const barData = Array(7).fill(0);
  const today = new Date();
  weekSessions.forEach((s) => {
    const date = s.startTime?.toDate?.() || new Date();
    const dayDiff = Math.floor((today - date) / (1000 * 60 * 60 * 24));
    const dayIdx = 6 - Math.min(dayDiff, 6);
    barData[dayIdx] += s.durationMinutes || 0;
  });
  const maxBar = Math.max(...barData, 1);

  // Best day
  const bestDayIdx = barData.indexOf(Math.max(...barData));
  const bestDay = DAYS[bestDayIdx] || "—";

  // AI insight
  const getInsight = () => {
    if (!selectedChild || weekSessions.length === 0)
      return "Start study sessions to see personalized insights.";
    if (avgScore >= 85)
      return `${selectedChild.name} is performing excellently this week with an avg score of ${avgScore}. Keep the momentum!`;
    if (totalBlocked > 50)
      return `${selectedChild.name} had ${totalBlocked} distraction attempts. Consider shorter, focused sessions.`;
    return `${selectedChild.name} studies best on ${bestDay}. Schedule important sessions on that day.`;
  };

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div className="logo-badge">📊</div>
            <span className="header-title">AI Focus Report</span>
          </div>
          <span style={{ color: "#94A3B8", fontSize: 12 }}>Last 7 days</span>
        </div>
      </div>

      <div className="scroll-content">
        {/* Child selector */}
        {children.length > 1 && (
          <div style={{ padding: "12px 16px 0", display: "flex", gap: 8, overflowX: "auto" }}>
            {children.map((child, idx) => (
              <button
                key={child.id}
                onClick={() => setSelectedChild(child)}
                style={{
                  display: "flex", alignItems: "center", gap: 8,
                  padding: "8px 14px",
                  borderRadius: 20,
                  border: `1.5px solid ${selectedChild?.id === child.id ? "var(--green)" : "var(--border)"}`,
                  background: selectedChild?.id === child.id ? "var(--green-light)" : "var(--surface)",
                  cursor: "pointer", whiteSpace: "nowrap", fontSize: 13, fontWeight: 500,
                }}
              >
                <div className={`avatar ${AVATAR_COLORS[idx % AVATAR_COLORS.length]}`}
                  style={{ width: 24, height: 24, fontSize: 10 }}>
                  {getInitials(child.name)}
                </div>
                {child.name.split(" ")[0]}
              </button>
            ))}
          </div>
        )}

        {loading ? (
          <div style={{ display: "flex", justifyContent: "center", padding: 40 }}>
            <div className="spinner" />
          </div>
        ) : (
          <>
            {/* Metric cards */}
            <div className="cards-grid">
              <div className="metric-card">
                <span className="mc-label">Study time</span>
                <span className="mc-value" style={{ color: "var(--green-dark)", fontSize: 20 }}>
                  {formatDuration(totalWeekMinutes)}
                </span>
                <span className="mc-sub">this week</span>
              </div>
              <div className="metric-card">
                <span className="mc-label">Focus score</span>
                <span className="mc-value" style={{ color: "var(--blue)" }}>
                  {avgScore > 0 ? `${avgScore}/100` : "—"}
                </span>
                <span className="mc-sub">{avgScore >= 80 ? "Good 👍" : avgScore > 0 ? "Needs work" : "No data"}</span>
              </div>
              <div className="metric-card">
                <span className="mc-label">Blocked</span>
                <span className="mc-value" style={{ color: "var(--amber)" }}>{totalBlocked}</span>
                <span className="mc-sub">attempts this week</span>
              </div>
              <div className="metric-card">
                <span className="mc-label">Today</span>
                <span className="mc-value" style={{ color: "var(--purple)", fontSize: 20 }}>
                  {formatDuration(todayMinutes)}
                </span>
                <span className="mc-sub">{todaySessions.length} session(s)</span>
              </div>
            </div>

            {/* Bar chart */}
            <div style={{
              background: "var(--surface)", borderRadius: "var(--radius)",
              border: "1px solid var(--border)", margin: "0 16px 12px", padding: 16,
            }}>
              <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Daily focus (minutes)</p>
              <div className="bar-chart-wrap">
                {barData.map((val, i) => (
                  <div
                    key={i}
                    className={`chart-bar ${i === 6 ? "today" : ""}`}
                    style={{ height: `${(val / maxBar) * 100}%`, minHeight: val > 0 ? 6 : 4 }}
                    title={`${DAYS[i]}: ${formatDuration(val)}`}
                  />
                ))}
              </div>
              <div style={{ display: "flex", gap: 6, marginTop: 6 }}>
                {DAYS.map((d, i) => (
                  <span key={i} className="chart-bar-label"
                    style={{ fontWeight: i === 6 ? 700 : 400, color: i === 6 ? "var(--green)" : undefined }}>
                    {d}
                  </span>
                ))}
              </div>
            </div>

            {/* Weekly summary */}
            <div style={{
              background: "var(--surface)", borderRadius: "var(--radius)",
              border: "1px solid var(--border)", margin: "0 16px 12px", padding: 16,
            }}>
              <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>
                Weekly summary — {selectedChild?.name?.split(" ")[0]}
              </p>
              {[
                ["Best study day", bestDay],
                ["Total sessions", weekSessions.length],
                ["Most attempted app", mostAttempted],
                ["Avg session length",
                  weekSessions.length
                    ? formatDuration(Math.round(totalWeekMinutes / weekSessions.length))
                    : "—"],
                ["Current streak", `${selectedChild?.currentStreak || 0} days 🔥`],
              ].map(([label, val]) => (
                <div key={label} style={{
                  display: "flex", justifyContent: "space-between", alignItems: "center",
                  padding: "9px 0", borderBottom: "1px solid var(--border)",
                }}>
                  <span style={{ fontSize: 13, color: "var(--text-2)" }}>{label}</span>
                  <span style={{ fontSize: 13, fontWeight: 600, color: "var(--text)" }}>{val}</span>
                </div>
              ))}
            </div>

            {/* Recent sessions */}
            {weekSessions.length > 0 && (
              <div style={{
                background: "var(--surface)", borderRadius: "var(--radius)",
                border: "1px solid var(--border)", margin: "0 16px 12px", padding: 16,
              }}>
                <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Recent sessions</p>
                {weekSessions.slice(0, 5).map((s, i) => {
                  const date = s.startTime?.toDate?.()?.toLocaleDateString("en-IN", {
                    weekday: "short", month: "short", day: "numeric",
                  }) || "—";
                  return (
                    <div key={i} style={{
                      display: "flex", justifyContent: "space-between",
                      padding: "9px 0", borderBottom: i < 4 ? "1px solid var(--border)" : "none",
                    }}>
                      <div>
                        <p style={{ fontSize: 13, fontWeight: 500 }}>{date}</p>
                        <p style={{ fontSize: 11, color: "var(--text-3)" }}>
                          {formatDuration(s.durationMinutes)} · {s.blockedAttempts || 0} blocked
                        </p>
                      </div>
                      <div style={{
                        background: (s.focusScore || 0) >= 80 ? "var(--green-light)" : "#FEF3C7",
                        borderRadius: 20, padding: "4px 12px",
                        fontSize: 12, fontWeight: 700,
                        color: (s.focusScore || 0) >= 80 ? "var(--green-dark)" : "#92400E",
                        alignSelf: "center",
                      }}>
                        {s.focusScore || 0}/100
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {weekSessions.length === 0 && (
              <div style={{
                textAlign: "center", padding: "32px 20px",
                background: "var(--surface)", borderRadius: "var(--radius)",
                border: "1px dashed var(--border)", margin: "0 16px 12px",
              }}>
                <p style={{ fontSize: 48, marginBottom: 8 }}>📊</p>
                <p style={{ fontSize: 14, color: "var(--text-2)" }}>No sessions this week yet</p>
                <button className="btn-primary" style={{ marginTop: 16, maxWidth: 200 }}
                  onClick={() => navigate("/session/start")}>
                  Start first session
                </button>
              </div>
            )}

            {/* AI suggestion */}
            <div className="ai-box">
              <div className="ai-icon-box">✨</div>
              <p><strong>AI insight: </strong>{getInsight()}</p>
            </div>
          </>
        )}
      </div>

      <BottomNav active="reports" />
    </div>
  );
}
