// src/screens/AddChildScreen.js
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { addChild } from "../services/firebase";

const GRADES = [
  "Class 1","Class 2","Class 3","Class 4","Class 5",
  "Class 6","Class 7","Class 8","Class 9","Class 10",
  "Class 11","Class 12",
];

export default function AddChildScreen() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [name, setName] = useState("");
  const [grade, setGrade] = useState("Class 8");
  const [deviceId, setDeviceId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleAdd = async () => {
    setError("");
    if (!name.trim()) { setError("Enter child's name"); return; }
    setLoading(true);
    try {
      await addChild(user.uid, {
        name: name.trim(),
        grade,
        deviceId: deviceId.trim() || null,
        sessionStatus: "idle",
        activeSessionId: null,
        totalStudyMinutes: 0,
        currentStreak: 0,
        longestStreak: 0,
        xp: 0,
        level: 1,
        badges: [],
        rewardsEarned: 0,
        totalSessions: 0,
      });
      navigate("/home");
    } catch (e) {
      setError("Failed to add child: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <button className="btn-icon" onClick={() => navigate(-1)}>←</button>
          <span className="header-title">Add child</span>
          <div style={{ width: 36 }} />
        </div>
      </div>

      <div style={{ padding: 20, display: "flex", flexDirection: "column", gap: 16 }}>
        {/* Icon */}
        <div style={{ display: "flex", justifyContent: "center", padding: "16px 0" }}>
          <div style={{
            width: 80, height: 80, borderRadius: "50%",
            background: "linear-gradient(135deg,#22C55E,#0EA5E9)",
            display: "flex", alignItems: "center", justifyContent: "center", fontSize: 40,
          }}>👧</div>
        </div>

        <div>
          <label style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", display: "block", marginBottom: 6 }}>
            Child's full name *
          </label>
          <input
            className="input-field"
            placeholder="e.g. Rohit Sharma"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus
          />
        </div>

        <div>
          <label style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", display: "block", marginBottom: 6 }}>
            Class / Grade *
          </label>
          <select
            className="input-field"
            value={grade}
            onChange={(e) => setGrade(e.target.value)}
            style={{ cursor: "pointer" }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>{g}</option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", display: "block", marginBottom: 6 }}>
            Android device ID (optional)
          </label>
          <input
            className="input-field"
            placeholder="From Study Shield Android app"
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
          />
          <p style={{ fontSize: 11, color: "var(--text-3)", marginTop: 6, lineHeight: 1.5 }}>
            Install the Study Shield Android agent on the child's phone and enter the Device ID shown on its setup screen. This enables real app blocking.
          </p>
        </div>

        {error && <p className="error-text">{error}</p>}

        <button className="btn-primary" onClick={handleAdd} disabled={loading}>
          {loading ? (
            <span className="spinner" style={{ width: 18, height: 18 }} />
          ) : (
            "➕ Add child"
          )}
        </button>

        <button className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
    </div>
  );
}
