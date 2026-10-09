export const SAMPLE_MEETING_NOTES = `Q4 Product Architecture & Sprint Planning Sync
Date: October 9, 2026
Attendees: Sarah Connor (Product Lead), Alex Mercer (Lead Architect), Priya Sharma (Backend Engineer), David Kim (Frontend Engineer)

Discussion & Updates:
- Sarah opened the meeting reviewing our Q4 enterprise milestones. We must finalize the MeetMind AI core services before next quarter's pilot.
- Alex reviewed the database options between relational PostgreSQL and document MongoDB.
Decision: Officially selected MongoDB Community Server as our primary document database for meetings, action items, and follow-up plans.
- Priya pointed out that our Spring Boot backend needs to strictly validate incoming requests and maintain single-ownership of database writes.
Action: Priya will build the Java Spring Boot Maven service with MongoDB repositories and REST endpoints by October 16, 2026.
- Alex proposed integrating a Groq-powered multi-step LLM tool calling agent in Python Flask.
Decision: Approved Python Flask with Groq Llama 3.3 for the AI agent service, keeping the Groq API key strictly inside the Python environment.
- David discussed the user interface specifications.
Action: David will implement the React + Vite frontend with responsive cards, external CSS, file upload, and export actions by October 18, 2026.
- Sarah asked whether we should implement automatic email notifications for assigned action items.
Unresolved Question: Should email notification webhooks be added to the v1 scope or deferred to v2?
- Alex raised that we still need to confirm the hosting environment memory limits for MongoDB Compass access.
Unresolved Question: Does the staging environment require replica sets for local testing?
Action: Sarah will review stakeholder feedback on the action item export format by October 20, 2026.
- Next Sync: Scheduled for October 23, 2026 at 2:00 PM EST.`;
