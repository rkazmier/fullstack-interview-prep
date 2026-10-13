import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, HttpError } from '../api/client';
import type { TaskPriority, TaskRequest, TaskResponse, TaskStatus } from '../api/types';

const EMPTY: TaskRequest = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: null,
  tags: [],
};

/**
 * One component used for both create and edit (presence of :id decides).
 * Shows client-side validation plus mapping server-side Bean Validation
 * errors (400 + fieldErrors) back onto the form.
 */
export function TaskFormPage() {
  const { id } = useParams();
  const isEdit = id !== undefined;
  const navigate = useNavigate();

  const [form, setForm] = useState<TaskRequest>(EMPTY);
  const [tagsInput, setTagsInput] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(isEdit);

  useEffect(() => {
    if (!isEdit) return;
    api
      .get<TaskResponse>(`/api/tasks/${id}`)
      .then((task) => {
        setForm({
          title: task.title,
          description: task.description ?? '',
          status: task.status,
          priority: task.priority,
          dueDate: task.dueDate,
          tags: task.tags,
        });
        setTagsInput(task.tags.join(', '));
      })
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id, isEdit]);

  const set = <K extends keyof TaskRequest>(key: K, value: TaskRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    const payload: TaskRequest = {
      ...form,
      description: form.description || null,
      dueDate: form.dueDate || null,
      tags: tagsInput
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
    };

    try {
      if (isEdit) {
        await api.put(`/api/tasks/${id}`, payload);
      } else {
        await api.post('/api/tasks', payload);
      }
      navigate('/tasks');
    } catch (err) {
      if (err instanceof HttpError && err.body?.fieldErrors) {
        setFieldErrors(err.body.fieldErrors);
        setError(err.body.message);
      } else {
        setError(err instanceof Error ? err.message : 'Unexpected error');
      }
    }
  };

  if (loading) return <p>Loading…</p>;

  return (
    <div>
      <h1>{isEdit ? 'Edit task' : 'New task'}</h1>
      <form className="card" onSubmit={handleSubmit}>
        <label>
          Title *
          <input
            value={form.title}
            onChange={(e) => set('title', e.target.value)}
            maxLength={100}
            required
          />
          {fieldErrors.title && <span className="error">{fieldErrors.title}</span>}
        </label>

        <label>
          Description
          <textarea
            value={form.description ?? ''}
            onChange={(e) => set('description', e.target.value)}
            rows={3}
            maxLength={1000}
          />
          {fieldErrors.description && <span className="error">{fieldErrors.description}</span>}
        </label>

        <div className="row">
          <label>
            Status
            <select
              value={form.status}
              onChange={(e) => set('status', e.target.value as TaskStatus)}
            >
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="DONE">DONE</option>
            </select>
          </label>

          <label>
            Priority
            <select
              value={form.priority}
              onChange={(e) => set('priority', e.target.value as TaskPriority)}
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
            </select>
          </label>

          <label>
            Due date
            <input
              type="date"
              value={form.dueDate ?? ''}
              onChange={(e) => set('dueDate', e.target.value || null)}
            />
            {fieldErrors.dueDate && <span className="error">{fieldErrors.dueDate}</span>}
          </label>
        </div>

        <label>
          Tags (comma separated)
          <input
            value={tagsInput}
            onChange={(e) => setTagsInput(e.target.value)}
            placeholder="backend, urgent"
          />
        </label>

        {error && <p className="error">{error}</p>}

        <div className="form-actions">
          <button type="submit">{isEdit ? 'Save changes' : 'Create task'}</button>
          <button type="button" className="secondary" onClick={() => navigate('/tasks')}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
