// src/screens/StartSession.js
import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { getChildren, startSession, DEFAULT_ALLOWED_APPS } from "../services/firebase";

const DURATIONS = [
  { label: "30 min", value: 30 },
  { label: "60 min", value: 60 },
  { label: "90 min", value: 90 },
  { label: "Custom", value: 0 },
];

const AVATAR_COLORS = ["av-blue", "av-pink", "av-green", "av-amber", "av-purple"];
const getInitials = (name) =>
  name.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();

export default function StartSession() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [children, setChildren] = useState([]);
  const [selectedChild, setSelectedChild] = useState(location.state?.childId || "");
  const [selectedDur, setSelectedDur] = useState(60);
  const [customDur, setCustomDur] = useState("");
  const [allowedApps, setAllowedApps] = useState(
    DEFAULT_ALLOWED_APPS.map((a) => a.id)
  );
  const [loading, setLoading] = useState(false);
  const [showCustom, setShowCustom] = useState(false);

  useEffect(() => {
    if (!user) return;
    const unsub = getChildren(user.uid, setChildren);
    return unsub;
  }, [user]);

  const toggleApp = (appId) => {
    setAllowedApps((prev) =>
      prev.includes(appId) ? prev.filter((id) => id !== appId) : [...prev, appId]
    );
  };

  const handleStart = async () => {
    if (!selectedChild) {
      alert("Please select a child");
      return;
    }
    const duration = showCustom ? parseInt(customDur) || 60 : selectedDur;
    if (duration < 5 || duration > 480) {
      alert("Duration must be between 5 and 480 minutes");
      return;
    }
    setLoading(true);
    try {
      const sessionData = {
        durationMinutes: duration,
        durationSeconds: duration * 60,
        allowedApps: DEFAULT_ALLOWED_APPS.filter((a) => allowedApps.includes(a.id)),
        subject: "General",
      };
      await startSession(selectedChild, user.uid, sessionData);
      navigate(`/session/focus/${selectedChild}`);
    } catch (e) {
      alert("Failed to start session: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <button className="btn-icon" onClick={() => navigate("/home")}>←</button>
          <span className="header-title">Start study session</span>
          <div style={{ width: 36 }} />
        </div>
      </div>

      <div className="scroll-content">
        {/* Select child */}
        <p className="section-label">Select child</p>
        <div style={{ padding: "0 16px", display: "flex", flexDirection: "column", gap: 8 }}>
          {children.length === 0 ? (
            <p style={{ fontSize: 14, color: "var(--text-3)", textAlign: "center", padding: "16px 0" }}>
              Add a child first from the home screen
            </p>
          ) : (
            children.map((child, idx) => (
              <div
                key={child.id}
                className={`child-item ${selectedChild === child.id ? "selected" : ""}`}
                onClick={() => setSelectedChild(child.id)}
              >
                <div className={`avatar ${AVATAR_COLORS[idx % AVATAR_COLORS.length]}`}>
                  {getInitials(child.name)}
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontSize: 14, fontWeight: 600 }}>{child.name}</p>
                  <p style={{ fontSize: 12, color: "var(--text-3)" }}>{child.grade || "—"}</p>
                </div>
                {selectedChild === child.id && (
                  <span style={{ color: "var(--green)", fontSize: 20, fontWeight: 700 }}>✓</span>
                )}
              </div>
            ))
          )}
        </div>

        {/* Duration */}
        <p className="section-label">Select duration</p>
        <div style={{ display: "flex", gap: 8, padding: "0 16px" }}>
          {DURATIONS.map((d) => (
            <button
              key={d.label}
              className={`dur-pill ${
                (d.value === 0 ? showCustom : selectedDur === d.value && !showCustom)
                  ? "selected"
                  : ""
              }`}
              onClick={() => {
                if (d.value === 0) {
                  setShowCustom(true);
                } else {
                  setShowCustom(false);
                  setSelectedDur(d.value);
                }
              }}
            >
              {d.label}
            </button>
          ))}
        </div>
        {showCustom && (
          <div style={{ padding: "10px 16px 0" }}>
            <input
              className="input-field"
              type="number"
              placeholder="Enter minutes (5–480)"
              value={customDur}
              onChange={(e) => setCustomDur(e.target.value)}
              min={5}
              max={480}
            />
          </div>
        )}

        {/* Allowed apps */}
        <p className="section-label">Allowed apps</p>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 10, padding: "0 16px" }}>
          {DEFAULT_ALLOWED_APPS.map((app) => {
            const icons = {
              school: "🏫", book: "📚", layers: "📖", calculator: "🔢",
              phone: "📞", message: "💬", "file-text": "📝", "book-2": "📖",
              "file-pdf": "📄", camera: "📷",
            };
            return (
              <div
                key={app.id}
                className={`app-chip ${allowedApps.includes(app.id) ? "selected" : ""}`}
                onClick={() => toggleApp(app.id)}
              >
                <span className="chip-icon">{icons[app.icon] || "📱"}</span>
                <span>{app.name}</span>
              </div>
            );
          })}
        </div>

        <div style={{ padding: 16 }}>
          <button
            className="btn-primary"
            onClick={handleStart}
            disabled={loading || !selectedChild}
          >
            {loading ? (
              <span className="spinner" style={{ width: 18, height: 18 }} />
            ) : (
              "🛡️ Start Study Shield"
            )}
          </button>
          <p style={{ fontSize: 11, color: "var(--text-3)", textAlign: "center", marginTop: 8 }}>
            🔒 Parent PIN required to stop session
          </p>
        </div>
      </div>
    </div>
  );
}
