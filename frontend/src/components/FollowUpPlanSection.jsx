import React, { useState } from 'react';
import './FollowUpPlanSection.css';

export default function FollowUpPlanSection({
  followUpPlan,
  nextMeetingDate,
  unresolvedQuestions,
  meetingId,
  meetingTitle,
  onDownloadPdf,
  onDownloadJson,
}) {
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDownloadingJson, setIsDownloadingJson] = useState(false);

  if (!followUpPlan && !nextMeetingDate && (!unresolvedQuestions || unresolvedQuestions.length === 0)) {
    return null;
  }

  const overview = followUpPlan?.overview || 'Execution roadmap for agreed deliverables.';
  const steps = followUpPlan?.steps || [];
  const checkpoints = followUpPlan?.checkpoints || [];
  const upcomingReviewMeetings = followUpPlan?.upcomingReviewMeetings || [];
  const pendingClarifications = followUpPlan?.pendingClarifications || [];
  const questions =
    (unresolvedQuestions && unresolvedQuestions.length > 0)
      ? unresolvedQuestions
      : followUpPlan?.unresolvedQuestions || [];
  const nextDate =
    nextMeetingDate && nextMeetingDate !== 'Not specified'
      ? nextMeetingDate
      : followUpPlan?.nextMeetingDate || 'Not specified';

  const handlePdfClick = async () => {
    if (!meetingId || isDownloadingPdf) return;
    try {
      setIsDownloadingPdf(true);
      await onDownloadPdf(meetingId, meetingTitle);
    } catch (err) {
      alert(err.message || 'Error generating PDF report');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleJsonClick = async () => {
    if (!meetingId || isDownloadingJson) return;
    try {
      setIsDownloadingJson(true);
      await onDownloadJson(meetingId);
    } catch (err) {
      alert(err.message || 'Error exporting JSON');
    } finally {
      setIsDownloadingJson(false);
    }
  };

  return (
    <div className="card follow-up-card">
      <div className="follow-up-header">
        <div className="follow-up-title-wrapper">
          <span className="section-tag">Follow-Up Agent • Strategic Roadmap</span>
          <h2 className="follow-up-title">Comprehensive Follow-Up Plan</h2>
        </div>
        <div className="follow-up-actions-top">
          {meetingId && (
            <>
              <button
                type="button"
                className="btn btn-primary btn-sm btn-download-pdf"
                onClick={handlePdfClick}
                disabled={isDownloadingPdf}
                title="Download professional 7-section PDF report with meeting info, executive summary, decision log, action matrix, and roadmap"
              >
                {isDownloadingPdf ? '⏳ Generating PDF...' : '📑 Download Detailed Follow-Up Plan (PDF)'}
              </button>
              <button
                type="button"
                className="btn btn-outline btn-sm btn-download-json"
                onClick={handleJsonClick}
                disabled={isDownloadingJson}
                title="Download follow-up roadmap as JSON"
              >
                {isDownloadingJson ? '⏳...' : '📄 Download JSON'}
              </button>
            </>
          )}
        </div>
      </div>

      {overview && (
        <div className="follow-up-overview-box">
          <span className="overview-label">Roadmap Overview:</span>
          <p className="overview-text">{overview}</p>
        </div>
      )}

      <div className="follow-up-content-grid">
        {/* Next Meeting Callout */}
        <div className="next-meeting-callout">
          <div className="next-meeting-icon">📅</div>
          <div className="next-meeting-details">
            <span className="next-meeting-label">Next Scheduled Sync</span>
            <span className="next-meeting-value">{nextDate}</span>
          </div>
        </div>

        {/* Milestone Steps */}
        <div className="steps-container">
          <h3 className="subheading-title">Next Milestone Steps ({steps.length})</h3>
          {steps.length === 0 ? (
            <p className="empty-subtext">No distinct milestone steps recorded.</p>
          ) : (
            <ol className="steps-list">
              {steps.map((step, idx) => (
                <li key={idx} className="step-item">
                  <span className="step-number">{idx + 1}</span>
                  <span className="step-text">{step}</span>
                </li>
              ))}
            </ol>
          )}
        </div>

        {/* AI Suggested Checkpoints */}
        {checkpoints.length > 0 && (
          <div className="checkpoints-container">
            <h3 className="subheading-title">
              💡 Suggested Checkpoints ({checkpoints.length})
              <span className="ai-rec-badge">AI Recommendation</span>
            </h3>
            <ul className="checkpoints-list">
              {checkpoints.map((cp, idx) => (
                <li key={idx} className="checkpoint-item">
                  <span className="checkpoint-icon">🚩</span>
                  <span className="checkpoint-text">{cp}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Upcoming Review Meetings */}
        {upcomingReviewMeetings.length > 0 && (
          <div className="review-meetings-container">
            <h3 className="subheading-title">🗓️ Upcoming Review Meetings ({upcomingReviewMeetings.length})</h3>
            <ul className="review-meetings-list">
              {upcomingReviewMeetings.map((rm, idx) => (
                <li key={idx} className="review-meeting-item">
                  <span>📌</span> {rm}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Pending Clarifications */}
        {pendingClarifications.length > 0 && (
          <div className="clarifications-container">
            <h3 className="subheading-title">⚠️ Pending Clarifications ({pendingClarifications.length})</h3>
            <ul className="clarifications-list">
              {pendingClarifications.map((item, idx) => (
                <li key={idx} className="clarification-item">
                  <span>🔍</span> {item}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Unresolved Questions & Clarifications */}
        <div className="unresolved-container">
          <h3 className="subheading-title">❓ Unresolved Questions & Open Items ({questions.length})</h3>
          {questions.length === 0 ? (
            <p className="empty-subtext">No open blockers or unresolved questions identified.</p>
          ) : (
            <ul className="questions-list">
              {questions.map((q, idx) => (
                <li key={idx} className="question-item">
                  <span className="question-bullet">❓</span>
                  <span className="question-text">{q}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
