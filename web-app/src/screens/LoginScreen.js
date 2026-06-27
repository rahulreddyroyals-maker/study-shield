// src/screens/LoginScreen.js
import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  setupRecaptcha,
  sendOTP,
  signInWithGoogle,
} from "../services/firebase";

export default function LoginScreen() {
  const navigate = useNavigate();
  const [step, setStep] = useState("phone"); // phone | otp
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [confirmResult, setConfirmResult] = useState(null);
  const recaptchaRef = useRef(null);
  const otpRefs = useRef([]);

  const formatPhone = (val) => {
    const digits = val.replace(/\D/g, "");
    if (digits.length <= 5) return digits;
    return digits.slice(0, 5) + " " + digits.slice(5, 10);
  };

  const handleSendOTP = async () => {
    setError("");
    const digits = phone.replace(/\D/g, "");
    if (digits.length !== 10) {
      setError("Enter a valid 10-digit mobile number");
      return;
    }
    setLoading(true);
    try {
      if (!recaptchaRef.current) {
        recaptchaRef.current = setupRecaptcha("recaptcha-container");
      }
      const result = await sendOTP("+91" + digits, recaptchaRef.current);
      setConfirmResult(result);
      setStep("otp");
    } catch (e) {
      setError("Failed to send OTP. Check the number and try again.");
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleOtpChange = (idx, val) => {
    if (!/^\d?$/.test(val)) return;
    const newOtp = [...otp];
    newOtp[idx] = val;
    setOtp(newOtp);
    if (val && idx < 5) otpRefs.current[idx + 1]?.focus();
    if (!val && idx > 0) otpRefs.current[idx - 1]?.focus();
  };

  const handleVerifyOTP = async () => {
    setError("");
    const code = otp.join("");
    if (code.length !== 6) {
      setError("Enter the 6-digit OTP");
      return;
    }
    setLoading(true);
    try {
      await confirmResult.confirm(code);
      navigate("/home", { replace: true });
    } catch (e) {
      setError("Incorrect OTP. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogle = async () => {
    setError("");
    setLoading(true);
    try {
      await signInWithGoogle();
      navigate("/home", { replace: true });
    } catch (e) {
      setError("Google sign-in failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "#0D1B3E", display: "flex", flexDirection: "column" }}>
      {/* Top illustration */}
      <div style={{ padding: "60px 24px 32px", display: "flex", flexDirection: "column", alignItems: "center", gap: 16 }}>
        <div style={{
          width: 80, height: 80,
          background: "linear-gradient(135deg,#22C55E,#0EA5E9)",
          borderRadius: 22,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 42,
        }}>🛡️</div>
        <div style={{ textAlign: "center" }}>
          <h1 style={{ color: "white", fontSize: 28, fontWeight: 800 }}>
            Study <span style={{ color: "#22C55E" }}>Shield</span>
          </h1>
          <p style={{ color: "#64748B", fontSize: 13, marginTop: 4 }}>
            Parent sign-in · Powered by School Connect
          </p>
        </div>
      </div>

      {/* Form card */}
      <div style={{
        flex: 1,
        background: "#F8FAFC",
        borderRadius: "28px 28px 0 0",
        padding: "28px 24px",
      }}>
        {step === "phone" ? (
          <>
            <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0F172A" }}>Welcome, Parent 👋</h2>
            <p style={{ fontSize: 13, color: "#64748B", marginTop: 4, marginBottom: 24 }}>
              Enter your mobile number to get started
            </p>

            <label style={{ fontSize: 13, fontWeight: 600, color: "#475569", display: "block", marginBottom: 8 }}>
              Mobile number
            </label>
            <div style={{ display: "flex", gap: 8 }}>
              <div style={{
                background: "#E2E8F0", borderRadius: 10, padding: "12px 14px",
                fontSize: 15, fontWeight: 600, color: "#475569",
                border: "1px solid #CBD5E1",
              }}>+91</div>
              <input
                className="input-field"
                type="tel"
                placeholder="98765 43210"
                value={phone}
                onChange={(e) => setPhone(formatPhone(e.target.value))}
                maxLength={11}
                onKeyDown={(e) => e.key === "Enter" && handleSendOTP()}
              />
            </div>
            {error && <p className="error-text">{error}</p>}

            <button
              className="btn-primary"
              style={{ marginTop: 20 }}
              onClick={handleSendOTP}
              disabled={loading}
            >
              {loading ? <span className="spinner" style={{ width: 18, height: 18 }} /> : "Send OTP →"}
            </button>

            <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "20px 0" }}>
              <div style={{ flex: 1, height: 1, background: "#E2E8F0" }} />
              <span style={{ fontSize: 12, color: "#94A3B8" }}>or</span>
              <div style={{ flex: 1, height: 1, background: "#E2E8F0" }} />
            </div>

            <button
              className="btn-secondary"
              onClick={handleGoogle}
              disabled={loading}
              style={{ gap: 10 }}
            >
              <span style={{ fontSize: 18 }}>🔵</span> Continue with Google
            </button>

            <p style={{ fontSize: 11, color: "#94A3B8", textAlign: "center", marginTop: 24, lineHeight: 1.5 }}>
              By continuing you agree to our Terms of Service and Privacy Policy.
              Study Shield is a parental control add-on for School Connect ERP.
            </p>
          </>
        ) : (
          <>
            <button
              onClick={() => setStep("phone")}
              style={{ background: "none", border: "none", fontSize: 22, cursor: "pointer", marginBottom: 16, padding: 0 }}
            >←</button>
            <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0F172A" }}>Verify OTP</h2>
            <p style={{ fontSize: 13, color: "#64748B", marginTop: 4, marginBottom: 24 }}>
              Enter the 6-digit code sent to +91 {phone}
            </p>

            <div style={{ display: "flex", gap: 8, justifyContent: "center", marginBottom: 8 }}>
              {otp.map((digit, idx) => (
                <input
                  key={idx}
                  ref={(el) => (otpRefs.current[idx] = el)}
                  type="tel"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleOtpChange(idx, e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Backspace" && !otp[idx] && idx > 0) {
                      otpRefs.current[idx - 1]?.focus();
                    }
                  }}
                  style={{
                    width: 44, height: 52,
                    textAlign: "center", fontSize: 22, fontWeight: 700,
                    border: `2px solid ${digit ? "#22C55E" : "#E2E8F0"}`,
                    borderRadius: 10,
                    background: digit ? "#F0FDF4" : "#F8FAFC",
                    outline: "none", color: "#0F172A",
                    transition: "border-color 0.2s",
                  }}
                />
              ))}
            </div>
            {error && <p className="error-text">{error}</p>}

            <button
              className="btn-primary"
              style={{ marginTop: 20 }}
              onClick={handleVerifyOTP}
              disabled={loading}
            >
              {loading ? <span className="spinner" style={{ width: 18, height: 18 }} /> : "Verify & Sign In →"}
            </button>

            <button
              className="btn-ghost"
              style={{ display: "block", margin: "16px auto 0", textAlign: "center" }}
              onClick={handleSendOTP}
              disabled={loading}
            >
              Resend OTP
            </button>
          </>
        )}
      </div>

      <div id="recaptcha-container" />
    </div>
  );
}
