import { useCallback, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Page, TaskResponse, TaskStatus } from '../api/types';
import { useApi } from '../hooks/useApi';
import { useDebounce } from '../hooks/useDebounce';

const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

/**
 * Interview topics demonstrated here:
 * - server-side pagination + filtering + debounced search
 * - derived state kept in URL-friendly local state
 * - lists with stable keys
 * - optimistic-ish delete with refetch
 */
export function TasksPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<TaskStatus | ''>('');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search);

  const fetcher = useCallback(() => {
    const params = new URLSearchParams({ page: String(page), size: '5' });
    if (status) params.set('status', status);
    if (debouncedSearch) params.set('q', debouncedSearch);
    return api.get<Page<TaskResponse>>(`/api/tasks?${params}`);
  }, [page, status, debouncedSearch]);

  const { data, loading, error, refetch } = useApi(fetcher);

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this task?')) return;
    await api.delete(`/api/tasks/${id}`);
    refetch();
  };

  return (
    <div>
      <h1>Tasks</h1>

      <div className="toolbar">
        <input
          placeholder="Search tasks…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as TaskStatus | '');
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}

      {data && (
        <>
          <ul className="task-list">
            {data.content.map((task) => (
              <li key={task.id} className="task-card">
                <div className="task-main">
                  <Link to={`/tasks/${task.id}/edit`} className="task-title">
                    {task.title}
                  </Link>
                  <span className={`badge status-${task.status.toLowerCase()}`}>
                    {task.status}
                  </span>
                  <span className={`badge priority-${task.priority.toLowerCase()}`}>
                    {task.priority}
                  </span>
                </div>
                {task.description && <p className="task-desc">{task.description}</p>}
                <div className="task-meta">
                  {task.dueDate && <span>Due: {task.dueDate}</span>}
                  {task.tags.map((tag) => (
                    <span key={tag} className="tag">
                      #{tag}
                    </span>
                  ))}
                </div>
                <div className="task-actions">
                  <button onClick={() => navigate(`/tasks/${task.id}/edit`)}>Edit</button>
                  <button className="danger" onClick={() => handleDelete(task.id)}>
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>

          {data.content.length === 0 && <p>No tasks found.</p>}

          <div className="pagination">
            <button disabled={data.first} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {data.number + 1} / {Math.max(data.totalPages, 1)}
            </span>
            <button disabled={data.last} onClick={() => setPage((p) => p + 1)}>
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}
