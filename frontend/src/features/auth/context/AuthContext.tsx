import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import type { AuthResponse, Role } from '@/shared/types';
import { authApi } from '@/shared/api';
import {
  AUTH_TOKEN_REFRESHED_EVENT,
  clearAuthSession,
  refreshAccessToken,
} from '@/shared/api/client';

interface AuthContextType {
  user: AuthResponse | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

function readStoredUser(): AuthResponse | null {
  const stored = localStorage.getItem('user');
  return stored ? JSON.parse(stored) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => readStoredUser());
  const [accessToken, setAccessToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [isBootstrapping, setIsBootstrapping] = useState(() => {
    return !!localStorage.getItem('user') && !!localStorage.getItem('refreshToken');
  });
  const loginInProgressRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    async function bootstrapSession() {
      const storedUser = readStoredUser();
      const refreshToken = localStorage.getItem('refreshToken');

      if (!storedUser || !refreshToken) {
        if (!cancelled) setIsBootstrapping(false);
        return;
      }

      // Always validate/refresh on bootstrap — do not trust a stored access token alone.
      const token = await refreshAccessToken();
      if (cancelled || loginInProgressRef.current) return;

      if (!token) {
        // Concurrent login may have replaced tokens while refresh was in flight.
        const recoveredRefresh = localStorage.getItem('refreshToken');
        const recoveredToken = localStorage.getItem('accessToken');
        if (recoveredToken && recoveredRefresh && recoveredRefresh !== refreshToken) {
          setUser(readStoredUser());
          setAccessToken(recoveredToken);
        } else {
          clearAuthSession();
          setUser(null);
          setAccessToken(null);
        }
      } else {
        setUser(readStoredUser());
        setAccessToken(token);
      }
      setIsBootstrapping(false);
    }

    bootstrapSession();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onTokenRefreshed = (event: Event) => {
      const detail = (event as CustomEvent<{ accessToken: string }>).detail;
      setAccessToken(detail.accessToken);
      setUser(readStoredUser());
    };

    window.addEventListener(AUTH_TOKEN_REFRESHED_EVENT, onTokenRefreshed);
    return () => window.removeEventListener(AUTH_TOKEN_REFRESHED_EVENT, onTokenRefreshed);
  }, []);

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const login = async (email: string, password: string) => {
    loginInProgressRef.current = true;
    try {
      const data = await authApi.login(email, password);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      setAccessToken(data.accessToken);
      setUser(data);
      setIsBootstrapping(false);
    } finally {
      loginInProgressRef.current = false;
    }
  };

  const logout = () => {
    clearAuthSession();
    setAccessToken(null);
    setUser(null);
    authApi.logout().catch(() => {});
  };

  const hasRole = (...roles: Role[]) => (user ? roles.includes(user.role) : false);

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        hasRole,
        isAuthenticated: !isBootstrapping && !!user && !!accessToken,
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
