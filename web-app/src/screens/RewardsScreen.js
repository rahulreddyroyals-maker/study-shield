// src/screens/RewardsScreen.js
import React, { useState, useEffect } from "react";
import { useAuth } from "../hooks/useAuth";
import {
  getRewards, addReward, getChildren, getDailyReport,
} from "../services/firebase";
import { formatDuration } from "../utils/pin";
import BottomNav from "../components/BottomNav";

const DEFAULT_REWARDS = [
  { studyMinutes: 30, reward: "10 min YouTube", icon: "📺", category: "daily" },
  { studyMinutes: 60, reward: "15 min YouTube", icon: "📺", category: "daily" },
  { studyMinutes: 120, reward: "30 min Gaming", icon: "🎮", category: "daily" },
  { studyMinutes: 900, reward: "Movie night 🎬", icon: "🎬", category: "weekly", weeklyMinutes: 900 },
  { studyMinutes: 0, reward: "Extra gaming time", icon: "⭐", category: "monthly", minScore: 80 },
];

export default function RewardsScreen() {
  const { user } = useAuth();
  const [rewards, setRewards] = useState([]);
  const [children, setChildren] = useState([]);
  const [selectedChild, setSelectedChild] = useState(null);
  const [todayMinutes, setTodayMinutes] = useState(0);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newReward, setNewReward] = useState({ studyMinutes: 30, reward: "", icon: "🎁" });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user) return;
    const unsub1 = getRewards(user.uid, setRewards);
    const unsub2 = getChildren(user.uid, (kids) => {
      setChildren(kids);
      if (kids.length > 0) setSelectedChild(kids[0]);
    });
    return () => { unsub1(); unsub2(); };
  }, [user]);

  useEffect(() => {
    if (!selectedChild) return;
    getDailyReport(selectedChild.id).then((sessions) => {
      setTodayMinutes(sessions.reduce((a, s) => a + (s.durationMinutes || 0), 0));
    });
  }, [selectedChild]);

  const handleAddReward = async () => {
    if (!newReward.reward.trim()) return;
    setLoading(true);
    try {
      await addReward(user.uid, newReward);
      setShowAddForm(false);
      setNewReward({ studyMinutes: 30, reward: "", icon: "🎁" });
    } catch (e) {
      alert("Failed to add reward");
    } finally {
      setLoading(false);
    }
  };

  const allRewards = [
    ...DEFAULT_REWARDS,
    ...rewards.map((r) => ({ ...r, custom: true })),
  ];

  // Goal progress — find next unlockable reward
  const nextReward = DEFAULT_REWARDS.filter((r) => r.studyMinutes > 0)
    .sort((a, b) => a.studyMinutes - b.studyMinutes)
    .find((r) => todayMinutes < r.studyMinutes);

  const goalPct = nextReward
    ? Math.min(100, (todayMinutes / nextReward.studyMinutes) * 100)
    : 100;

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div className="logo-badge">🎁</div>
            <span className="header-title">Rewards</span>
          </div>
        </div>
      </div>

      <div className="scroll-content">
        {/* Today's goal progress */}
        {nextReward && (
          <div style={{ padding: "16px 16px 0" }}>
            <div style={{
              background: "linear-gradient(135deg,#DCFCE7,#DBEAFE)",
              borderRadius: "var(--radius)", padding: 16,
              border: "1px solid #BBF7D0",
            }}>
              <p style={{ fontSize: 12, color: "var(--green-dark)", fontWeight: 600 }}>
                Today's goal · {selectedChild?.name?.split(" ")[0]}
              </p>
              <h3 style={{ fontSize: 16, fontWeight: 700, color: "#14532D", marginTop: 4 }}>
                Study for {formatDuration(nextReward.studyMinutes)}
              </h3>
              <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 10 }}>
                <div style={{ flex: 1 }}>
                  <div className="progress-bar-bg">
                    <div className="progress-bar-fill" style={{ width: `${goalPct}%` }} />
                  </div>
                  <p style={{ fontSize: 11, color: "#166534", marginTop: 4 }}>
                    {formatDuration(todayMinutes)} / {formatDuration(nextReward.studyMinutes)}
                  </p>
                </div>
                <div style={{ textAlign: "center", flexShrink: 0 }}>
                  <span style={{ fontSize: 32 }}>{nextReward.icon}</span>
                  <p style={{ fontSize: 11, color: "var(--green-dark)", fontWeight: 600, marginTop: 2 }}>
                    {nextReward.reward}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {todayMinutes > 0 && !nextReward && (
          <div style={{
            margin: "16px 16px 0",
            background: "var(--green-light)", borderRadius: "var(--radius)",
            padding: 16, textAlign: "center", border: "1px solid #BBF7D0",
          }}>
            <p style={{ fontSize: 24 }}>🏆</p>
            <p style={{ fontSize: 14, fontWeight: 700, color: "var(--green-dark)", marginTop: 4 }}>
              All daily rewards unlocked!
            </p>
            <p style={{ fontSize: 12, color: "var(--text-2)", marginTop: 2 }}>
              {selectedChild?.name} studied {formatDuration(todayMinutes)} today
            </p>
          </div>
        )}

        {/* Reward rules */}
        <p className="section-label">Reward rules</p>
        <div style={{
          margin: "0 16px 12px",
          borderRadius: "var(--radius)",
          overflow: "hidden",
          border: "1px solid var(--border)",
        }}>
          {allRewards.map((r, i) => {
            const earned = r.studyMinutes > 0 && todayMinutes >= r.studyMinutes;
            return (
              <div key={i} className="reward-row">
                <div style={{
                  width: 38, height: 38, borderRadius: 10,
                  background: earned ? "var(--green-light)" : "var(--surface-2)",
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 20, flexShrink: 0,
                }}>
                  {r.icon}
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontSize: 13, fontWeight: 600, color: "var(--text)" }}>
                    {r.category === "daily"
                      ? `${formatDuration(r.studyMinutes)} study`
                      : r.category === "weekly"
                      ? "15h this week"
                      : "80+ focus score / month"}
                    {r.custom && " (custom)"}
                  </p>
                  <p style={{ fontSize: 11, color: "var(--text-3)" }}>
                    {r.category === "daily" ? "Daily session" : r.category === "weekly" ? "Weekly challenge" : "Monthly goal"}
                  </p>
                </div>
                <span style={{
                  fontSize: 11, fontWeight: 600,
                  padding: "4px 10px", borderRadius: 20,
                  background: earned ? "var(--green-light)" : "var(--surface-2)",
                  color: earned ? "var(--green-dark)" : "var(--text-3)",
                  whiteSpace: "nowrap",
                }}>
                  {earned ? "✓ " : ""}{r.reward}
                </span>
              </div>
            );
          })}
        </div>

        {/* Add custom reward */}
        {showAddForm ? (
          <div style={{
            margin: "0 16px 12px",
            background: "var(--surface)", borderRadius: "var(--radius)",
            border: "1px solid var(--border)", padding: 16,
          }}>
            <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Add custom reward</p>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <input
                className="input-field"
                placeholder="Reward name (e.g. Ice cream 🍦)"
                value={newReward.reward}
                onChange={(e) => setNewReward({ ...newReward, reward: e.target.value })}
              />
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  className="input-field"
                  type="number"
                  placeholder="Study minutes required"
                  value={newReward.studyMinutes}
                  onChange={(e) => setNewReward({ ...newReward, studyMinutes: parseInt(e.target.value) || 0 })}
                />
                <input
                  className="input-field"
                  placeholder="Icon emoji"
                  value={newReward.icon}
                  onChange={(e) => setNewReward({ ...newReward, icon: e.target.value.slice(-2) || "🎁" })}
                  style={{ width: 80 }}
                />
              </div>
              <div style={{ display: "flex", gap: 8 }}>
                <button className="btn-primary" onClick={handleAddReward} disabled={loading}>
                  {loading ? "Saving..." : "Save reward"}
                </button>
                <button className="btn-secondary" style={{ width: "auto", padding: "12px 20px" }}
                  onClick={() => setShowAddForm(false)}>
                  Cancel
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div style={{ padding: "0 16px 16px" }}>
            <button className="btn-primary" onClick={() => setShowAddForm(true)}>
              + Add custom reward
            </button>
          </div>
        )}

        {/* How it works */}
        <div style={{
          margin: "0 16px 16px",
          background: "var(--surface-2)", borderRadius: "var(--radius)", padding: 14,
        }}>
          <p style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>How it works</p>
          <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
            <span style={{ fontSize: 12, color: "var(--text-2)" }}>📚 Study 60 min</span>
            <span style={{ color: "var(--text-3)" }}>→</span>
            <span style={{ fontSize: 12, color: "var(--text-2)" }}>✅ Goal unlocked</span>
            <span style={{ color: "var(--text-3)" }}>→</span>
            <span style={{ fontSize: 12, color: "var(--text-2)" }}>📺 15 min YouTube</span>
          </div>
        </div>
      </div>

      <BottomNav active="rewards" />
    </div>
  );
}
