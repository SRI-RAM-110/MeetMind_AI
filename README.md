# MeetMind AI — Multi-Agent Meeting Intelligence Assistant
> **Agent-X 2026 Enterprise Edition**

MeetMind AI is a comprehensive multi-agent meeting intelligence application that ingests meeting content across **4 input methods**, coordinates **5 specialized AI agents**, extracts meeting summaries, explicit decisions, structured deliverables, task owners, roles, deadlines, dependencies, and follow-up plans, displays meeting date and time metadata throughout, and generates a professional, detailed 7-section PDF report using OpenPDF.

---

## 🏛️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 frontend/ (React + Vite)                    │  Port 5173
│  - 4 Ingestion Tabs: Google Meet, Audio/Video, TXT, Notes   │
│  - Metadata Editor & Editable Transcript Preview            │
│  - Filterable Task Matrix & Person-Wise Responsibilities    │
│  - PDF Follow-Up Plan & CSV/JSON Exports                    │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST API Calls (HTTP JSON / Multipart)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                java-backend/ (Spring Boot)                  │  Port 8080 (STS Compatible)
│  - TranscriptParserService (.txt, .vtt, .srt, Google Meet)  │
│  - PdfReportService (7-Section PDF via OpenPDF 2.0.3)       │
│  - Single Database Owner & Duplicate Prevention             │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP POST /agent/analyze & /agent/transcribe
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 ai-agent/ (Python Flask)                    │  Port 5001
│  - Central MeetingOrchestrator                              │
│  - 5 Specialized Modular Agents (Groq LLM Llama 3.3 70B)    │
│    1. Transcript Agent   4. Follow-Up Agent                 │
│    2. Decision Agent     5. Review Agent (Audit & Check)    │
│    3. Action Item Agent                                     │
│  - Speech-to-Text via Groq Whisper API (whisper-large-v3)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Reads / Writes only via Spring Boot
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  MongoDB Community Server                   │  Port 27017
│  Database: meetmind_db (Inspected via MongoDB Compass)      │
│  - meetings                                                 │
│  - action_items                                             │
│  - follow_up_plans                                          │
│  - review_findings                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🌟 Four Ingestion Methods

1. **Import Google Meet Transcript**
   - Upload Google Meet transcript files (`.txt`, `.vtt`, `.srt`) saved from Google Drive.
   - Parses speaker names, timestamps, and cleans formatting artifacts while preserving evidence.
   - Allows correcting title, date, time, and time zone metadata.
2. **Audio / Video to Transcript (Speech-to-Text)**
   - Upload MP3, MP4, M4A, WAV, WebM, or FLAC recordings (up to 25MB).
   - Audio transcribed server-side via Groq Whisper API (`whisper-large-v3`) with zero client credential exposure.
   - Real-time progress, timestamp segments, and transcript review/editing before analysis.
3. **Upload Existing Transcript**
   - Upload any SRT, WebVTT, or plain text transcript file.
   - Normalized into a clean common transcript representation.
4. **Paste Meeting Text / Notes**
   - Direct textarea for pasted notes, bullet points, or raw conversation text.

All 4 input methods feed into the exact same **5-agent multi-agent pipeline**.

---

## 🤖 The Five Specialized AI Agents

Each agent has a strictly defined role, independent prompt, schema validation, and fallback logic:

1. **Transcript Agent**: Analyzes the transcript, extracts topics, participants, key discussion points, and executive summary.
2. **Decision Agent**: Extracts explicit agreed-upon decisions with direct supporting evidence; separates suggestions, proposals, and unresolved questions.
3. **Action Item Agent**: Extracts concrete deliverables with `taskId`, `title`, `description`, `ownerName`, `ownerRole`, `deadline`, `priority` (High/Medium/Low), `status`, `dependencies`, verbatim `sourceEvidence`, `sourceTimestamp`, and `needsConfirmation`.
4. **Follow-Up Agent**: Synthesizes an executive overview, milestone steps, AI-recommended checkpoints, confirmed upcoming review meetings, and pending clarifications.
5. **Review Agent**: Audits combined agent outputs against the transcript, detects contradictions or duplicate tasks, and flags ungrounded claims or missing information.

---

## 📑 Professional 7-Section PDF Follow-Up Plan

Generated via backend OpenPDF (`com.github.librepdf:openpdf:2.0.3`):
- **Section 1: Meeting Information**: Title, meeting date, start & end time, time zone, duration, source type, and participants.
- **Section 2: Executive Summary**: High-level overview of meeting purpose and key outcomes.
- **Section 3: Decisions Made**: Confirmed decisions, context, and supporting transcript quotes.
- **Section 4: Detailed Action Plan Table**: Task ID, title & description, responsible person, role, deadline, priority, status, dependencies, and confirmation flags. Repeated table headers across pages.
- **Section 5: Person-Wise Responsibilities**: Tasks grouped by owner with specific deliverables.
- **Section 6: Follow-Up Schedule**: Next sync date, milestones, AI checkpoints, and review meetings.
- **Section 7: Pending Clarifications**: Ambiguities, unassigned tasks, and details requiring confirmation.

---

## 🚀 Local Setup & Startup Instructions

### Prerequisites
- Java 21 JDK (Spring Boot) & Maven
- Node.js 18+ & npm (React + Vite)
- Python 3.10+ (Flask + Groq SDK)
- MongoDB Community Server running locally on `localhost:27017`
- Groq API Key (from [console.groq.com](https://console.groq.com/keys))

---

### Step 1: Python Multi-Agent Service Setup

```bash
cd ai-agent
python -m venv venv
# On Windows:
venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt

# Configure your Groq API key in ai-agent/.env
# GROQ_API_KEY=gsk_...
# GROQ_MODEL=llama-3.3-70b-versatile
# FLASK_PORT=5001

python app.py
```
*Runs on `http://localhost:5001` (Health check: `http://localhost:5001/health`).*

---

### Step 2: Spring Boot Backend Setup

```bash
cd java-backend
# Run tests
mvn test

# Run application (or run in Spring Tool Suite as Spring Boot App)
mvn spring-boot:run
```
*Runs on `http://localhost:8080` (Health check: `http://localhost:8080/api/health`).*

---

### Step 3: React + Vite Frontend Setup

```bash
cd frontend
npm install
npm run dev -- --host
```
*Runs on `http://localhost:5173`.*

---

## 🧪 Testing Instructions

1. **Python AI Agent Tests**:
   ```bash
   cd ai-agent
   python -m pytest tests/test_agent.py -v
   ```
   *Runs 14 automated unit and integration tests covering all 5 agents, orchestrator, edge cases, and Whisper endpoints.*

2. **Java Spring Boot Tests**:
   ```bash
   cd java-backend
   mvn test
   ```
   *Runs 10 unit tests covering `MeetingService`, `MeetingController`, duplicate prevention, and CSV/JSON generation.*

3. **Frontend Production Build**:
   ```bash
   cd frontend
   npm run build
   ```
   *Verifies zero JSX/JS compilation or asset bundling errors.*

---

## 📡 API Reference Summary

- `POST /api/meetings/parse-transcript` — Parse `.txt`, `.vtt`, `.srt` files and extract speakers and timestamps.
- `POST /api/meetings/parse-transcript-text` — Parse raw text or transcript format.
- `POST /api/meetings/transcribe` — Convert audio/video files to transcript using Groq Whisper.
- `POST /api/meetings/analyze` — Run full 5-agent pipeline and persist to MongoDB.
- `GET /api/meetings` — List historical meetings.
- `GET /api/meetings/{id}` — Get single meeting details.
- `GET /api/meetings/{id}/follow-up-plan/pdf` — Download 7-section PDF report.
- `GET /api/meetings/{id}/actions.csv` — Download CSV of action items.
- `GET /api/meetings/{id}/follow-up.json` — Download JSON follow-up plan.
