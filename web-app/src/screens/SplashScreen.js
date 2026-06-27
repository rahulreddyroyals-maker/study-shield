// src/screens/SplashScreen.js
import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function SplashScreen() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();

  useEffect(() => {
    if (loading) return;
    const timer = setTimeout(() => {
      navigate(user ? "/home" : "/login", { replace: true });
    }, 2200);
    return () => clearTimeout(timer);
  }, [user, loading, navigate]);

  return (
    <div className="splash-screen">
      {/* Logo */}
      <div style={{
        width: 110, height: 110,
        background: "linear-gradient(135deg,#22C55E,#0EA5E9)",
        borderRadius: 28,
        display: "flex", alignItems: "center", justifyContent: "center",
        boxShadow: "0 20px 60px rgba(34,197,94,0.3)",
      }}>
        <span style={{ fontSize: 56 }}>🛡️</span>
      </div>

      <div style={{ textAlign: "center" }}>
        <h1 style={{ color: "white", fontSize: 36, fontWeight: 800, letterSpacing: -1 }}>
          Study <span style={{ color: "#22C55E" }}>Shield</span>
        </h1>
        <p style={{ color: "#22C55E", fontSize: 15, fontWeight: 500, marginTop: 6 }}>
          Focus Today. Succeed Tomorrow.
        </p>
      </div>

      {/* Powered by */}
      <div style={{
        background: "rgba(255,255,255,0.08)",
        borderRadius: 20,
        padding: "8px 18px",
        display: "flex", alignItems: "center", gap: 8,
        color: "#CBD5E1", fontSize: 12,
        marginTop: 8,
      }}>
        🔗 Powered by <span style={{ color: "#0EA5E9", fontWeight: 700 }}>School Connect</span>
      </div>

      <div style={{ marginTop: 40 }}>
        <div className="spinner" style={{ borderTopColor: "#22C55E", borderColor: "rgba(34,197,94,0.2)" }} />
      </div>

      <p style={{ color: "#475569", fontSize: 12, textAlign: "center", position: "absolute", bottom: 32 }}>
        v1.0.0 · School Connect ERP Add-on
      </p>
    </div>
  );
}
