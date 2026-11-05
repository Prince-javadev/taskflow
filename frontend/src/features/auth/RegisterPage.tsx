import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { errorMessage, fieldErrors } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { Button } from '../../components/Button';
import { FormField } from '../../components/FormField';

const schema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(100, 'At most 100 characters'),
    email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
    password: z.string().min(8, 'Password must be 8-72 characters').max(72, 'Password must be 8-72 characters'),
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });
type RegisterForm = z.infer<typeof schema>;

export function RegisterPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<RegisterForm>({ resolver: zodResolver(schema) });

  if (isAuthenticated) return <Navigate to="/projects" replace />;

  const onSubmit = handleSubmit(async ({ name, email, password }) => {
    setServerError(null);
    try {
      await authApi.register(name, email, password);
      await login(email, password);
      navigate('/projects', { replace: true });
    } catch (error) {
      const fields = fieldErrors(error);
      Object.entries(fields).forEach(([field, message]) => setError(field as keyof RegisterForm, { message }));
      if (Object.keys(fields).length === 0) setServerError(errorMessage(error));
    }
  });

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <h1>Create your account</h1>
        {serverError && (
          <p className="notice notice-error" role="alert">
            {serverError}
          </p>
        )}
        <FormField label="Name" htmlFor="name" error={formState.errors.name?.message}>
          <input id="name" autoComplete="name" {...register('name')} />
        </FormField>
        <FormField label="Email" htmlFor="email" error={formState.errors.email?.message}>
          <input id="email" type="email" autoComplete="email" {...register('email')} />
        </FormField>
        <FormField label="Password" htmlFor="password" error={formState.errors.password?.message}>
          <input id="password" type="password" autoComplete="new-password" {...register('password')} />
        </FormField>
        <FormField label="Confirm password" htmlFor="confirmPassword" error={formState.errors.confirmPassword?.message}>
          <input id="confirmPassword" type="password" autoComplete="new-password" {...register('confirmPassword')} />
        </FormField>
        <Button type="submit" loading={formState.isSubmitting} className="btn-block">
          Register
        </Button>
        <p className="muted center">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
