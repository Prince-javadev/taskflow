import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import type { Task, TaskQuery, TaskStatus } from '../../api/types';
import { STATUSES, STATUS_LABELS } from '../../api/types';
import { Button } from '../../components/Button';
import { EmptyState, ErrorMessage, Spinner } from '../../components/States';
import { TaskCard } from '../../components/TaskCard';
import { useProject, useProjectSummary } from '../../hooks/useProjects';
import { useDeleteTask, useSaveTask, useTasks, useUpdateTaskStatus, useUsers } from '../../hooks/useTasks';
import { SummaryBar } from './SummaryBar';
import { TaskFilters } from './TaskFilters';
import { TaskFormModal } from './TaskFormModal';

type Editing = { mode: 'create' } | { mode: 'edit'; task: Task } | null;

export function BoardPage() {
  const projectId = Number(useParams().projectId);
  const [query, setQuery] = useState<TaskQuery>({ sort: 'dueDate', direction: 'asc' });
  const [editing, setEditing] = useState<Editing>(null);

  const project = useProject(projectId);
  const summary = useProjectSummary(projectId);
  const tasks = useTasks(projectId, query);
  const users = useUsers();
  const saveTask = useSaveTask(projectId);
  const updateStatus = useUpdateTaskStatus(projectId);
  const deleteTask = useDeleteTask(projectId);

  if (project.isPending) return <Spinner label="Loading project…" />;
  if (project.isError) {
    return (
      <>
        <Link to="/projects">← Back to projects</Link>
        <ErrorMessage message={errorMessage(project.error)} onRetry={() => project.refetch()} />
      </>
    );
  }

  const byStatus = (status: TaskStatus) => tasks.data?.content.filter((t) => t.status === status) ?? [];
  const onStatusChange = (task: Task, status: TaskStatus) => updateStatus.mutate({ id: task.id, status });

  return (
    <section>
      <Link to="/projects" className="muted small">
        ← All projects
      </Link>
      <div className="page-header">
        <div>
          <h1>{project.data.name}</h1>
          {project.data.description && <p className="muted">{project.data.description}</p>}
        </div>
        <Button onClick={() => setEditing({ mode: 'create' })}>+ New task</Button>
      </div>

      {summary.isPending && <Spinner label="Loading summary…" />}
      {summary.isError && <ErrorMessage message={errorMessage(summary.error)} onRetry={() => summary.refetch()} />}
      {summary.isSuccess && <SummaryBar summary={summary.data} />}

      <TaskFilters query={query} users={users.data ?? []} onChange={setQuery} />

      {updateStatus.isError && <p className="notice notice-error">{errorMessage(updateStatus.error)}</p>}

      {tasks.isPending && <Spinner label="Loading tasks…" />}
      {tasks.isError && <ErrorMessage message={errorMessage(tasks.error)} onRetry={() => tasks.refetch()} />}
      {tasks.isSuccess && tasks.data.totalElements === 0 && (
        <EmptyState title={query.priority || query.assigneeId ? 'No tasks match these filters' : 'No tasks yet'}>
          <p className="muted">Create a task to get started.</p>
        </EmptyState>
      )}
      {tasks.isSuccess && tasks.data.totalElements > 0 && (
        <div className="board">
          {STATUSES.map((status) => (
            <div key={status} className="column" aria-label={STATUS_LABELS[status]}>
              <h2 className="column-title">
                {STATUS_LABELS[status]} <span className="muted">{byStatus(status).length}</span>
              </h2>
              {byStatus(status).length === 0 && <p className="column-empty">Nothing here</p>}
              {byStatus(status).map((task) => (
                <TaskCard
                  key={task.id}
                  task={task}
                  onStatusChange={onStatusChange}
                  onEdit={(t) => setEditing({ mode: 'edit', task: t })}
                  updating={updateStatus.isPending && updateStatus.variables?.id === task.id}
                />
              ))}
            </div>
          ))}
        </div>
      )}

      {editing && (
        <TaskFormModal
          task={editing.mode === 'edit' ? editing.task : undefined}
          users={users.data ?? []}
          saving={saveTask.isPending}
          deleting={deleteTask.isPending}
          onSubmit={(input) =>
            saveTask.mutateAsync({ id: editing.mode === 'edit' ? editing.task.id : undefined, input })
          }
          onDelete={
            editing.mode === 'edit'
              ? () => deleteTask.mutate(editing.task.id, { onSuccess: () => setEditing(null) })
              : undefined
          }
          onClose={() => setEditing(null)}
        />
      )}
    </section>
  );
}
