import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Layout() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <header className="navbar">
        <span className="brand">Interview Prep</span>
        <nav>
          <NavLink to="/tasks">Tasks</NavLink>
          <NavLink to="/tasks/new">New Task</NavLink>
          <NavLink to="/stats">Stats</NavLink>
        </nav>
        <div className="user-info">
          <span>{username}</span>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
