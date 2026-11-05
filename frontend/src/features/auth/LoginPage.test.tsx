import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from './LoginPage';

const login = jest.fn();

jest.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ login, isAuthenticated: false, sessionExpired: false }),
}));

function renderPage() {
  return render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => login.mockReset());

  it('shows field errors and does not call the API when the form is empty', async () => {
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('submits valid credentials', async () => {
    login.mockResolvedValue(undefined);
    renderPage();

    await userEvent.type(screen.getByLabelText('Email'), 'demo@taskflow.dev');
    await userEvent.type(screen.getByLabelText('Password'), 'Password123!');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(login).toHaveBeenCalledWith('demo@taskflow.dev', 'Password123!');
  });

  it('shows the server error when login fails', async () => {
    login.mockRejectedValue(new Error('Invalid email or password'));
    renderPage();

    await userEvent.type(screen.getByLabelText('Email'), 'demo@taskflow.dev');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });
});
