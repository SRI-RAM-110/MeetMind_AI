import React, { useState } from 'react';
import './SummarySection.css';

export default function SummarySection({ meeting }) {
  const [showOriginalNotes, setShowOriginalNotes] = useState(false);

  if (!meeting) return null;

  const meetingTitle = meeting.meetingTitle || 'Business Meeting';
  const meetingDate = meeting.meetingDate || 'Not available';
  const sourceType = meeting.sourceType || 'PASTE';
  const sourceFileName = meeting.sourceFileName;
  const googleMeetingId = meeting.googleMeetingId;

  const topics = meeting.transcriptAnalysis?.topics || [];
  const participants = (meeting.participants && meeting.participants.length > 0)
    ? meeting.participants
    : (meeting.transcriptAnalysis?.participants || []);
  const speakers = meeting.speakers || [];
  const discussionPoints = meeting.transcriptAnalysis?.keyDiscussionPoints || [];
  const metadataProvenance = meeting.metadataProvenance || {};

  return (
    <div className="card summary-card">
      <div className="summary-header">
        <div className="summary-title-wrapper">
          <h2 className="summary-title">{meetingTitle}</h2>
          <div className="meeting-date-banner">
            <span>📅 <strong>Actual Meeting Date:</strong> {meetingDate}</span>
            {googleMeetingId && <span>🔗 <strong>Meet ID:</strong> {googleMeetingId}</span>}
            <span className="source-tag">Source: {sourceType} {sourceFileName ? `(${sourceFileName})` : ''}</span>
          </div>

          {/* Provenance Indicators */}
          {Object.keys(metadataProvenance).length > 0 && (
            <div className="provenance-summary-pills">
              {Object.entries(metadataProvenance).map(([key, prov]) => (
                <span
                  key={key}
                  className={`provenance-pill provenance-pill-${(prov?.status || 'info').toLowerCase()}`}
                  title={`Source: ${prov?.source || 'N/A'}, Method: ${prov?.method || 'N/A'}`}
                >
                  {key}: {prov?.status === 'VERIFIED' ? '🟢 Verified' : prov?.status === 'INFERRED' ? '🟡 Inferred' : '⚪ ' + (prov?.status || 'N/A')}
                </span>
              ))}
            </div>
          )}
        </div>
        <div className="summary-meta-badges">
          <span className="meta-badge status-badge-completed">
            ● {meeting.status || 'COMPLETED'}
          </span>
          {meeting.id && (
            <span className="meta-badge id-badge" title="MongoDB ObjectId">
              ID: {meeting.id}
            </span>
          )}
        </div>
      </div>

      <div className="summary-body">
        <p className="summary-text">{meeting.summary}</p>
      </div>

      {/* Structured Transcript Insights */}
      {(topics.length > 0 || participants.length > 0 || discussionPoints.length > 0) && (
        <div className="transcript-insights-grid">
          {topics.length > 0 && (
            <div className="insight-block">
              <span className="insight-label">🏷️ Topics Discussed</span>
              <div className="insight-tag-list">
                {topics.map((top, idx) => (
                  <span key={idx} className="topic-tag">
                    {top}
                  </span>
                ))}
              </div>
            </div>
          )}

          {participants.length > 0 && (
            <div className="insight-block">
              <span className="insight-label">👥 Confirmed Attendees / Participants</span>
              <div className="insight-tag-list">
                {participants.map((p, idx) => (
                  <span key={idx} className="participant-tag">
                    👤 {p}
                  </span>
                ))}
              </div>
            </div>
          )}

          {speakers.length > 0 && (
            <div className="insight-block">
              <span className="insight-label">🎙️ Transcript Speakers Detected</span>
              <div className="insight-tag-list">
                {speakers.map((s, idx) => (
                  <span key={idx} className="participant-tag speaker-tag">
                    🗣️ {s}
                  </span>
                ))}
              </div>
            </div>
          )}

          {discussionPoints.length > 0 && (
            <div className="insight-block full-width">
              <span className="insight-label">💡 Key Discussion Points</span>
              <ul className="discussion-points-list">
                {discussionPoints.map((pt, idx) => (
                  <li key={idx}>{pt}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {meeting.originalNotes && (
        <div className="original-notes-accordion">
          <button
            type="button"
            className="btn-accordion-toggle"
            onClick={() => setShowOriginalNotes(!showOriginalNotes)}
          >
            {showOriginalNotes ? '▼ Hide Original Transcript' : '▶ View Normalized Transcript Text'}
          </button>
          {showOriginalNotes && (
            <div className="original-notes-content">
              <pre>{meeting.originalNotes}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
