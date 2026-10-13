import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { HttpError } from '../api/client';

/**
 * Controlled form (interview topic: controlled vs uncontrolled inputs).
 * Toggles between login and registration modes.
 */
export function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('demo');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('demo123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await register(username, email, password);
      }
      navigate('/tasks');
    } catch (err) {
      if (err instanceof HttpError && err.body?.fieldErrors) {
        const fields = Object.entries(err.body.fieldErrors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ');
        setError(fields || err.message);
      } else {
        setError(err instanceof Error ? err.message : 'Unexpected error');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card" onSubmit={handleSubmit}>
        <h1>{mode === 'login' ? 'Sign in' : 'Create account'}</h1>
        <p className="hint">Demo user: demo / demo123</p>

        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            minLength={3}
          />
        </label>

        {mode === 'register' && (
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
        )}

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Register'}
        </button>

        <button
          type="button"
          className="link"
          onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
        >
          {mode === 'login' ? 'No account? Register' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}
