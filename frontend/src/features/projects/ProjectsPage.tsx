import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { Button } from '../../components/Button';
import { FormField } from '../../components/FormField';
import { Modal } from '../../components/Modal';
import { EmptyState, ErrorMessage, Spinner } from '../../components/States';
import { useCreateProject, useProjects } from '../../hooks/useProjects';

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'At most 100 characters'),
  description: z.string().max(1000, 'At most 1000 characters').optional(),
});
type ProjectForm = z.infer<typeof schema>;

export function ProjectsPage() {
  const projects = useProjects();
  const [creating, setCreating] = useState(false);

  return (
    <section>
      <div className="page-header">
        <h1>Your projects</h1>
        <Button onClick={() => setCreating(true)}>+ New project</Button>
      </div>

      {projects.isPending && <Spinner label="Loading projects…" />}
      {projects.isError && <ErrorMessage message={errorMessage(projects.error)} onRetry={() => projects.refetch()} />}
      {projects.isSuccess && projects.data.length === 0 && (
        <EmptyState title="No projects yet">
          <p className="muted">Create your first project to start tracking tasks.</p>
        </EmptyState>
      )}
      {projects.isSuccess && projects.data.length > 0 && (
        <ul className="project-grid">
          {projects.data.map((p) => (
            <li key={p.id}>
              <Link to={`/projects/${p.id}`} className="card project-card">
                <h2>{p.name}</h2>
                <p className="muted">{p.description || 'No description'}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {creating && <CreateProjectModal onClose={() => setCreating(false)} />}
    </section>
  );
}

function CreateProjectModal({ onClose }: { onClose: () => void }) {
  const createProject = useCreateProject();
  const { register, handleSubmit, formState } = useForm<ProjectForm>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit((values) =>
    createProject.mutate(
      { name: values.name, description: values.description?.trim() || null },
      { onSuccess: onClose },
    ),
  );

  return (
    <Modal title="New project" onClose={onClose}>
      <form onSubmit={onSubmit} noValidate>
        {createProject.isError && <p className="notice notice-error">{errorMessage(createProject.error)}</p>}
        <FormField label="Name" htmlFor="project-name" error={formState.errors.name?.message}>
          <input id="project-name" autoFocus {...register('name')} />
        </FormField>
        <FormField label="Description" htmlFor="project-description" error={formState.errors.description?.message}>
          <textarea id="project-description" rows={3} {...register('description')} />
        </FormField>
        <div className="modal-actions">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={createProject.isPending}>
            Create
          </Button>
        </div>
      </form>
    </Modal>
  );
}
