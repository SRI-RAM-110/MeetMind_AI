import React, { useState } from 'react';
import './ActionItemsTable.css';

export default function ActionItemsTable({ actionItems, meetingId, onDownloadCsv }) {
  const [isDownloading, setIsDownloading] = useState(false);
  const [ownerFilter, setOwnerFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const handleDownload = async () => {
    if (!meetingId || isDownloading) return;
    try {
      setIsDownloading(true);
      await onDownloadCsv(meetingId);
    } catch (err) {
      alert(err.message || 'Error downloading CSV');
    } finally {
      setIsDownloading(false);
    }
  };

  const items = actionItems || [];
  const hasItems = items.length > 0;

  // Extract unique owners for filter
  const uniqueOwners = Array.from(new Set(items.map((i) => i.ownerName || i.owner || 'Unassigned'))).sort();

  // Filter items
  const filteredItems = items.filter((item) => {
    const owner = item.ownerName || item.owner || 'Unassigned';
    const priority = item.priority || 'Medium';
    const status = item.status || 'Pending';
    const taskText = `${item.title || ''} ${item.description || ''} ${item.task || ''} ${item.evidence || ''}`.toLowerCase();

    if (ownerFilter !== 'ALL' && owner !== ownerFilter) return false;
    if (priorityFilter !== 'ALL' && priority.toLowerCase() !== priorityFilter.toLowerCase()) return false;
    if (statusFilter !== 'ALL' && status.toLowerCase() !== statusFilter.toLowerCase()) return false;
    if (searchQuery.trim() && !taskText.includes(searchQuery.toLowerCase())) return false;

    return true;
  });

  return (
    <div className="card action-items-card">
      <div className="action-items-header">
        <div className="action-items-title-wrapper">
          <span className="section-tag">Deliverables & Responsibilities</span>
          <h2 className="action-items-title">
            Action Items & Task Matrix ({filteredItems.length} of {items.length})
          </h2>
        </div>
        {hasItems && (
          <button
            type="button"
            className="btn btn-primary btn-sm btn-download-csv"
            onClick={handleDownload}
            disabled={isDownloading}
            title="Download structured action items as CSV"
          >
            {isDownloading ? '⏳ Exporting...' : '📥 Export action_items.csv'}
          </button>
        )}
      </div>

      {/* Filter controls */}
      {hasItems && (
        <div className="action-filters-bar">
          <div className="filter-group">
            <label className="filter-label">Search:</label>
            <input
              type="text"
              className="filter-input search-input"
              placeholder="Search tasks, deliverables, roles..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="filter-group">
            <label className="filter-label">Assignee:</label>
            <select
              className="filter-select"
              value={ownerFilter}
              onChange={(e) => setOwnerFilter(e.target.value)}
            >
              <option value="ALL">All Assignees</option>
              {uniqueOwners.map((own) => (
                <option key={own} value={own}>
                  {own}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label className="filter-label">Priority:</label>
            <select
              className="filter-select"
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
            >
              <option value="ALL">All Priorities</option>
              <option value="High">High</option>
              <option value="Medium">Medium</option>
              <option value="Low">Low</option>
            </select>
          </div>

          <div className="filter-group">
            <label className="filter-label">Status:</label>
            <select
              className="filter-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="Pending">Pending</option>
              <option value="In Progress">In Progress</option>
              <option value="Completed">Completed</option>
            </select>
          </div>
        </div>
      )}

      {!hasItems ? (
        <div className="empty-actions">
          <p className="empty-text">No action items or assigned deliverables identified in this meeting.</p>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="empty-actions">
          <p className="empty-text">No action items match the selected filters.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="action-table">
            <thead>
              <tr>
                <th className="th-task-id">ID</th>
                <th className="th-task">Task / Deliverable</th>
                <th className="th-owner">Assignee & Role</th>
                <th className="th-deadline">Deadline</th>
                <th className="th-priority">Priority</th>
                <th className="th-status">Status</th>
                <th className="th-dependencies">Dependencies</th>
                <th className="th-evidence">Supporting Evidence</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item, idx) => {
                const owner = item.ownerName || item.owner || 'Unassigned';
                const isUnassigned = owner === 'Unassigned' || !owner;
                const deadline = item.deadline || 'Not specified';
                const isNoDeadline = deadline === 'Not specified' || !deadline;
                const taskId = item.taskId || `TASK-${String(idx + 1).padStart(2, '0')}`;
                const title = item.title || item.task;
                const role = item.ownerRole && item.ownerRole !== 'Not specified' ? item.ownerRole : null;
                const priority = (item.priority || 'Medium').toLowerCase();
                const deps = item.dependencies || [];

                return (
                  <tr key={item.id || idx} className="action-row">
                    <td className="task-id-cell">
                      <span className="task-id-badge">{taskId}</span>
                    </td>
                    <td className="task-cell">
                      <div className="task-title-bold">{title}</div>
                      {item.description && item.description !== title && (
                        <div className="task-desc-sub">{item.description}</div>
                      )}
                      {item.needsConfirmation && (
                        <span className="badge-flag-conf" title="Flagged: Owner or deadline requires human confirmation">
                          ⚠️ Needs Confirmation
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="owner-role-stack">
                        <span className={`badge ${isUnassigned ? 'badge-unassigned' : 'badge-owner'}`}>
                          {isUnassigned ? '👤 Unassigned' : `👤 ${owner}`}
                        </span>
                        {role && <span className="owner-role-badge">Role: {role}</span>}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${isNoDeadline ? 'badge-nodeadline' : 'badge-deadline'}`}>
                        {isNoDeadline ? '📅 Not specified' : `📅 ${deadline}`}
                      </span>
                    </td>
                    <td>
                      <span className={`priority-badge priority-${priority}`}>
                        {item.priority || 'Medium'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-status-${(item.status || 'Pending').toLowerCase().replace(/\s+/g, '-')}`}>
                        {item.status || 'Pending'}
                      </span>
                    </td>
                    <td className="deps-cell">
                      {deps.length > 0 ? (
                        <div className="deps-list">
                          {deps.map((d, dIdx) => (
                            <span key={dIdx} className="dep-tag">🔗 {d}</span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-muted-dash">—</span>
                      )}
                    </td>
                    <td className="evidence-cell">
                      <blockquote className="evidence-quote" title={item.sourceEvidence || item.evidence}>
                        "{item.sourceEvidence || item.evidence || 'Direct transcription excerpt'}"
                      </blockquote>
                      {item.sourceTimestamp && (
                        <div className="time-code-tag">⏱️ {item.sourceTimestamp}</div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
