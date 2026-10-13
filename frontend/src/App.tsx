import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { StatsPage } from './pages/StatsPage';
import { TaskFormPage } from './pages/TaskFormPage';
import { TasksPage } from './pages/TasksPage';

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route path="/tasks" element={<TasksPage />} />
                <Route path="/tasks/new" element={<TaskFormPage />} />
                <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
                <Route path="/stats" element={<StatsPage />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/tasks" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
