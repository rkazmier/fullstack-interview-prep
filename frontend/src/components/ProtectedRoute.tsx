import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/** Route guard: redirects unauthenticated users to /login. */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}
