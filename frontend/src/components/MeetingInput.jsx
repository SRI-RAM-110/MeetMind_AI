import React, { useState, useRef, useEffect } from 'react';
import './MeetingInput.css';
import { parseTranscriptFile, parseTranscriptText, transcribeAudioFile } from '../services/api';

export default function MeetingInput({
  meetingNotes,
  setMeetingNotes,
  onAnalyze,
  isLoading,
  onLoadSample,
}) {
  const [activeTab, setActiveTab] = useState('google-meet'); // 'google-meet' | 'audio-video' | 'upload-transcript' | 'paste-notes'

  // Metadata state - initialized cleanly without inventing arbitrary dates or dummy names
  const [meetingTitle, setMeetingTitle] = useState('');
  const [meetingDate, setMeetingDate] = useState('');
  const [participantsText, setParticipantsText] = useState('');
  const [sourceType, setSourceType] = useState('GOOGLE_MEET_FILE');
  const [sourceFileName, setSourceFileName] = useState('');
  const [googleMeetingId, setGoogleMeetingId] = useState('');
  const [detectedSpeakers, setDetectedSpeakers] = useState([]);

  // Provenance dictionary: fieldKey -> { status: 'VERIFIED' | 'INFERRED' | 'NOT_AVAILABLE' | 'MANUAL_CORRECTION', source, method, confidence }
  const [metadataProvenance, setMetadataProvenance] = useState({});

  // Status and file parsing states
  const [isProcessingFile, setIsProcessingFile] = useState(false);
  const [fileStatusMessage, setFileStatusMessage] = useState(null);
  const [fileErrorMessage, setFileErrorMessage] = useState(null);
  const [parsedPreviewInfo, setParsedPreviewInfo] = useState(null);

  const googleMeetFileRef = useRef(null);
  const audioVideoFileRef = useRef(null);
  const transcriptFileRef = useRef(null);

  // Helper to apply parsed metadata results into state with provenance
  const applyParsedMetadata = (parsed, sourceTypeName, fileName) => {
    setMeetingNotes(parsed.cleanedTranscript || '');
    setSourceType(sourceTypeName);
    if (fileName) setSourceFileName(fileName);

    const prov = parsed.metadataFields || {};
    setMetadataProvenance(prov);

    // 1. Meeting Title
    if (parsed.suggestedTitle) {
      setMeetingTitle(parsed.suggestedTitle);
    } else {
      setMeetingTitle('');
    }

    // 2. Meeting Date
    if (parsed.suggestedDate) {
      setMeetingDate(parsed.suggestedDate);
    } else {
      setMeetingDate('');
    }

    // 3. Meeting ID
    setGoogleMeetingId(parsed.googleMeetingId || '');

    // 4. Participants vs Speakers
    const participants = parsed.detectedParticipants || [];
    setParticipantsText(participants.join(', '));
    setDetectedSpeakers(parsed.detectedSpeakers || []);

    setParsedPreviewInfo({
      format: parsed.format,
      speakers: parsed.detectedSpeakers || [],
      wordCount: parsed.wordCount || 0,
      timestampCount: parsed.timestampCount || 0,
    });
  };

  // Mark field as manually corrected/confirmed by user
  const handleFieldChange = (fieldKey, value, setter) => {
    setter(value);
    setMetadataProvenance((prev) => ({
      ...prev,
      [fieldKey]: {
        value,
        status: 'MANUAL_CORRECTION',
        source: 'USER_INPUT',
        method: 'USER_EDIT',
        confidence: 1.0,
      },
    }));
  };

  // Helper to render provenance badge for a metadata field
  const renderFieldBadge = (fieldKey, currentValue) => {
    const prov = metadataProvenance[fieldKey];
    if (prov?.status === 'MANUAL_CORRECTION') {
      return <span className="meta-field-badge badge-manual">🔵 User Confirmed</span>;
    }
    if (prov?.status === 'VERIFIED') {
      return (
        <span className="meta-field-badge badge-verified" title={`Verified via ${prov.method || 'Source Metadata'}`}>
          🟢 Verified
        </span>
      );
    }
    if (prov?.status === 'INFERRED') {
      return (
        <span className="meta-field-badge badge-inferred" title={`Inferred via ${prov.method || 'Transcript Text'}`}>
          🟡 Inferred
        </span>
      );
    }
    if (!currentValue || prov?.status === 'NOT_AVAILABLE') {
      return (
        <span className="meta-field-badge badge-missing" title="Missing from source; please confirm or enter">
          ⚪ Not available
        </span>
      );
    }
    return <span className="meta-field-badge badge-missing">⚪ Pending</span>;
  };

  // Handle Google Meet file import (.txt, .vtt, .srt)
  const handleGoogleMeetUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validExtensions = ['.txt', '.vtt', '.srt'];
    const hasValidExt = validExtensions.some((ext) => file.name.toLowerCase().endsWith(ext));
    if (!hasValidExt) {
      setFileErrorMessage('Invalid file format. Please upload a .txt, .vtt, or .srt Google Meet transcript.');
      e.target.value = '';
      return;
    }

    try {
      setIsProcessingFile(true);
      setFileErrorMessage(null);
      setFileStatusMessage(`Extracting meeting metadata and cues from ${file.name}...`);

      const parsed = await parseTranscriptFile(file);
      applyParsedMetadata(parsed, 'GOOGLE_MEET_FILE', file.name);

      setFileStatusMessage(
        `✓ Automatically extracted metadata from ${file.name} (${parsed.format}, ${parsed.wordCount} words, ${parsed.speakerCount} speakers). Review details below.`
      );
    } catch (err) {
      setFileErrorMessage(err.message || 'Failed to parse Google Meet transcript.');
    } finally {
      setIsProcessingFile(false);
      e.target.value = '';
    }
  };

  // Handle Audio/Video upload & Speech-to-Text via Groq Whisper API
  const handleAudioVideoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const allowed = ['.mp3', '.mp4', '.m4a', '.wav', '.webm', '.ogg', '.flac'];
    const hasValidExt = allowed.some((ext) => file.name.toLowerCase().endsWith(ext));
    if (!hasValidExt) {
      setFileErrorMessage('Unsupported media format. Please upload MP3, MP4, M4A, WAV, WebM, or FLAC.');
      e.target.value = '';
      return;
    }

    if (file.size > 25 * 1024 * 1024) {
      setFileErrorMessage(`File size (${(file.size / (1024 * 1024)).toFixed(1)}MB) exceeds 25MB transcription limit.`);
      e.target.value = '';
      return;
    }

    try {
      setIsProcessingFile(true);
      setFileErrorMessage(null);
      setFileStatusMessage(`Transcribing ${file.name} using Groq Speech-to-Text (Whisper Large v3)...`);

      const result = await transcribeAudioFile(file);
      const transcript = result.transcript || '';
      setMeetingNotes(transcript);

      // Now auto-extract metadata from the transcribed text
      const parsed = await parseTranscriptText(transcript);
      applyParsedMetadata(parsed, 'AUDIO_RECORDING', file.name);

      setFileStatusMessage(`✓ Audio transcribed successfully! Extracted meeting metadata and speakers from transcript.`);
    } catch (err) {
      setFileErrorMessage(err.message || 'Audio transcription failed. Ensure Groq API key is configured.');
    } finally {
      setIsProcessingFile(false);
      e.target.value = '';
    }
  };

  // Handle generic transcript upload (.txt, .vtt, .srt)
  const handleGenericTranscriptUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsProcessingFile(true);
      setFileErrorMessage(null);
      setFileStatusMessage(`Parsing transcript: ${file.name}...`);

      const parsed = await parseTranscriptFile(file);
      applyParsedMetadata(parsed, 'UPLOAD_TRANSCRIPT', file.name);

      setFileStatusMessage(`✓ Extracted metadata from ${file.name}. Review and correct if needed.`);
    } catch (err) {
      setFileErrorMessage(err.message || 'Failed to parse transcript file.');
    } finally {
      setIsProcessingFile(false);
      e.target.value = '';
    }
  };

  // Auto-detect and extract metadata from current text in editor
  const handleExtractFromPastedText = async () => {
    if (!meetingNotes || meetingNotes.trim().length < 10) {
      setFileErrorMessage('Please enter or paste meeting notes first (at least 10 characters).');
      return;
    }
    try {
      setIsProcessingFile(true);
      setFileErrorMessage(null);
      setFileStatusMessage('Extracting meeting metadata and speakers from text...');

      const parsed = await parseTranscriptText(meetingNotes);
      applyParsedMetadata(parsed, sourceType || 'PASTE', sourceFileName || 'pasted_text.txt');

      setFileStatusMessage(`✓ Metadata extraction complete (${parsed.speakerCount} speakers detected). Review below.`);
    } catch (err) {
      setFileErrorMessage(err.message || 'Failed to extract metadata from text.');
    } finally {
      setIsProcessingFile(false);
    }
  };

  const handleClear = () => {
    setMeetingNotes('');
    setMeetingTitle('');
    setMeetingDate('');
    setParticipantsText('');
    setGoogleMeetingId('');
    setDetectedSpeakers([]);
    setMetadataProvenance({});
    setParsedPreviewInfo(null);
    setFileStatusMessage(null);
    setFileErrorMessage(null);
  };

  const characterCount = meetingNotes.trim().length;
  const isInputValid = characterCount >= 10;

  // Validation: Essential fields (title and date) need to be present or confirmed
  const hasEssentialTitle = Boolean(meetingTitle && meetingTitle.trim().length > 0);
  const hasEssentialDate = Boolean(meetingDate && meetingDate.trim().length > 0);

  const handleSubmitAnalysis = () => {
    if (!hasEssentialTitle) {
      setFileErrorMessage('Meeting Title is required. Please enter or confirm the meeting title.');
      return;
    }
    if (!hasEssentialDate) {
      setFileErrorMessage('Meeting Date is required. Please enter or confirm the meeting date.');
      return;
    }

    const participantsList = participantsText
      ? participantsText
          .split(',')
          .map((p) => p.trim())
          .filter(Boolean)
      : [];

    const payload = {
      meetingNotes,
      meetingTitle: meetingTitle.trim(),
      meetingDate: meetingDate.trim(),
      sourceType: sourceType || 'PASTE',
      sourceFileName: sourceFileName || null,
      googleMeetingId: googleMeetingId || null,
      participants: participantsList,
      speakers: detectedSpeakers,
      metadataProvenance,
    };

    onAnalyze(payload);
  };

  return (
    <div className="card meeting-input-card">
      <div className="meeting-input-header">
        <div>
          <span className="section-tag">Input Method & Automatic Extraction</span>
          <h2 className="input-card-title">Meeting Content & Metadata Setup</h2>
          <p className="input-card-subtitle">
            Ingest meeting intelligence via 4 unified input methods with automatic metadata extraction.
          </p>
        </div>
        <div className="input-actions-top">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={async () => {
              if (onLoadSample) {
                onLoadSample();
                setTimeout(() => {
                  handleExtractFromPastedText();
                }, 100);
              }
            }}
            disabled={isLoading || isProcessingFile}
            title="Load realistic sample meeting transcript"
          >
            📋 Load Sample Sync
          </button>
        </div>
      </div>

      {/* 4 Input Method Tabs */}
      <div className="input-tabs-nav" role="tablist">
        <button
          type="button"
          className={`input-tab-btn ${activeTab === 'google-meet' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('google-meet');
            setSourceType('GOOGLE_MEET_FILE');
          }}
        >
          <span className="tab-icon">📥</span>
          <span className="tab-title">1. Import Google Meet Transcript</span>
        </button>

        <button
          type="button"
          className={`input-tab-btn ${activeTab === 'audio-video' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('audio-video');
            setSourceType('AUDIO_RECORDING');
          }}
        >
          <span className="tab-icon">🎙️</span>
          <span className="tab-title">2. Audio / Video to Transcript</span>
        </button>

        <button
          type="button"
          className={`input-tab-btn ${activeTab === 'upload-transcript' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('upload-transcript');
            setSourceType('UPLOAD_TRANSCRIPT');
          }}
        >
          <span className="tab-icon">📄</span>
          <span className="tab-title">3. Upload Existing Transcript</span>
        </button>

        <button
          type="button"
          className={`input-tab-btn ${activeTab === 'paste-notes' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('paste-notes');
            setSourceType('PASTE');
          }}
        >
          <span className="tab-icon">✍️</span>
          <span className="tab-title">4. Paste Meeting Text</span>
        </button>
      </div>

      {/* Tab 1: Google Meet Transcript Import Area */}
      {activeTab === 'google-meet' && (
        <div className="tab-panel-container">
          <div className="upload-dropzone" onClick={() => googleMeetFileRef.current?.click()}>
            <span className="dropzone-icon">📥</span>
            <div className="dropzone-text">
              <strong>Import Google Meet Transcript file (.txt, .vtt, .srt)</strong>
              <p>Google Meet transcripts downloaded from Google Drive. Metadata headers, speaker cues, and timestamps are extracted automatically without manual entry.</p>
            </div>
            <button type="button" className="btn btn-outline btn-sm" disabled={isProcessingFile}>
              Browse Files
            </button>
            <input
              type="file"
              ref={googleMeetFileRef}
              onChange={handleGoogleMeetUpload}
              accept=".txt,.vtt,.srt"
              className="hidden-file-input"
            />
          </div>
          <div className="tab-helper-note">
            💡 <em>Drive Export Support:</em> Select your transcript file exported from Google Drive. Automatic extraction parses meeting title, date, speaker labels, and timestamps.
          </div>
        </div>
      )}

      {/* Tab 2: Speech-to-Text Upload Area */}
      {activeTab === 'audio-video' && (
        <div className="tab-panel-container">
          <div className="upload-dropzone audio-dropzone" onClick={() => audioVideoFileRef.current?.click()}>
            <span className="dropzone-icon">🎙️</span>
            <div className="dropzone-text">
              <strong>Upload Meeting Audio or Video (MP3, MP4, M4A, WAV, FLAC)</strong>
              <p>Processed server-side via Groq Whisper API (whisper-large-v3). Transcript is automatically recognized.</p>
            </div>
            <button type="button" className="btn btn-outline btn-sm" disabled={isProcessingFile}>
              Select Audio/Video
            </button>
            <input
              type="file"
              ref={audioVideoFileRef}
              onChange={handleAudioVideoUpload}
              accept=".mp3,.mp4,.m4a,.wav,.webm,.ogg,.flac"
              className="hidden-file-input"
            />
          </div>
          <div className="tab-helper-note">
            ⚡ Max file size: 25MB. Audio is transcribed automatically into clean text for analysis.
          </div>
        </div>
      )}

      {/* Tab 3: Upload Existing Transcript */}
      {activeTab === 'upload-transcript' && (
        <div className="tab-panel-container">
          <div className="upload-dropzone" onClick={() => transcriptFileRef.current?.click()}>
            <span className="dropzone-icon">📄</span>
            <div className="dropzone-text">
              <strong>Upload Transcript File (.txt, .vtt, .srt)</strong>
              <p>Extracts WebVTT voice tags, SRT caption ranges, and speaker blocks into cleaned meeting text and metadata.</p>
            </div>
            <button type="button" className="btn btn-outline btn-sm" disabled={isProcessingFile}>
              Select File
            </button>
            <input
              type="file"
              ref={transcriptFileRef}
              onChange={handleGenericTranscriptUpload}
              accept=".txt,.vtt,.srt"
              className="hidden-file-input"
            />
          </div>
        </div>
      )}

      {/* Tab 4: Paste Meeting Text */}
      {activeTab === 'paste-notes' && (
        <div className="tab-panel-container tab-paste-notice">
          <p>Paste your raw meeting notes, attendee statements, and decisions below. Click "Auto-Extract Metadata" to detect title, date, and speakers.</p>
          {characterCount >= 10 && (
            <button
              type="button"
              className="btn btn-outline btn-sm btn-auto-extract"
              onClick={handleExtractFromPastedText}
              disabled={isProcessingFile}
            >
              ✨ Auto-Extract Meeting Metadata
            </button>
          )}
        </div>
      )}

      {/* File feedback alerts */}
      {fileStatusMessage && (
        <div className="alert-inline alert-info">
          <span>ℹ️</span> {fileStatusMessage}
        </div>
      )}
      {fileErrorMessage && (
        <div className="alert-inline alert-danger">
          <span>⚠️</span> {fileErrorMessage}
        </div>
      )}

      {/* Extracted Meeting Metadata Panel */}
      <div className="metadata-form-card">
        <div className="metadata-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h3 className="metadata-heading" style={{ margin: 0 }}>📋 Automatic Meeting Metadata Extraction</h3>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem', color: '#64748b' }}>
              Extracted automatically from source files or transcript text. Fields clearly distinguish verified, inferred, and missing values.
            </p>
          </div>
          {parsedPreviewInfo && (
            <span className="meta-field-badge badge-verified" style={{ padding: '4px 10px' }}>
              Format: {parsedPreviewInfo.format}
            </span>
          )}
        </div>

        <div className="metadata-grid">
          {/* Meeting Title */}
          <div className="form-group">
            <div className="field-header-row">
              <label className="form-label">Meeting Title *</label>
              {renderFieldBadge('title', meetingTitle)}
            </div>
            <input
              type="text"
              className={`form-input ${!hasEssentialTitle && characterCount > 0 ? 'input-needs-attention' : ''}`}
              value={meetingTitle}
              onChange={(e) => handleFieldChange('title', e.target.value, setMeetingTitle)}
              placeholder="e.g. Q4 Sprint Planning Sync (or extracted from header)"
              disabled={isLoading || isProcessingFile}
            />
            {!hasEssentialTitle && characterCount > 0 && (
              <span className="field-hint-warning" style={{ fontSize: '0.75rem', color: '#dc2626' }}>
                Required: Please confirm or enter a meeting title.
              </span>
            )}
          </div>

          {/* Meeting Date */}
          <div className="form-group">
            <div className="field-header-row">
              <label className="form-label">Actual Meeting Date *</label>
              {renderFieldBadge('date', meetingDate)}
            </div>
            <input
              type="text"
              className={`form-input ${!hasEssentialDate && characterCount > 0 ? 'input-needs-attention' : ''}`}
              value={meetingDate}
              onChange={(e) => handleFieldChange('date', e.target.value, setMeetingDate)}
              placeholder="YYYY-MM-DD or Month DD, YYYY"
              disabled={isLoading || isProcessingFile}
            />
            {!hasEssentialDate && characterCount > 0 && (
              <span className="field-hint-warning" style={{ fontSize: '0.75rem', color: '#dc2626' }}>
                Required: Enter actual meeting date (file creation date is not assumed).
              </span>
            )}
          </div>



          {/* Google Meet ID */}
          {googleMeetingId && (
            <div className="form-group">
              <div className="field-header-row">
                <label className="form-label">Google Meet ID</label>
                {renderFieldBadge('googleMeetingId', googleMeetingId)}
              </div>
              <input
                type="text"
                className="form-input"
                value={googleMeetingId}
                onChange={(e) => handleFieldChange('googleMeetingId', e.target.value, setGoogleMeetingId)}
                disabled={isLoading || isProcessingFile}
              />
            </div>
          )}

          {/* Confirmed Participants */}
          <div className="form-group full-width">
            <div className="field-header-row">
              <label className="form-label">Confirmed Participants (Meeting Attendees)</label>
              {renderFieldBadge('participants', participantsText)}
            </div>
            <input
              type="text"
              className="form-input"
              value={participantsText}
              onChange={(e) => handleFieldChange('participants', e.target.value, setParticipantsText)}
              placeholder="e.g. Sarah Connor, Alex Mercer, Priya Sharma (comma-separated)"
              disabled={isLoading || isProcessingFile}
            />
            <span style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '2px', display: 'block' }}>
              Distinguished from transcript speaker labels and third parties mentioned in discussion.
            </span>
          </div>
        </div>

        {/* Separate Transcript Speakers Indicator */}
        {detectedSpeakers.length > 0 && (
          <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid #e2e8f0' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#334155' }}>
              🎙️ Transcript Speakers Detected ({detectedSpeakers.length}):
            </span>
            <div className="speakers-chips-container">
              {detectedSpeakers.map((spk, idx) => (
                <span key={idx} className="speaker-chip">
                  🗣️ {spk}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Editable Transcript Preview Area */}
      <div className="textarea-wrapper">
        <div className="textarea-header">
          <label className="textarea-label">
            Transcript / Meeting Text (Review & Edit before running AI agents):
          </label>
          <span className="source-indicator">Source: {sourceType} {sourceFileName ? `• ${sourceFileName}` : ''}</span>
        </div>
        <textarea
          className="meeting-textarea"
          rows={10}
          placeholder="Transcript text will appear here after upload, speech-to-text, or paste. You may review and edit the content freely before analyzing..."
          value={meetingNotes}
          onChange={(e) => setMeetingNotes(e.target.value)}
          disabled={isLoading || isProcessingFile}
        />
      </div>

      <div className="meeting-input-footer">
        <div className="input-stats">
          <span className="char-count">
            {characterCount} characters {characterCount < 10 && characterCount > 0 && '(min 10 required)'}
          </span>
          {characterCount > 0 && (
            <button
              type="button"
              className="btn-text"
              onClick={handleClear}
              disabled={isLoading || isProcessingFile}
            >
              Clear All
            </button>
          )}
        </div>

        <button
          type="button"
          className="btn btn-primary btn-analyze"
          onClick={handleSubmitAnalysis}
          disabled={!isInputValid || isLoading || isProcessingFile}
        >
          {isLoading ? (
            <>
              <span className="spinner"></span>
              <span>Running 5-Agent Multi-Agent Pipeline...</span>
            </>
          ) : (
            <>
              <span>⚡ Analyze Meeting ({characterCount} chars)</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}
