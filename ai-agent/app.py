"""
MeetMind AI - AI Agent Flask Service
Exposes POST /agent/analyze and GET /health.
Coordinated by the Spring Boot backend. Does not write directly to MongoDB.
"""

import os
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv

from orchestrator import MeetingOrchestrator, OrchestrationError

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s in %(module)s: %(message)s"
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Configure CORS (allow specific origins or wildcard)
cors_origins_env = os.getenv("CORS_ALLOWED_ORIGINS", "*").strip()
if cors_origins_env == "*":
    CORS(app, resources={r"/*": {"origins": "*"}})
else:
    origins_list = [origin.strip() for origin in cors_origins_env.split(",") if origin.strip()]
    CORS(app, resources={r"/*": {"origins": origins_list}})

# Initialize Central Multi-Agent Orchestrator
orchestrator = MeetingOrchestrator()


@app.route("/health", methods=["GET"])
def health_check():
    """
    Service health check endpoint.
    Exposes multi-agent service status without leaking the actual Groq API key.
    """
    has_api_key = bool(
        orchestrator.api_key and 
        orchestrator.api_key.strip() and 
        orchestrator.api_key not in ["replace_me", "your_groq_api_key_here"]
    )
    return jsonify({
        "status": "UP",
        "service": "MeetMind AI Multi-Agent Service",
        "architecture": "Multi-Agent Orchestrator (5 Specialized Agents)",
        "agents": [
            "Transcript Agent",
            "Decision Agent",
            "Action Item Agent",
            "Follow-Up Agent",
            "Review Agent"
        ],
        "groq_configured": has_api_key,
        "model": orchestrator.model,
        "mode": "live_groq" if orchestrator.is_live_client_ready() else "offline_rule_based"
    }), 200


@app.route("/agent/analyze", methods=["POST"])
def analyze_meeting():
    """
    Internal endpoint called by Spring Boot.
    Accepts raw meeting notes and executes the 5-agent orchestrated workflow.
    Returns validated structured meeting intelligence.
    """
    if not request.is_json:
        return jsonify({
            "error": "Invalid request: Content-Type must be application/json"
        }), 400

    payload = request.get_json() or {}
    meeting_notes = payload.get("meeting_notes")

    if not meeting_notes or not str(meeting_notes).strip():
        return jsonify({
            "error": "Validation error: 'meeting_notes' field is required and cannot be empty"
        }), 400

    try:
        logger.info("Received meeting analysis request (%d chars)", len(str(meeting_notes)))
        result = orchestrator.analyze_meeting(str(meeting_notes))
        logger.info(
            "Orchestration complete: %d decisions, %d action items, %d review findings",
            len(result.get("decisions", [])),
            len(result.get("action_items", [])),
            len(result.get("review_findings", []))
        )
        return jsonify(result), 200

    except ValueError as ve:
        logger.warning("Validation issue in meeting analysis: %s", ve)
        return jsonify({"error": str(ve)}), 400
    except OrchestrationError as oe:
        logger.error("Orchestration pipeline failed at stage '%s': %s", oe.stage, oe)
        return jsonify({
            "error": str(oe),
            "stage": oe.stage
        }), 502
    except Exception as e:
        logger.error("Unhandled error during meeting analysis: %s", e, exc_info=True)
        return jsonify({
            "error": f"Failed to analyze meeting notes: {str(e)}"
        }), 500


@app.route("/agent/transcribe", methods=["POST"])
def transcribe_audio():
    """
    Accepts multipart audio/video file upload and transcribes via Groq Whisper API.
    Supported extensions: .mp3, .mp4, .m4a, .wav, .webm, .ogg, .flac.
    """
    if "file" not in request.files:
        return jsonify({"error": "No file part provided in request."}), 400

    uploaded_file = request.files["file"]
    if not uploaded_file or not uploaded_file.filename:
        return jsonify({"error": "No file selected."}), 400

    filename = uploaded_file.filename.lower()
    allowed_exts = (".mp3", ".mp4", ".m4a", ".wav", ".webm", ".ogg", ".flac")
    if not any(filename.endswith(ext) for ext in allowed_exts):
        return jsonify({
            "error": f"Unsupported media format. Allowed formats: {', '.join(allowed_exts)}"
        }), 400

    # Check file size (Groq limit is 25MB)
    uploaded_file.seek(0, os.SEEK_END)
    file_size_bytes = uploaded_file.tell()
    uploaded_file.seek(0)

    if file_size_bytes > 25 * 1024 * 1024:
        return jsonify({
            "error": f"File size ({file_size_bytes / (1024*1024):.1f}MB) exceeds 25MB limit."
        }), 400

    logger.info("Received audio transcription request: %s (%.2f MB)", uploaded_file.filename, file_size_bytes / (1024*1024))

    # If Groq client is configured, call audio transcriptions API
    if orchestrator.is_live_client_ready():
        try:
            transcription = orchestrator.client.audio.transcriptions.create(
                file=(uploaded_file.filename, uploaded_file.read()),
                model="whisper-large-v3",
                response_format="verbose_json"
            )
            
            transcript_text = getattr(transcription, "text", "") or ""
            duration_secs = getattr(transcription, "duration", None)
            duration_mins = int(round(duration_secs / 60)) if duration_secs else None

            # Collect segments if available
            segments = []
            raw_segments = getattr(transcription, "segments", None)
            if raw_segments:
                for seg in raw_segments:
                    start_t = getattr(seg, "start", 0.0)
                    end_t = getattr(seg, "end", 0.0)
                    text_content = getattr(seg, "text", "").strip()
                    m_start, s_start = divmod(int(start_t), 60)
                    m_end, s_end = divmod(int(end_t), 60)
                    timestamp_str = f"[{m_start:02d}:{s_start:02d} - {m_end:02d}:{s_end:02d}]"
                    segments.append(f"{timestamp_str} {text_content}")

            formatted_transcript = "\n".join(segments) if segments else transcript_text

            return jsonify({
                "transcript": formatted_transcript,
                "raw_text": transcript_text,
                "duration_minutes": duration_mins,
                "model": "whisper-large-v3",
                "source_file_name": uploaded_file.filename
            }), 200

        except Exception as e:
            logger.error("Groq Whisper API transcription failed: %s", e, exc_info=True)
            return jsonify({
                "error": f"Audio transcription service failed: {str(e)}"
            }), 502
    else:
        # Offline fallback for local development or testing when Groq API key is not supplied
        logger.info("Operating in offline mode: returning simulated transcription for %s", uploaded_file.filename)
        fallback_transcript = (
            f"[00:00 - 00:30] Speaker 1: Welcome everyone to today's meeting regarding {uploaded_file.filename}.\n"
            f"[00:31 - 01:15] Speaker 2: We have reviewed the deliverables and agreed to deploy the service next week.\n"
            f"[01:16 - 02:00] Speaker 1: Action item for Bob to verify API routes and for Dana to finalize frontend tests."
        )
        return jsonify({
            "transcript": fallback_transcript,
            "raw_text": fallback_transcript,
            "duration_minutes": 2,
            "model": "whisper-large-v3 (simulated-offline)",
            "source_file_name": uploaded_file.filename
        }), 200


@app.errorhandler(404)
def handle_not_found(e):
    return jsonify({
        "error": "Not Found",
        "message": "The requested resource was not found on this server.",
        "status": 404
    }), 404


@app.errorhandler(405)
def handle_method_not_allowed(e):
    return jsonify({
        "error": "Method Not Allowed",
        "message": "The method is not allowed for the requested URL.",
        "status": 405
    }), 405


@app.errorhandler(500)
def handle_internal_server_error(e):
    logger.error("Internal Server Error: %s", e, exc_info=True)
    return jsonify({
        "error": "Internal Server Error",
        "message": "An internal server error occurred while processing the request.",
        "status": 500
    }), 500


if __name__ == "__main__":
    port = int(os.getenv("PORT", os.getenv("FLASK_PORT", 5001)))
    debug = os.getenv("FLASK_DEBUG", "False").lower() in ["true", "1"]
    logger.info("Starting MeetMind AI Multi-Agent Service on port %d...", port)
    app.run(host="0.0.0.0", port=port, debug=debug)

