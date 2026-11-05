import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { errorMessage } from '../../api/client';
import { Button } from '../../components/Button';
import { FormField } from '../../components/FormField';

const schema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});
type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const { login, isAuthenticated, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const registered = (location.state as { registered?: boolean } | null)?.registered;
  const from = (location.state as { from?: string } | null)?.from ?? '/projects';
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState } = useForm<LoginForm>({ resolver: zodResolver(schema) });

  if (isAuthenticated) return <Navigate to={from} replace />;

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setServerError(null);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (error) {
      setServerError(errorMessage(error));
    }
  });

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <h1>Log in to TaskFlow</h1>
        {sessionExpired && <p className="notice">Your session expired. Please log in again.</p>}
        {registered && <p className="notice notice-success">Account created. You can log in now.</p>}
        {serverError && (
          <p className="notice notice-error" role="alert">
            {serverError}
          </p>
        )}
        <FormField label="Email" htmlFor="email" error={formState.errors.email?.message}>
          <input id="email" type="email" autoComplete="email" {...register('email')} />
        </FormField>
        <FormField label="Password" htmlFor="password" error={formState.errors.password?.message}>
          <input id="password" type="password" autoComplete="current-password" {...register('password')} />
        </FormField>
        <Button type="submit" loading={formState.isSubmitting} className="btn-block">
          Log in
        </Button>
        <p className="muted center">
          No account? <Link to="/register">Register</Link>
        </p>
        <p className="muted center small">Demo: demo@taskflow.dev / Password123!</p>
      </form>
    </div>
  );
}
