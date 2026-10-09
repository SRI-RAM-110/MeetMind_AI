import React from 'react';
import './DecisionsSection.css';

export default function DecisionsSection({ decisions }) {
  const hasDecisions = decisions && decisions.length > 0;

  return (
    <div className="card decisions-card">
      <div className="decisions-header">
        <div className="decisions-title-wrapper">
          <span className="section-tag">Agreed Outcomes</span>
          <h2 className="decisions-title">Confirmed Decisions ({hasDecisions ? decisions.length : 0})</h2>
        </div>
      </div>

      {!hasDecisions ? (
        <div className="empty-decisions">
          <p className="empty-text">No confirmed decisions were explicitly recorded in these notes.</p>
        </div>
      ) : (
        <div className="decisions-grid">
          {decisions.map((decision, index) => (
            <div key={index} className="decision-item">
              <span className="decision-icon">✓</span>
              <div className="decision-content">
                <span className="decision-badge">Decision #{index + 1}</span>
                <p className="decision-text">{decision}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
