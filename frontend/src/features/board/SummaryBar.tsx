import type { TaskSummary } from '../../api/types';
import { STATUSES, STATUS_LABELS } from '../../api/types';

export function SummaryBar({ summary }: { summary: TaskSummary }) {
  return (
    <div className="summary-bar" aria-label="Task summary">
      {STATUSES.map((s) => (
        <div key={s} className="summary-item">
          <span className="summary-value">{summary.counts[s] ?? 0}</span>
          <span className="summary-label">{STATUS_LABELS[s]}</span>
        </div>
      ))}
      <div className={`summary-item ${summary.overdue > 0 ? 'summary-overdue' : ''}`.trim()}>
        <span className="summary-value">{summary.overdue}</span>
        <span className="summary-label">Overdue</span>
      </div>
    </div>
  );
}
