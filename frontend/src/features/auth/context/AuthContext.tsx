import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import type { AuthResponse, Role } from '@/shared/types';
import { authApi } from '@/shared/api';
import {
  AUTH_TOKEN_REFRESHED_EVENT,
  clearAuthSession,
  readUserProfile,
  refreshAccessToken,
  storeUserProfile,
} from '@/shared/api/client';
import { clearConversationStorageForUser } from '@/features/ai-assistant/utils/conversationStorage';

interface AuthContextType {
  user: AuthResponse | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => readUserProfile());
  const [sessionReady, setSessionReady] = useState(false);
  const [isBootstrapping, setIsBootstrapping] = useState(() => !!readUserProfile());
  const loginInProgressRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    async function bootstrapSession() {
      const storedUser = readUserProfile();
      if (!storedUser) {
        // Still try cookie-based refresh (page reload with cookies, empty sessionStorage)
        const token = await refreshAccessToken();
        if (cancelled || loginInProgressRef.current) return;
        if (token) {
          setUser(readUserProfile());
          setSessionReady(true);
        } else {
          setUser(null);
          setSessionReady(false);
        }
        setIsBootstrapping(false);
        return;
      }

      const token = await refreshAccessToken();
      if (cancelled || loginInProgressRef.current) return;

      if (!token) {
        clearAuthSession();
        setUser(null);
        setSessionReady(false);
      } else {
        setUser(readUserProfile());
        setSessionReady(true);
      }
      setIsBootstrapping(false);
    }

    bootstrapSession();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onTokenRefreshed = () => {
      setUser(readUserProfile());
      setSessionReady(true);
    };

    window.addEventListener(AUTH_TOKEN_REFRESHED_EVENT, onTokenRefreshed);
    return () => window.removeEventListener(AUTH_TOKEN_REFRESHED_EVENT, onTokenRefreshed);
  }, []);

  const login = async (email: string, password: string) => {
    loginInProgressRef.current = true;
    try {
      const data = await authApi.login(email, password);
      storeUserProfile(data);
      setUser(readUserProfile());
      setSessionReady(true);
      setIsBootstrapping(false);
    } finally {
      loginInProgressRef.current = false;
    }
  };

  const logout = () => {
    const email = user?.email;
    authApi.logout().catch(() => {});
    if (email) {
      clearConversationStorageForUser(email);
    }
    clearAuthSession();
    setUser(null);
    setSessionReady(false);
  };

  const hasRole = (...roles: Role[]) => (user ? roles.includes(user.role) : false);

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        hasRole,
        isAuthenticated: !isBootstrapping && !!user && sessionReady,
        isBootstrapping,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
