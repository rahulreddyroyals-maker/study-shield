// src/hooks/useAuth.js
import { useState, useEffect, createContext, useContext } from "react";
import {
  onAuthChange,
  getParent,
  createParent,
  requestNotificationPermission,
} from "../services/firebase";

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [parent, setParent] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsub = onAuthChange(async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        let parentData = await getParent(firebaseUser.uid);
        if (!parentData) {
          await createParent(firebaseUser.uid, {
            name: firebaseUser.displayName || "",
            phone: firebaseUser.phoneNumber || "",
            email: firebaseUser.email || "",
          });
          parentData = await getParent(firebaseUser.uid);
        }
        setParent(parentData);
        // Request notification permission
        const fcmToken = await requestNotificationPermission();
        if (fcmToken) {
          const { updateParent } = await import("../services/firebase");
          await updateParent(firebaseUser.uid, { fcmToken });
        }
      } else {
        setParent(null);
      }
      setLoading(false);
    });
    return unsub;
  }, []);

  return (
    <AuthContext.Provider value={{ user, parent, loading, setParent }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
