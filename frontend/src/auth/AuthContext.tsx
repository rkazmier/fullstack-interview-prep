/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, getToken, setToken } from '../api/client';
import type { AuthResponse } from '../api/types';

interface AuthState {
  username: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * Interview topics: Context API vs prop drilling, when to reach for
 * Redux/Zustand instead, storing JWT (localStorage vs httpOnly cookie trade-offs).
 *
 * ODPOWIEDŹ (Context vs prop drilling vs Redux):
 * - Prop drilling = przekazywanie propsów przez wiele poziomów komponentów,
 *   które ich nie używają; Context rozwiązuje to dla danych "globalnych"
 *   (auth, motyw, język).
 * - Context jest wbudowany i prosty, ale KAŻDA zmiana value powoduje
 *   re-render wszystkich konsumentów — dlatego value jest memoizowane.
 * - Redux/Zustand ma sens przy dużym, często zmienianym stanie, selektorach
 *   (re-render tylko zainteresowanych komponentów), devtools i middleware.
 *   Do samego auth Context w zupełności wystarcza.
 *
 * ODPOWIEDŹ (localStorage vs httpOnly cookie dla JWT):
 * - localStorage: prosty, działa z nagłówkiem Authorization, odporny na CSRF
 *   (token nie jest wysyłany automatycznie), ALE podatny na XSS — skrypt
 *   wstrzyknięty na stronę może odczytać token.
 * - httpOnly cookie: JS nie ma dostępu (odporny na kradzież przez XSS), ALE
 *   wysyłany automatycznie, więc wymaga ochrony CSRF (SameSite=Strict/Lax,
 *   token CSRF) i komplikuje CORS.
 * - Produkcyjnie najczęściej: krótki access token w pamięci (nie localStorage)
 *   + refresh token w httpOnly cookie. Tu dla prostoty: localStorage.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(() =>
    getToken() ? localStorage.getItem('username') : null,
  );

  const handleAuth = useCallback((auth: AuthResponse) => {
    setToken(auth.token);
    localStorage.setItem('username', auth.username);
    setUsername(auth.username);
  }, []);

  const login = useCallback(
    async (user: string, password: string) => {
      const auth = await api.post<AuthResponse>('/api/auth/login', {
        username: user,
        password,
      });
      handleAuth(auth);
    },
    [handleAuth],
  );

  const register = useCallback(
    async (user: string, email: string, password: string) => {
      const auth = await api.post<AuthResponse>('/api/auth/register', {
        username: user,
        email,
        password,
      });
      handleAuth(auth);
    },
    [handleAuth],
  );

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem('username');
    setUsername(null);
  }, []);

  // useMemo keeps the context value referentially stable between renders
  const value = useMemo<AuthState>(
    () => ({ username, isAuthenticated: username !== null, login, register, logout }),
    [username, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
