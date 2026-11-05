import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Task } from '../api/types';
import { TaskCard } from './TaskCard';

const baseTask: Task = {
  id: 1,
  projectId: 1,
  title: 'Fix broken footer links',
  description: null,
  status: 'TODO',
  priority: 'HIGH',
  dueDate: '2026-01-10',
  overdue: false,
  assignee: { id: 2, name: 'Asha Teammate', email: 'asha@taskflow.dev' },
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('TaskCard', () => {
  it('shows title, priority and assignee', () => {
    render(<TaskCard task={baseTask} onStatusChange={jest.fn()} onEdit={jest.fn()} />);

    expect(screen.getByText('Fix broken footer links')).toBeInTheDocument();
    expect(screen.getByText('HIGH')).toBeInTheDocument();
    expect(screen.getByText('Asha Teammate')).toBeInTheDocument();
    expect(screen.queryByText('Overdue')).not.toBeInTheDocument();
  });

  it('highlights an overdue task', () => {
    render(<TaskCard task={{ ...baseTask, overdue: true }} onStatusChange={jest.fn()} onEdit={jest.fn()} />);

    expect(screen.getByText('Overdue')).toBeInTheDocument();
    expect(screen.getByRole('article')).toHaveClass('task-overdue');
  });

  it('reports a status change from the dropdown', async () => {
    const onStatusChange = jest.fn();
    render(<TaskCard task={baseTask} onStatusChange={onStatusChange} onEdit={jest.fn()} />);

    await userEvent.selectOptions(screen.getByLabelText(/status for/i), 'IN_PROGRESS');

    expect(onStatusChange).toHaveBeenCalledWith(baseTask, 'IN_PROGRESS');
  });

  it('opens the editor when the title is clicked', async () => {
    const onEdit = jest.fn();
    render(<TaskCard task={baseTask} onStatusChange={jest.fn()} onEdit={onEdit} />);

    await userEvent.click(screen.getByRole('button', { name: 'Fix broken footer links' }));

    expect(onEdit).toHaveBeenCalledWith(baseTask);
  });
});
