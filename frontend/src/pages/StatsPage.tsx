import { useCallback, useMemo } from 'react';
import { api } from '../api/client';
import type { TaskStats } from '../api/types';
import { useApi } from '../hooks/useApi';

const LABELS: Record<string, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};

/**
 * Interview topic: useMemo for derived data - the totals are only
 * recomputed when `data` changes, not on every render.
 */
export function StatsPage() {
  const fetcher = useCallback(() => api.get<TaskStats[]>('/api/tasks/stats'), []);
  const { data, loading, error } = useApi(fetcher);

  const total = useMemo(
    () => (data ?? []).reduce((sum, s) => sum + s.count, 0),
    [data],
  );

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <div>
      <h1>Statistics</h1>
      <div className="stats-grid">
        {(data ?? []).map((stat) => (
          <div key={stat.status} className={`stat-card status-${stat.status.toLowerCase()}`}>
            <span className="stat-count">{stat.count}</span>
            <span className="stat-label">{LABELS[stat.status] ?? stat.status}</span>
            <span className="stat-percent">
              {total > 0 ? Math.round((stat.count / total) * 100) : 0}%
            </span>
          </div>
        ))}
        <div className="stat-card total">
          <span className="stat-count">{total}</span>
          <span className="stat-label">Total</span>
        </div>
      </div>
    </div>
  );
}
