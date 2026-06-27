// src/App.js
import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./hooks/useAuth";
import SplashScreen from "./screens/SplashScreen";
import LoginScreen from "./screens/LoginScreen";
import ParentDashboard from "./screens/ParentDashboard";
import StartSession from "./screens/StartSession";
import ChildFocusScreen from "./screens/ChildFocusScreen";
import PinUnlock from "./screens/PinUnlock";
import ReportsScreen from "./screens/ReportsScreen";
import RewardsScreen from "./screens/RewardsScreen";
import AchievementsScreen from "./screens/AchievementsScreen";
import SettingsScreen from "./screens/SettingsScreen";
import AddChildScreen from "./screens/AddChildScreen";
import "./App.css";

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <div className="app-loading"><div className="spinner" /></div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

const AppRoutes = () => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="app-shell">
        <div className="app-loading"><div className="spinner" /></div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<SplashScreen />} />
        <Route path="/login" element={user ? <Navigate to="/home" replace /> : <LoginScreen />} />
        <Route path="/home" element={<ProtectedRoute><ParentDashboard /></ProtectedRoute>} />
        <Route path="/session/start" element={<ProtectedRoute><StartSession /></ProtectedRoute>} />
        <Route path="/session/focus/:childId" element={<ProtectedRoute><ChildFocusScreen /></ProtectedRoute>} />
        <Route path="/session/unlock/:childId" element={<ProtectedRoute><PinUnlock /></ProtectedRoute>} />
        <Route path="/reports" element={<ProtectedRoute><ReportsScreen /></ProtectedRoute>} />
        <Route path="/rewards" element={<ProtectedRoute><RewardsScreen /></ProtectedRoute>} />
        <Route path="/achievements" element={<ProtectedRoute><AchievementsScreen /></ProtectedRoute>} />
        <Route path="/settings" element={<ProtectedRoute><SettingsScreen /></ProtectedRoute>} />
        <Route path="/add-child" element={<ProtectedRoute><AddChildScreen /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
};

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
