import type { TaskPriority, TaskQuery, User } from '../../api/types';
import { PRIORITIES } from '../../api/types';

interface TaskFiltersProps {
  query: TaskQuery;
  users: User[];
  onChange: (query: TaskQuery) => void;
}

export function TaskFilters({ query, users, onChange }: TaskFiltersProps) {
  return (
    <div className="filters">
      <label>
        Priority
        <select
          value={query.priority ?? ''}
          onChange={(e) => onChange({ ...query, priority: (e.target.value || undefined) as TaskPriority | undefined })}
        >
          <option value="">All</option>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
      </label>
      <label>
        Assignee
        <select
          value={query.assigneeId ?? ''}
          onChange={(e) => onChange({ ...query, assigneeId: e.target.value ? Number(e.target.value) : undefined })}
        >
          <option value="">Anyone</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Sort
        <select
          value={`${query.sort ?? 'dueDate'}:${query.direction ?? 'asc'}`}
          onChange={(e) => {
            const [sort, direction] = e.target.value.split(':') as [TaskQuery['sort'], TaskQuery['direction']];
            onChange({ ...query, sort, direction });
          }}
        >
          <option value="dueDate:asc">Due date (soonest)</option>
          <option value="dueDate:desc">Due date (latest)</option>
          <option value="priority:desc">Priority (high first)</option>
          <option value="createdAt:desc">Newest first</option>
        </select>
      </label>
    </div>
  );
}
