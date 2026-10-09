/**
 * MeetMind AI - API Service Layer
 * Interfaces exclusively with the Java Spring Boot Backend (default http://localhost:8080 or Vite proxy /api)
 */

const API_BASE = '/api';

/**
 * Submits meeting notes and metadata to Spring Boot for validation, AI agent analysis, and persistence.
 * Accepts either an object (AnalyzeRequest) or a raw string (meetingNotes).
 */
export async function analyzeMeeting(payload) {
  const body = typeof payload === 'string' ? { meetingNotes: payload } : payload;
  const response = await fetch(`${API_BASE}/meetings/analyze`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    let errorData;
    try {
      errorData = await response.json();
    } catch {
      errorData = { message: `Request failed with status ${response.status}` };
    }
    const errorMsg = errorData.message || (errorData.validationErrors && errorData.validationErrors.join(', ')) || errorData.error || 'Failed to analyze meeting';
    throw new Error(errorMsg);
  }

  return response.json();
}

/**
 * Parses an uploaded transcript file (.txt, .vtt, .srt) via backend TranscriptParserService.
 */
export async function parseTranscriptFile(file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/meetings/parse-transcript`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    let errorData;
    try {
      errorData = await response.json();
    } catch {
      errorData = { message: `Parse failed with status ${response.status}` };
    }
    throw new Error(errorData.message || errorData.error || 'Failed to parse transcript file');
  }

  return response.json();
}

/**
 * Parses raw text or transcript format via backend TranscriptParserService.
 */
export async function parseTranscriptText(text) {
  const response = await fetch(`${API_BASE}/meetings/parse-transcript-text`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ text }),
  });

  if (!response.ok) {
    let errorData;
    try {
      errorData = await response.json();
    } catch {
      errorData = { message: `Parse failed with status ${response.status}` };
    }
    throw new Error(errorData.message || errorData.error || 'Failed to parse transcript text');
  }

  return response.json();
}

/**
 * Transcribes audio or video file (WAV, MP3, M4A, MP4) via Groq Whisper API.
 */
export async function transcribeAudioFile(file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/meetings/transcribe`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    let errorData;
    try {
      errorData = await response.json();
    } catch {
      errorData = { message: `Transcription failed with status ${response.status}` };
    }
    throw new Error(errorData.message || errorData.error || 'Failed to transcribe audio file');
  }

  return response.json();
}

/**
 * Retrieves the list of historical meetings from Spring Boot.
 */
export async function fetchMeetings() {
  const response = await fetch(`${API_BASE}/meetings`);
  if (!response.ok) {
    throw new Error(`Failed to load meeting history (Status ${response.status})`);
  }
  return response.json();
}

/**
 * Retrieves full details for a single meeting (including action items and follow-up plan).
 */
export async function fetchMeetingDetails(id) {
  const response = await fetch(`${API_BASE}/meetings/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to load meeting details (Status ${response.status})`);
  }
  return response.json();
}

/**
 * Downloads action_items.csv for a meeting.
 */
export async function downloadActionItemsCsv(id) {
  const response = await fetch(`${API_BASE}/meetings/${id}/actions.csv`);
  if (!response.ok) {
    throw new Error('Failed to download action items CSV');
  }
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `meeting_${id}_action_items.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Downloads detailed follow-up plan PDF for a meeting with robust binary blob handling and error parsing.
 */
export async function downloadFollowUpPlanPdf(id, title = 'meeting') {
  if (!id) {
    throw new Error('Meeting ID is required to download PDF report.');
  }

  const response = await fetch(`${API_BASE}/meetings/${id}/follow-up-plan/pdf`, {
    method: 'GET',
    headers: {
      'Accept': 'application/pdf, application/json;q=0.9',
    },
  });

  const contentType = response.headers.get('content-type') || '';

  if (!response.ok) {
    let errorDetail = `Download failed with HTTP ${response.status}`;
    if (contentType.includes('application/json')) {
      try {
        const errorJson = await response.json();
        errorDetail = errorJson.message || errorJson.error || errorDetail;
      } catch {
        // Fallback
      }
    } else {
      try {
        const errorText = await response.text();
        if (errorText) errorDetail = errorText;
      } catch {}
    }
    throw new Error(errorDetail);
  }

  // Verify response is actually a PDF and not a JSON error or empty payload
  if (!contentType.includes('application/pdf')) {
    throw new Error(`Unexpected server response type (${contentType || 'unknown'}). Expected application/pdf.`);
  }

  const blob = await response.blob();
  if (blob.size === 0) {
    throw new Error('Received empty PDF file from server.');
  }

  // Derive filename from Content-Disposition header if available, otherwise construct safe filename
  let filename = '';
  const disposition = response.headers.get('content-disposition');
  if (disposition) {
    const filenameMatch = disposition.match(/filename\*?=['"]?(?:UTF-\d['"]*)?([^;\r\n"']+)['"]?/i);
    if (filenameMatch && filenameMatch[1]) {
      filename = decodeURIComponent(filenameMatch[1].trim());
    }
  }

  if (!filename) {
    const safeTitle = (title || 'meeting').replace(/[^a-zA-Z0-9_-]/g, '_');
    filename = `MeetMind_FollowUpPlan_${safeTitle}.pdf`;
  }

  // Trigger binary file download
  const url = window.URL.createObjectURL(blob);
  try {
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } finally {
    // Release object URL after initiation
    setTimeout(() => {
      window.URL.revokeObjectURL(url);
    }, 1000);
  }

  return { success: true, filename, sizeBytes: blob.size };
}

/**
 * Downloads follow_up_plan.json for a meeting.
 */
export async function downloadFollowUpPlanJson(id) {
  const response = await fetch(`${API_BASE}/meetings/${id}/follow-up.json`);
  if (!response.ok) {
    throw new Error('Failed to download follow-up plan JSON');
  }
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `meeting_${id}_follow_up_plan.json`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Health check endpoint for system status.
 */
export async function checkBackendHealth() {
  const response = await fetch(`${API_BASE}/health`);
  if (!response.ok) {
    throw new Error('Backend health check failed');
  }
  return response.json();
}
