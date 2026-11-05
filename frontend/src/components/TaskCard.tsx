import type { Task, TaskStatus } from '../api/types';
import { STATUSES, STATUS_LABELS } from '../api/types';
import { formatDate } from '../utils/date';

interface TaskCardProps {
  task: Task;
  onStatusChange: (task: Task, status: TaskStatus) => void;
  onEdit: (task: Task) => void;
  updating?: boolean;
}

export function TaskCard({ task, onStatusChange, onEdit, updating = false }: TaskCardProps) {
  return (
    <article className={`task-card ${task.overdue ? 'task-overdue' : ''}`.trim()} aria-label={task.title}>
      <div className="task-card-top">
        <span className={`badge badge-${task.priority.toLowerCase()}`}>{task.priority}</span>
        {task.overdue && <span className="badge badge-overdue">Overdue</span>}
      </div>
      <button type="button" className="task-title" onClick={() => onEdit(task)}>
        {task.title}
      </button>
      <div className="task-meta">
        <span>{formatDate(task.dueDate)}</span>
        <span>{task.assignee ? task.assignee.name : 'Unassigned'}</span>
      </div>
      <label className="sr-only" htmlFor={`status-${task.id}`}>
        Status for {task.title}
      </label>
      <select
        id={`status-${task.id}`}
        value={task.status}
        disabled={updating}
        onChange={(e) => onStatusChange(task, e.target.value as TaskStatus)}
      >
        {STATUSES.map((s) => (
          <option key={s} value={s}>
            {STATUS_LABELS[s]}
          </option>
        ))}
      </select>
    </article>
  );
}
