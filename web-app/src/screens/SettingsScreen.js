// src/screens/SettingsScreen.js
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { setParentPin, updateParent, logOut } from "../services/firebase";
import { hashPin } from "../utils/pin";

export default function SettingsScreen() {
  const navigate = useNavigate();
  const { user, parent, setParent } = useAuth();
  const [pinStep, setPinStep] = useState(null); // null | 'new' | 'confirm'
  const [pin1, setPin1] = useState("");
  const [pin2, setPin2] = useState("");
  const [pinError, setPinError] = useState("");
  const [pinSuccess, setPinSuccess] = useState("");
  const [nameEdit, setNameEdit] = useState(false);
  const [name, setName] = useState(parent?.name || "");
  const [saving, setSaving] = useState(false);
  const [notifications, setNotifications] = useState(true);
  const [weeklyReport, setWeeklyReport] = useState(true);

  const handleSetPin = async () => {
    if (pin1.length !== 4) { setPinError("PIN must be 4 digits"); return; }
    if (pinStep === "new") { setPinStep("confirm"); setPinError(""); return; }
    if (pin1 !== pin2) { setPinError("PINs don't match. Try again."); setPin2(""); return; }
    setSaving(true);
    try {
      const hashed = await hashPin(pin1);
      await setParentPin(user.uid, hashed);
      setPinSuccess("PIN set successfully! ✓");
      setPinStep(null); setPin1(""); setPin2("");
      setTimeout(() => setPinSuccess(""), 3000);
    } catch (e) {
      setPinError("Failed to set PIN. Try again.");
    } finally {
      setSaving(false);
    }
  };

  const handleSaveName = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      await updateParent(user.uid, { name: name.trim() });
      setParent({ ...parent, name: name.trim() });
      setNameEdit(false);
    } catch (e) {
      alert("Failed to save name");
    } finally {
      setSaving(false);
    }
  };

  const handleSignOut = async () => {
    if (window.confirm("Sign out of Study Shield?")) {
      await logOut();
      navigate("/login", { replace: true });
    }
  };

  const PinInput = ({ value, onChange, placeholder }) => (
    <input
      className="input-field"
      type="password"
      inputMode="numeric"
      maxLength={4}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value.replace(/\D/g, "").slice(0, 4))}
      style={{ textAlign: "center", fontSize: 24, letterSpacing: 8 }}
    />
  );

  const SettingsSection = ({ title }) => (
    <p style={{
      fontSize: 12, fontWeight: 700, color: "var(--text-3)",
      textTransform: "uppercase", letterSpacing: 0.6,
      padding: "16px 16px 8px",
    }}>{title}</p>
  );

  return (
    <div className="screen">
      <div className="screen-header">
        <div className="header-row">
          <button className="btn-icon" onClick={() => navigate("/home")}>←</button>
          <span className="header-title">Settings</span>
          <div style={{ width: 36 }} />
        </div>
      </div>

      <div className="scroll-content">
        {/* Profile */}
        <SettingsSection title="Profile" />
        <div style={{
          background: "var(--surface)", border: "1px solid var(--border)",
          borderRadius: "var(--radius)", margin: "0 16px 4px",
        }}>
          <div className="settings-row">
            <div className="sr-left">
              <div className="sr-icon" style={{ background: "var(--blue-light)", fontSize: 18 }}>👤</div>
              <div>
                {nameEdit ? (
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      className="input-field"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      style={{ padding: "6px 10px", fontSize: 14, width: 160 }}
                      autoFocus
                    />
                    <button
                      onClick={handleSaveName}
                      disabled={saving}
                      style={{
                        background: "var(--green)", color: "white", border: "none",
                        borderRadius: 8, padding: "6px 12px", cursor: "pointer", fontSize: 13,
                      }}
                    >{saving ? "..." : "Save"}</button>
                    <button onClick={() => setNameEdit(false)}
                      style={{ background: "none", border: "none", cursor: "pointer", fontSize: 13, color: "var(--text-3)" }}>
                      Cancel
                    </button>
                  </div>
                ) : (
                  <>
                    <p className="sr-label">{parent?.name || "Parent"}</p>
                    <p className="sr-sub">{user?.phoneNumber || user?.email || "—"}</p>
                  </>
                )}
              </div>
            </div>
            {!nameEdit && (
              <button onClick={() => setNameEdit(true)}
                style={{ background: "none", border: "none", color: "var(--blue)", fontSize: 13, cursor: "pointer" }}>
                Edit
              </button>
            )}
          </div>

          <div className="settings-row">
            <div className="sr-left">
              <div className="sr-icon" style={{ background: "#FEF3C7" }}>📋</div>
              <div>
                <p className="sr-label">Plan</p>
                <p className="sr-sub">{parent?.plan === "premium" ? "Premium" : "Free"}</p>
              </div>
            </div>
            {parent?.plan !== "premium" && (
              <button style={{
                background: "linear-gradient(135deg,#22C55E,#0EA5E9)",
                color: "white", border: "none", borderRadius: 8,
                padding: "6px 14px", cursor: "pointer", fontSize: 13, fontWeight: 600,
              }}>
                Upgrade
              </button>
            )}
          </div>
        </div>

        {/* Security */}
        <SettingsSection title="Security" />
        <div style={{
          background: "var(--surface)", border: "1px solid var(--border)",
          borderRadius: "var(--radius)", margin: "0 16px 4px",
        }}>
          <div className="settings-row" style={{ flexDirection: "column", alignItems: "flex-start", gap: 12 }}>
            <div className="sr-left">
              <div className="sr-icon" style={{ background: "var(--green-light)" }}>🔐</div>
              <div>
                <p className="sr-label">Parent PIN</p>
                <p className="sr-sub">
                  {parent?.pinHash ? "PIN is set" : "No PIN set (default: 1234)"}
                </p>
              </div>
            </div>

            {pinSuccess && <p style={{ color: "var(--green)", fontSize: 13, fontWeight: 600 }}>{pinSuccess}</p>}

            {pinStep === null ? (
              <button
                className="btn-secondary"
                style={{ marginLeft: 48 }}
                onClick={() => { setPinStep("new"); setPinError(""); }}
              >
                {parent?.pinHash ? "Change PIN" : "Set PIN"}
              </button>
            ) : (
              <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 10 }}>
                <p style={{ fontSize: 13, color: "var(--text-2)" }}>
                  {pinStep === "new" ? "Enter new 4-digit PIN" : "Confirm your PIN"}
                </p>
                <PinInput
                  value={pinStep === "new" ? pin1 : pin2}
                  onChange={pinStep === "new" ? setPin1 : setPin2}
                  placeholder="• • • •"
                />
                {pinError && <p className="error-text">{pinError}</p>}
                <div style={{ display: "flex", gap: 8 }}>
                  <button className="btn-primary" onClick={handleSetPin} disabled={saving}>
                    {saving ? "Saving..." : pinStep === "new" ? "Next →" : "Set PIN"}
                  </button>
                  <button className="btn-secondary" style={{ width: "auto", padding: "12px 16px" }}
                    onClick={() => { setPinStep(null); setPin1(""); setPin2(""); setPinError(""); }}>
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Notifications */}
        <SettingsSection title="Notifications" />
        <div style={{
          background: "var(--surface)", border: "1px solid var(--border)",
          borderRadius: "var(--radius)", margin: "0 16px 4px",
        }}>
          {[
            { icon: "🔔", label: "Push notifications", sub: "Session start/end alerts", val: notifications, set: setNotifications },
            { icon: "📊", label: "Weekly report", sub: "Every Sunday evening", val: weeklyReport, set: setWeeklyReport },
          ].map((item) => (
            <div key={item.label} className="settings-row">
              <div className="sr-left">
                <div className="sr-icon" style={{ background: "var(--surface-2)" }}>{item.icon}</div>
                <div>
                  <p className="sr-label">{item.label}</p>
                  <p className="sr-sub">{item.sub}</p>
                </div>
              </div>
              <button
                className={`toggle ${item.val ? "on" : ""}`}
                onClick={() => item.set(!item.val)}
              />
            </div>
          ))}
        </div>

        {/* About */}
        <SettingsSection title="About" />
        <div style={{
          background: "var(--surface)", border: "1px solid var(--border)",
          borderRadius: "var(--radius)", margin: "0 16px 4px",
        }}>
          {[
            { icon: "📄", label: "Privacy Policy", action: () => {} },
            { icon: "📋", label: "Terms of Service", action: () => {} },
            { icon: "💬", label: "Contact Support", action: () => {} },
          ].map((item) => (
            <div key={item.label} className="settings-row" onClick={item.action} style={{ cursor: "pointer" }}>
              <div className="sr-left">
                <div className="sr-icon" style={{ background: "var(--surface-2)" }}>{item.icon}</div>
                <p className="sr-label">{item.label}</p>
              </div>
              <span style={{ color: "var(--text-3)" }}>›</span>
            </div>
          ))}
          <div className="settings-row">
            <div className="sr-left">
              <div className="sr-icon" style={{ background: "var(--surface-2)" }}>ℹ️</div>
              <div>
                <p className="sr-label">Version</p>
                <p className="sr-sub">Study Shield v1.0.0 · Powered by School Connect</p>
              </div>
            </div>
          </div>
        </div>

        {/* Sign out */}
        <div style={{ padding: "16px" }}>
          <button
            className="btn-secondary"
            onClick={handleSignOut}
            style={{ color: "var(--red)", borderColor: "var(--red)" }}
          >
            🚪 Sign out
          </button>
        </div>
      </div>
    </div>
  );
}
