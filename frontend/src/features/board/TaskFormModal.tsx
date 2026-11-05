import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { errorMessage, fieldErrors } from '../../api/client';
import type { Task, TaskInput, User } from '../../api/types';
import { PRIORITIES, STATUSES, STATUS_LABELS } from '../../api/types';
import { Button } from '../../components/Button';
import { FormField } from '../../components/FormField';
import { Modal } from '../../components/Modal';
import { todayIso } from '../../utils/date';

function buildSchema(isNew: boolean) {
  return z.object({
    title: z.string().trim().min(1, 'Title is required').max(120, 'Title must be at most 120 characters'),
    description: z.string().max(2000, 'At most 2000 characters'),
    priority: z.enum(['LOW', 'MEDIUM', 'HIGH']),
    status: z.enum(['TODO', 'IN_PROGRESS', 'DONE']),
    dueDate: z
      .string()
      .refine((v) => !isNew || !v || v >= todayIso(), 'Due date cannot be in the past'),
    assigneeId: z.string(),
  });
}
type TaskForm = z.infer<ReturnType<typeof buildSchema>>;

interface TaskFormModalProps {
  task?: Task;
  users: User[];
  saving: boolean;
  deleting?: boolean;
  onSubmit: (input: TaskInput) => Promise<unknown>;
  onDelete?: () => void;
  onClose: () => void;
}

export function TaskFormModal({ task, users, saving, deleting, onSubmit, onDelete, onClose }: TaskFormModalProps) {
  const isNew = !task;
  const { register, handleSubmit, formState, setError } = useForm<TaskForm>({
    resolver: zodResolver(buildSchema(isNew)),
    defaultValues: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      priority: task?.priority ?? 'MEDIUM',
      status: task?.status ?? 'TODO',
      dueDate: task?.dueDate ?? '',
      assigneeId: task?.assignee ? String(task.assignee.id) : '',
    },
  });

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit({
        title: values.title.trim(),
        description: values.description.trim() || null,
        priority: values.priority,
        status: values.status,
        dueDate: values.dueDate || null,
        assigneeId: values.assigneeId ? Number(values.assigneeId) : null,
      });
      onClose();
    } catch (error) {
      const fields = fieldErrors(error);
      Object.entries(fields).forEach(([field, message]) => setError(field as keyof TaskForm, { message }));
      if (Object.keys(fields).length === 0) setError('root', { message: errorMessage(error) });
    }
  });

  const errors = formState.errors;

  return (
    <Modal title={isNew ? 'New task' : 'Edit task'} onClose={onClose}>
      <form onSubmit={submit} noValidate>
        {errors.root && <p className="notice notice-error">{errors.root.message}</p>}
        <FormField label="Title" htmlFor="task-title" error={errors.title?.message}>
          <input id="task-title" autoFocus {...register('title')} />
        </FormField>
        <FormField label="Description" htmlFor="task-description" error={errors.description?.message}>
          <textarea id="task-description" rows={3} {...register('description')} />
        </FormField>
        <div className="field-row">
          <FormField label="Priority" htmlFor="task-priority" error={errors.priority?.message}>
            <select id="task-priority" {...register('priority')}>
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Status" htmlFor="task-status" error={errors.status?.message}>
            <select id="task-status" {...register('status')}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </FormField>
        </div>
        <div className="field-row">
          <FormField label="Due date" htmlFor="task-due" error={errors.dueDate?.message}>
            <input id="task-due" type="date" min={isNew ? todayIso() : undefined} {...register('dueDate')} />
          </FormField>
          <FormField label="Assignee" htmlFor="task-assignee" error={errors.assigneeId?.message}>
            <select id="task-assignee" {...register('assigneeId')}>
              <option value="">Unassigned</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </FormField>
        </div>
        <div className="modal-actions">
          {onDelete && (
            <Button variant="danger" onClick={onDelete} loading={deleting} className="mr-auto">
              Delete
            </Button>
          )}
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {isNew ? 'Create task' : 'Save changes'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
