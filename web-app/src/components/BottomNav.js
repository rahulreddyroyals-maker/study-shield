// src/components/BottomNav.js
import React from "react";
import { useNavigate } from "react-router-dom";

const NAV_ITEMS = [
  { id: "home",         label: "Home",    icon: "🏠", path: "/home" },
  { id: "reports",      label: "Reports", icon: "📊", path: "/reports" },
  { id: "rewards",      label: "Rewards", icon: "🎁", path: "/rewards" },
  { id: "achievements", label: "Achieve", icon: "🏆", path: "/achievements" },
];

export default function BottomNav({ active }) {
  const navigate = useNavigate();
  return (
    <nav className="bottom-nav">
      {NAV_ITEMS.map((item) => (
        <button
          key={item.id}
          className={`nav-btn ${active === item.id ? "active" : ""}`}
          onClick={() => navigate(item.path)}
          aria-label={item.label}
        >
          <span className="icon">{item.icon}</span>
          <span>{item.label}</span>
        </button>
      ))}
    </nav>
  );
}
