// src/screens/PinUnlock.js
import React, { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import {
  getActiveSession,
  endSession,
  getParent,
} from "../services/firebase";
import { verifyPin, hashPin, calcFocusScore } from "../utils/pin";

export default function PinUnlock() {
  const navigate = useNavigate();
  const { childId } = useParams();
  const { user } = useAuth();
  const [pin, setPin] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [shake, setShake] = useState(false);

  const handleKey = (val) => {
    if (loading) return;
    if (val === "del") {
      setPin((p) => p.slice(0, -1));
      return;
    }
    if (val === "clear") {
      setPin("");
      return;
    }
    if (pin.length >= 4) return;
    const newPin = pin + val;
    setPin(newPin);
    if (newPin.length === 4) {
      setTimeout(() => verifyAndStop(newPin), 200);
    }
  };

  const verifyAndStop = async (enteredPin) => {
    setLoading(true);
    setError("");
    try {
      const parent = await getParent(user.uid);
      if (!parent?.pinHash) {
        // No PIN set — use default 1234
        const defaultHash = await hashPin("1234");
        if (!(await verifyPin(enteredPin, defaultHash))) {
          showError("Incorrect PIN. Default PIN is 1234.");
          return;
        }
      } else {
        const ok = await verifyPin(enteredPin, parent.pinHash);
        if (!ok) {
          showError("Incorrect PIN. Try again.");
          return;
        }
      }

      // Stop session
      let sessionId = null;
      let blockedAttempts = 0;
      let durationMinutes = 0;

      await new Promise((resolve) => {
        const unsub = getActiveSession(childId, (sess) => {
          if (sess) {
            sessionId = sess.id;
            blockedAttempts = sess.blockedAttempts || 0;
            durationMinutes = sess.durationMinutes || 0;
          }
          unsub();
          resolve();
        });
      });

      if (sessionId) {
        const score = calcFocusScore(durationMinutes, blockedAttempts);
        await endSession(sessionId, childId, { blockedAttempts, durationMinutes });
        alert(`Session ended! Focus Score: ${score}/100 🎉`);
      }

      navigate("/home", { replace: true });
    } catch (e) {
      showError("Something went wrong. Try again.");
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const showError = (msg) => {
    setError(msg);
    setPin("");
    setShake(true);
    setTimeout(() => setShake(false), 600);
  };

  const handleForgotPin = async () => {
    alert("OTP sent to your registered mobile number. Use it to reset your PIN in Settings.");
  };

  const keys = [
    ["1", "2", "3"],
    ["4", "5", "6"],
    ["7", "8", "9"],
    ["clear", "0", "del"],
  ];

  return (
    <div className="screen" style={{ background: "var(--bg)" }}>
      {/* Header */}
      <div style={{
        background: "var(--navy)",
        padding: "52px 20px 40px",
        display: "flex", flexDirection: "column", alignItems: "center", gap: 16,
      }}>
        <div style={{
          width: 64, height: 64, borderRadius: "50%",
          background: "rgba(255,255,255,0.1)",
          display: "flex", alignItems: "center", justifyContent: "center", fontSize: 32,
        }}>🔒</div>
        <h2 style={{ color: "white", fontSize: 20, fontWeight: 700 }}>Enter parent PIN</h2>
        <p style={{ color: "#94A3B8", fontSize: 13, textAlign: "center" }}>
          Enter your 4-digit PIN to stop the session
        </p>

        {/* Dots */}
        <div
          style={{ display: "flex", gap: 16, marginTop: 8 }}
          className={shake ? "shake" : ""}
        >
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className={`pin-dot ${i < pin.length ? "filled" : ""}`} />
          ))}
        </div>
      </div>

      {/* Keypad */}
      <div style={{ padding: "24px 20px 0" }}>
        {error && (
          <p style={{ color: "var(--red)", fontSize: 13, textAlign: "center", marginBottom: 12 }}>
            {error}
          </p>
        )}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 12 }}>
          {keys.flat().map((key) => (
            <button
              key={key}
              onClick={() => handleKey(key)}
              disabled={loading}
              style={{
                background: "var(--surface)",
                border: "1px solid var(--border)",
                borderRadius: 12,
                padding: "18px 0",
                fontSize: key === "del" || key === "clear" ? 14 : 22,
                fontWeight: key === "del" || key === "clear" ? 500 : 600,
                cursor: "pointer",
                color: "var(--text)",
                transition: "background 0.15s, transform 0.1s",
                active: { transform: "scale(0.95)" },
              }}
              onMouseDown={(e) => e.currentTarget.style.background = "var(--surface-2)"}
              onMouseUp={(e) => e.currentTarget.style.background = "var(--surface)"}
            >
              {key === "del" ? "⌫" : key === "clear" ? "✕" : key}
            </button>
          ))}
        </div>

        <button
          className="btn-ghost"
          style={{ display: "block", margin: "16px auto 0", textAlign: "center" }}
          onClick={handleForgotPin}
        >
          Forgot PIN? Send OTP
        </button>

        <div style={{ padding: "16px 0" }}>
          <button
            className="btn-secondary"
            onClick={() => navigate(`/session/focus/${childId}`)}
          >
            ← Back to session
          </button>
        </div>
      </div>

      <style>{`
        .shake {
          animation: shake 0.5s ease;
        }
        @keyframes shake {
          0%,100%{transform:translateX(0)}
          20%{transform:translateX(-8px)}
          40%{transform:translateX(8px)}
          60%{transform:translateX(-6px)}
          80%{transform:translateX(6px)}
        }
      `}</style>
    </div>
  );
}
