import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import * as SecureStore from "expo-secure-store";
import { login as loginRequest, register as registerRequest, MemberSummary } from "../api/auth";
import { setStoredToken, setUnauthorizedHandler, TOKEN_KEY } from "../api/client";

const MEMBER_KEY = "pact_member_summary";

type AuthContextValue = {
  token: string | null;
  member: MemberSummary | null;
  isRestoring: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [member, setMember] = useState<MemberSummary | null>(null);
  const [isRestoring, setIsRestoring] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const storedToken = await SecureStore.getItemAsync(TOKEN_KEY);
        const storedMember = await SecureStore.getItemAsync(MEMBER_KEY);

        if (storedToken && storedMember) {
          setToken(storedToken);
          setMember(JSON.parse(storedMember) as MemberSummary);
        }
      } finally {
        setIsRestoring(false);
      }
    })();
  }, []);

  const persist = useCallback(async (newToken: string, newMember: MemberSummary) => {
    await setStoredToken(newToken);
    await SecureStore.setItemAsync(MEMBER_KEY, JSON.stringify(newMember));
    setToken(newToken);
    setMember(newMember);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await loginRequest(email, password);
      await persist(response.token, response.member);
    },
    [persist]
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string) => {
      const response = await registerRequest(email, password, displayName);
      await persist(response.token, response.member);
    },
    [persist]
  );

  const logout = useCallback(async () => {
    await setStoredToken(null);
    await SecureStore.deleteItemAsync(MEMBER_KEY);
    setToken(null);
    setMember(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => logout());
  }, [logout]);

  return (
    <AuthContext.Provider value={{ token, member, isRestoring, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}