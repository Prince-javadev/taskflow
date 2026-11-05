import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button } from './Button';

export function Layout() {
  const { user, logout } = useAuth();
  return (
    <>
      <header className="topbar">
        <Link to="/projects" className="brand">
          TaskFlow
        </Link>
        <div className="topbar-right">
          <span className="muted hide-sm">{user?.name}</span>
          <Button variant="ghost" onClick={logout}>
            Log out
          </Button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}
