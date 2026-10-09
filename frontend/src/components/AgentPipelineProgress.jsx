import React, { useState, useEffect } from 'react';
import './AgentPipelineProgress.css';

const STAGES = [
  {
    id: 1,
    name: 'Transcript Agent',
    description: 'Analyzing notes, identifying topics and participants',
    icon: '📝',
    durationEst: 1800,
  },
  {
    id: 2,
    name: 'Decision & Action Agents',
    description: 'Extracting confirmed decisions & assigned deliverables (Parallel)',
    icon: '⚡',
    durationEst: 2200,
  },
  {
    id: 3,
    name: 'Follow-Up Agent',
    description: 'Synthesizing timeline, next steps & unresolved questions',
    icon: '🗓️',
    durationEst: 1800,
  },
  {
    id: 4,
    name: 'Review Agent',
    description: 'Auditing claims, verifying dates & checking evidence',
    icon: '🛡️',
    durationEst: 2000,
  },
  {
    id: 5,
    name: 'Spring Boot & MongoDB',
    description: 'Validating final intelligence & persisting records',
    icon: '💾',
    durationEst: 1200,
  },
];

export default function AgentPipelineProgress({ isLoading }) {
  const [activeStageIndex, setActiveStageIndex] = useState(0);

  useEffect(() => {
    if (!isLoading) {
      setActiveStageIndex(0);
      return;
    }

    let currentIdx = 0;
    const interval = setInterval(() => {
      currentIdx = (currentIdx + 1) % STAGES.length;
      setActiveStageIndex(currentIdx);
    }, 2000);

    return () => clearInterval(interval);
  }, [isLoading]);

  if (!isLoading) return null;

  const currentStage = STAGES[activeStageIndex];
  const progressPercent = Math.min(100, Math.round(((activeStageIndex + 1) / STAGES.length) * 100));

  return (
    <div className="card pipeline-progress-card">
      <div className="pipeline-header">
        <div className="pipeline-header-title">
          <span className="pipeline-pulse-dot"></span>
          <h3>Multi-Agent Orchestrator Active</h3>
        </div>
        <span className="pipeline-badge">
          Stage {activeStageIndex + 1} of {STAGES.length} • {progressPercent}%
        </span>
      </div>

      <div className="pipeline-progress-bar-container">
        <div
          className="pipeline-progress-bar-fill"
          style={{ width: `${progressPercent}%` }}
        ></div>
      </div>

      <div className="pipeline-stages-grid">
        {STAGES.map((stage, idx) => {
          const isDone = idx < activeStageIndex;
          const isCurrent = idx === activeStageIndex;
          const isPending = idx > activeStageIndex;

          let stateClass = 'stage-pending';
          if (isDone) stateClass = 'stage-done';
          if (isCurrent) stateClass = 'stage-active';

          return (
            <div key={stage.id} className={`pipeline-stage-item ${stateClass}`}>
              <div className="stage-icon-circle">
                {isDone ? '✓' : stage.icon}
              </div>
              <div className="stage-info">
                <span className="stage-name">{stage.name}</span>
                <span className="stage-desc">{stage.description}</span>
              </div>
              {isCurrent && <span className="stage-active-pill">Processing...</span>}
            </div>
          );
        })}
      </div>

      <div className="pipeline-current-focus">
        <span className="focus-label">Current Task:</span>
        <strong className="focus-text">
          {currentStage.icon} {currentStage.name} — {currentStage.description}
        </strong>
      </div>
    </div>
  );
}
