// src/screens/ChildFocusScreen.js
import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { getActiveSession, db } from "../services/firebase";
import { doc, onSnapshot } from "firebase/firestore";
import { formatTimer } from "../utils/pin";

export default function ChildFocusScreen() {
  const navigate = useNavigate();
  const { childId } = useParams();
  const { user } = useAuth();
  const [session, setSession] = useState(null);
  const [child, setChild] = useState(null);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [loading, setLoading] = useState(true);
  const intervalRef = useRef(null);

  // Listen to child doc
  useEffect(() => {
    const unsub = onSnapshot(doc(db, "children", childId), (snap) => {
      if (snap.exists()) setChild({ id: snap.id, ...snap.data() });
    });
    return unsub;
  }, [childId]);

  // Listen to active session
  useEffect(() => {
    const unsub = getActiveSession(childId, (sess) => {
      setSession(sess);
      setLoading(false);
      if (sess) {
        // Calculate remaining time from start + duration - now
        const startMs = sess.startTime?.toMillis?.() || Date.now();
        const totalSec = (sess.durationMinutes || 60) * 60;
        const elapsedSec = Math.floor((Date.now() - startMs) / 1000);
        setSecondsLeft(Math.max(0, totalSec - elapsedSec));
      }
    });
    return unsub;
  }, [childId]);

  // Countdown timer
  useEffect(() => {
    if (!session) return;
    if (intervalRef.current) clearInterval(intervalRef.current);
    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current);
          // Session auto-ended — redirect to reports
          setTimeout(() => navigate("/reports"), 1500);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(intervalRef.current);
  }, [session]);

  const totalSeconds = (session?.durationMinutes || 60) * 60;
  const progressPct = totalSeconds > 0 ? (1 - secondsLeft / totalSeconds) * 100 : 0;

  if (loading) {
    return (
      <div className="screen" style={{ alignItems: "center", justifyContent: "center" }}>
        <div className="spinner" />
      </div>
    );
  }

  if (!session) {
    return (
      <div className="screen" style={{ alignItems: "center", justifyContent: "center", padding: 24 }}>
        <p style={{ fontSize: 16, color: "var(--text-2)", textAlign: "center", marginBottom: 20 }}>
          No active session found
        </p>
        <button className="btn-primary" onClick={() => navigate("/session/start")}>
          Start a session
        </button>
      </div>
    );
  }

  const childName = child?.name || "Student";
  const childGrade = child?.grade || "";

  return (
    <div className="screen">
      {/* Focus hero */}
      <div style={{
        background: "var(--navy)",
        padding: "52px 20px 32px",
        display: "flex", flexDirection: "column", alignItems: "center", gap: 14,
      }}>
        {/* Animated shield */}
        <div style={{
          width: 88, height: 88,
          background: "linear-gradient(135deg,#22C55E,#0EA5E9)",
          borderRadius: "50%",
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 44,
          boxShadow: "0 0 0 16px rgba(34,197,94,0.1)",
        }}>🛡️</div>

        <div style={{ color: "#22C55E", fontSize: 14, fontWeight: 600 }}>
          ✓ Study Shield active
        </div>

        <p style={{ color: "#CBD5E1", fontSize: 15 }}>
          {childName} · {childGrade}
        </p>

        {/* Timer */}
        <div className="timer-display">{formatTimer(secondsLeft)}</div>
        <p style={{ color: "#64748B", fontSize: 12 }}>time remaining</p>

        {/* Progress ring (linear bar) */}
        <div style={{ width: "100%", maxWidth: 280 }}>
          <div className="progress-bar-bg" style={{ height: 6 }}>
            <div
              className="progress-bar-fill"
              style={{ width: `${progressPct}%` }}
            />
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}>
            <span style={{ fontSize: 10, color: "#64748B" }}>0</span>
            <span style={{ fontSize: 10, color: "#22C55E", fontWeight: 600 }}>
              {Math.round(progressPct)}% complete
            </span>
            <span style={{ fontSize: 10, color: "#64748B" }}>{session.durationMinutes}m</span>
          </div>
        </div>
      </div>

      <div className="scroll-content">
        {/* Allowed apps */}
        <p className="section-label">Allowed apps</p>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 10, padding: "0 16px" }}>
          {(session.allowedApps || []).map((app) => {
            const icons = {
              school: "🏫", book: "📚", layers: "📖", calculator: "🔢",
              phone: "📞", message: "💬", "file-text": "📝", "book-2": "📖",
              "file-pdf": "📄", camera: "📷",
            };
            return (
              <div key={app.id} className="app-chip">
                <span className="chip-icon">{icons[app.icon] || "📱"}</span>
                <span>{app.name}</span>
              </div>
            );
          })}
        </div>

        {/* Blocked attempts live */}
        <div style={{
          margin: "12px 16px",
          background: "var(--surface)",
          borderRadius: "var(--radius)",
          border: "1px solid var(--border)",
          padding: "12px 14px",
          display: "flex", justifyContent: "space-between",
        }}>
          <span style={{ fontSize: 13, color: "var(--text-2)" }}>Blocked attempts</span>
          <span style={{ fontSize: 13, fontWeight: 700, color: "var(--red)" }}>
            {session.blockedAttempts || 0}
          </span>
        </div>

        {/* Stop session */}
        <div style={{ padding: "0 16px 12px" }}>
          <button
            className="btn-secondary"
            onClick={() => navigate(`/session/unlock/${childId}`)}
          >
            🔒 Stop session (parent PIN)
          </button>
        </div>
      </div>

      {/* Emergency */}
      <button
        className="emergency-btn"
        onClick={() => {
          if (window.confirm("Send emergency alert to parent?")) {
            alert("Emergency alert sent! Parent notified.");
          }
        }}
      >
        📞 Emergency call
      </button>
    </div>
  );
}
