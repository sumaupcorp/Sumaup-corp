"use client";

import { createContext, useContext, useEffect, useState } from "react";
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
  type User,
} from "firebase/auth";
import { auth, googleProvider } from "@/lib/firebase";

interface AuthState {
  user: User | null;
  emailVerified: boolean;
  loading: boolean;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  registerWithEmail: (name: string, email: string, password: string) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
  resendVerification: () => Promise<void>;
  reloadUser: () => Promise<boolean>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [emailVerified, setEmailVerified] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u);
      setEmailVerified(u?.emailVerified ?? false);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  const value: AuthState = {
    user,
    emailVerified,
    loading,
    loginWithEmail: async (email, password) => {
      await signInWithEmailAndPassword(auth, email, password);
    },
    registerWithEmail: async (name, email, password) => {
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      if (name) await updateProfile(cred.user, { displayName: name });
      await sendEmailVerification(cred.user); // envia el correo de verificacion
    },
    loginWithGoogle: async () => {
      await signInWithPopup(auth, googleProvider);
    },
    resetPassword: async (email) => {
      await sendPasswordResetEmail(auth, email);
    },
    resendVerification: async () => {
      if (auth.currentUser) await sendEmailVerification(auth.currentUser);
    },
    reloadUser: async () => {
      await auth.currentUser?.reload();
      const u = auth.currentUser;
      setUser(u);
      setEmailVerified(u?.emailVerified ?? false);
      return u?.emailVerified ?? false;
    },
    logout: async () => {
      await signOut(auth);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de AuthProvider");
  return ctx;
}
