import React, { useState } from 'react';
import './ReviewFindingsSection.css';

export default function ReviewFindingsSection({ reviewFindings, agentExecution }) {
  const [selectedCategory, setSelectedCategory] = useState('ALL');

  const findings = reviewFindings || [];
  const hasFindings = findings.length > 0;

  const categories = ['ALL', ...Array.from(new Set(findings.map((f) => f.category || 'GENERAL')))];

  const filteredFindings = selectedCategory === 'ALL'
    ? findings
    : findings.filter((f) => (f.category || 'GENERAL') === selectedCategory);

  const passCount = findings.filter((f) => f.severity === 'PASS').length;
  const warningCount = findings.filter((f) => f.severity === 'WARNING').length;
  const flagCount = findings.filter((f) => f.severity === 'FLAG').length;

  const getSeverityBadgeClass = (severity) => {
    switch ((severity || '').toUpperCase()) {
      case 'PASS':
        return 'severity-badge-pass';
      case 'WARNING':
        return 'severity-badge-warning';
      case 'FLAG':
        return 'severity-badge-flag';
      case 'INFO':
      default:
        return 'severity-badge-info';
    }
  };

  const stages = agentExecution?.stages || [];

  return (
    <div className="card review-findings-card">
      <div className="review-findings-header">
        <div className="review-title-wrapper">
          <span className="section-tag">Quality Assurance & Fact Verification</span>
          <h2 className="review-title">Review Agent Audit Findings ({findings.length})</h2>
        </div>
        <div className="review-meta-pills">
          <span className="review-status-pill pill-pass">
            ✓ {passCount} Verified
          </span>
          {warningCount > 0 && (
            <span className="review-status-pill pill-warning">
              ⚠️ {warningCount} Warnings
            </span>
          )}
          {flagCount > 0 && (
            <span className="review-status-pill pill-flag">
              🚩 {flagCount} Flags
            </span>
          )}
        </div>
      </div>

      {stages.length > 0 && (
        <div className="agent-execution-strip">
          <span className="strip-title">Multi-Agent Execution Pipeline:</span>
          <div className="strip-badges">
            {stages.map((stg, i) => (
              <span key={i} className="strip-agent-badge" title={stg.details}>
                🤖 {stg.agent} ({stg.duration_ms}ms)
              </span>
            ))}
          </div>
        </div>
      )}

      {categories.length > 2 && (
        <div className="review-category-filters">
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              className={`cat-filter-btn ${selectedCategory === cat ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      )}

      {!hasFindings ? (
        <div className="empty-review">
          <p className="empty-text">No audit discrepancies found. All extracted facts adhere to strict accuracy rules.</p>
        </div>
      ) : (
        <div className="review-findings-list">
          {filteredFindings.map((finding, idx) => (
            <div key={finding.id || idx} className="finding-item-card">
              <div className="finding-top-row">
                <span className={`severity-badge ${getSeverityBadgeClass(finding.severity)}`}>
                  {finding.severity || 'INFO'}
                </span>
                <span className="finding-category-tag">
                  {(finding.category || 'AUDIT').replace(/_/g, ' ')}
                </span>
              </div>

              <h4 className="finding-issue">{finding.issue}</h4>

              {finding.evidenceChecked && (
                <div className="finding-evidence-box">
                  <span className="box-label">Evidence Checked:</span>
                  <span className="box-val">"{finding.evidenceChecked}"</span>
                </div>
              )}

              {finding.actionTaken && (
                <div className="finding-action-box">
                  <span className="box-label">Action Taken:</span>
                  <span className="box-val">{finding.actionTaken}</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
