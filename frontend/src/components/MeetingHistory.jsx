import React from 'react';
import './MeetingHistory.css';

export default function MeetingHistory({
  meetings,
  currentMeetingId,
  onSelectMeeting,
  onRefresh,
  isLoadingHistory,
}) {
  return (
    <div className="card history-card">
      <div className="history-header">
        <div className="history-title-wrapper">
          <span className="section-tag">Archive & Persistence</span>
          <h2 className="history-title">Saved Meetings ({meetings.length})</h2>
        </div>
        <button
          type="button"
          className="btn btn-outline btn-sm btn-refresh-history"
          onClick={onRefresh}
          disabled={isLoadingHistory}
          title="Reload history from MongoDB"
        >
          {isLoadingHistory ? '⏳ Refreshing...' : '🔄 Refresh History'}
        </button>
      </div>

      {isLoadingHistory && meetings.length === 0 ? (
        <div className="history-loading">
          <span className="spinner-blue"></span>
          <span>Loading meetings from MongoDB...</span>
        </div>
      ) : meetings.length === 0 ? (
        <div className="empty-history">
          <p className="empty-history-text">
            No saved meetings yet. Analyze your first transcript to persist records in MongoDB.
          </p>
        </div>
      ) : (
        <div className="history-list">
          {meetings.map((m) => {
            const isSelected = m.id === currentMeetingId;
            const dateStr = m.createdAt
              ? new Date(m.createdAt).toLocaleDateString('en-US', {
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })
              : 'Unknown date';

            const meetingTitle = m.meetingTitle || 'Business Meeting';
            const displayDate = m.meetingDate ? m.meetingDate : 'Date: Not available';

            return (
              <div
                key={m.id}
                className={`history-item ${isSelected ? 'history-item-active' : ''}`}
                onClick={() => onSelectMeeting(m.id)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') onSelectMeeting(m.id);
                }}
              >
                <div className="history-item-top">
                  <span className="history-item-title-text" style={{ fontWeight: 700, color: 'var(--color-heading)' }}>
                    {meetingTitle}
                  </span>
                  <span className="history-item-badge">
                    {m.actionItemCount} {m.actionItemCount === 1 ? 'Action' : 'Actions'}
                  </span>
                </div>

                <div className="history-item-submeta" style={{ fontSize: '0.8rem', color: '#64748b', margin: '3px 0 6px 0' }}>
                  <span>📅 {displayDate}</span>
                  {m.sourceType && <span> • 🏷️ {m.sourceType}</span>}
                </div>

                <p className="history-item-summary">
                  {m.summary ? (m.summary.length > 120 ? m.summary.substring(0, 120) + '...' : m.summary) : 'No summary available.'}
                </p>

                <div className="history-item-footer">
                  <span className="history-item-decisions">
                    ✓ {m.decisionCount} {m.decisionCount === 1 ? 'Decision' : 'Decisions'}
                  </span>
                  <span className="history-item-view-link">
                    {isSelected ? 'Active View' : 'Open Details →'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
