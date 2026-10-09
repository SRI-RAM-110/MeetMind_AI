import React, { useState, useEffect } from 'react';
import './App.css';
import MeetingInput from './components/MeetingInput';
import AgentPipelineProgress from './components/AgentPipelineProgress';
import SummarySection from './components/SummarySection';
import DecisionsSection from './components/DecisionsSection';
import ActionItemsTable from './components/ActionItemsTable';
import FollowUpPlanSection from './components/FollowUpPlanSection';
import ReviewFindingsSection from './components/ReviewFindingsSection';
import MeetingHistory from './components/MeetingHistory';
import { SAMPLE_MEETING_NOTES } from './data/sampleNotes';
import {
  analyzeMeeting,
  fetchMeetings,
  fetchMeetingDetails,
  downloadActionItemsCsv,
  downloadFollowUpPlanJson,
  downloadFollowUpPlanPdf,
  checkBackendHealth,
} from './services/api';

export default function App() {
  const [meetingNotes, setMeetingNotes] = useState('');
  const [currentMeeting, setCurrentMeeting] = useState(null);
  const [meetingsList, setMeetingsList] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [backendHealth, setBackendHealth] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Initial load: Fetch health status and meeting history from Spring Boot
  useEffect(() => {
    loadHealth();
    loadHistory();
  }, []);

  const loadHealth = async () => {
    try {
      const health = await checkBackendHealth();
      setBackendHealth(health);
    } catch {
      setBackendHealth({ status: 'DOWN', database: 'DISCONNECTED' });
    }
  };

  const loadHistory = async () => {
    try {
      setIsLoadingHistory(true);
      const list = await fetchMeetings();
      setMeetingsList(list || []);
    } catch {
      // Silently catch on initial startup if backend isn't running yet
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const handleAnalyze = async (payload) => {
    const rawContent = typeof payload === 'string' ? payload : payload?.meetingNotes;
    if (!rawContent || !rawContent.trim()) {
      setErrorMessage('Please enter, upload, or paste meeting notes before analyzing.');
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);
      setSuccessMessage(null);

      const result = await analyzeMeeting(payload);
      setCurrentMeeting(result);
      setSuccessMessage('Meeting analyzed by 5-Agent Multi-Agent Orchestrator and successfully persisted to MongoDB!');
      // Refresh history list so newly created meeting appears immediately
      await loadHistory();
    } catch (err) {
      setErrorMessage(err.message || 'An error occurred while analyzing the meeting.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectMeeting = async (id) => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const details = await fetchMeetingDetails(id);
      setCurrentMeeting(details);
      setSuccessMessage(`Loaded saved meeting details from MongoDB (ID: ${id})`);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load meeting details.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoadSample = () => {
    setMeetingNotes(SAMPLE_MEETING_NOTES);
    setErrorMessage(null);
  };

  const handleDownloadCsv = async (meetingId) => {
    await downloadActionItemsCsv(meetingId);
  };

  const handleDownloadJson = async (meetingId) => {
    await downloadFollowUpPlanJson(meetingId);
  };

  const handleDownloadPdf = async (meetingId, title) => {
    try {
      setErrorMessage(null);
      const res = await downloadFollowUpPlanPdf(meetingId, title);
      setSuccessMessage(`Successfully downloaded PDF report: ${res.filename} (${(res.sizeBytes / 1024).toFixed(1)} KB)`);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to download follow-up plan PDF.');
      throw err;
    }
  };

  return (
    <div className="meetmind-app">
      {/* Top Navigation / App Header */}
      <header className="app-header">
        <div className="header-container">
          <div className="brand-group">
            <div className="brand-icon">🧠</div>
            <div>
              <h1 className="brand-title">MeetMind AI</h1>
              <p className="brand-subtitle">Multi-Agent AI Business Meeting Assistant • 5 Specialized LLM Agents</p>
            </div>
          </div>

          <div className="header-status-group">
            <span className="status-pill status-pill-online">
              <span className="status-dot"></span>
              Multi-Agent Engine: 5 Agents
            </span>
            <span
              className={`status-pill ${
                backendHealth?.status === 'UP' ? 'status-pill-online' : 'status-pill-offline'
              }`}
            >
              <span className="status-dot"></span>
              Spring Boot: {backendHealth?.status === 'UP' ? 'Online' : 'Offline / Connecting'}
            </span>
            <span
              className={`status-pill ${
                backendHealth?.database === 'CONNECTED' ? 'status-pill-online' : 'status-pill-neutral'
              }`}
            >
              MongoDB: {backendHealth?.database === 'CONNECTED' ? 'Connected' : 'meetmind_db'}
            </span>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="app-container">
        {/* Notification Banners */}
        {errorMessage && (
          <div className="alert alert-error">
            <span className="alert-icon">⚠️</span>
            <div className="alert-content">
              <strong>Error:</strong> {errorMessage}
            </div>
            <button
              type="button"
              className="alert-close"
              onClick={() => setErrorMessage(null)}
            >
              ✕
            </button>
          </div>
        )}

        {successMessage && (
          <div className="alert alert-success">
            <span className="alert-icon">✓</span>
            <div className="alert-content">{successMessage}</div>
            <button
              type="button"
              className="alert-close"
              onClick={() => setSuccessMessage(null)}
            >
              ✕
            </button>
          </div>
        )}

        {/* Input Card Component */}
        <MeetingInput
          meetingNotes={meetingNotes}
          setMeetingNotes={setMeetingNotes}
          onAnalyze={handleAnalyze}
          isLoading={isLoading}
          onLoadSample={handleLoadSample}
        />

        {/* Live Multi-Agent Pipeline Progress Indicator */}
        <AgentPipelineProgress isLoading={isLoading} />

        {/* Active Analysis Results */}
        {currentMeeting && (
          <div className="analysis-results-section">
            {/* 1. Summary & Transcript Analysis */}
            <SummarySection meeting={currentMeeting} />

            {/* 2. Confirmed Decisions */}
            <DecisionsSection decisions={currentMeeting.decisions} />

            {/* 3. Structured Action Items */}
            <ActionItemsTable
              actionItems={currentMeeting.actionItems}
              meetingId={currentMeeting.id}
              onDownloadCsv={handleDownloadCsv}
            />

            {/* 4. Follow-Up Plan */}
            <FollowUpPlanSection
              followUpPlan={currentMeeting.followUpPlan}
              nextMeetingDate={currentMeeting.nextMeetingDate}
              unresolvedQuestions={currentMeeting.unresolvedQuestions}
              meetingId={currentMeeting.id}
              meetingTitle={currentMeeting.meetingTitle}
              onDownloadPdf={handleDownloadPdf}
              onDownloadJson={handleDownloadJson}
            />

            {/* 5. Review Agent Quality & Fact Verification Findings */}
            <ReviewFindingsSection
              reviewFindings={currentMeeting.reviewFindings}
              agentExecution={currentMeeting.agentExecution}
            />
          </div>
        )}

        {/* Meeting History Section */}
        <MeetingHistory
          meetings={meetingsList}
          currentMeetingId={currentMeeting?.id}
          onSelectMeeting={handleSelectMeeting}
          onRefresh={loadHistory}
          isLoadingHistory={isLoadingHistory}
        />
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <p>MeetMind AI — Multi-Agent AI Business Meeting Assistant</p>
        <p className="footer-stack">
          React (Vite) • Java 21 Spring Boot (STS) • Python Multi-Agent Service (5 Groq Agents) • MongoDB
        </p>
      </footer>
    </div>
  );
}
