"""
MeetMind AI - Base Agent
Provides foundational execution logic, shared Groq client integration,
bounded retries, timeouts, and structured error handling.
"""

import json
import logging
import time
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)


class AgentExecutionError(Exception):
    """Raised when an agent stage fails during execution or validation."""
    def __init__(self, agent_name: str, message: str, original_exception: Optional[Exception] = None):
        super().__init__(f"Agent '{agent_name}' failed: {message}")
        self.agent_name = agent_name
        self.original_exception = original_exception


class BaseAgent:
    """
    Base class for all specialized agents in MeetMind AI.
    """
    def __init__(self, name: str, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        self.name = name
        self.client = client
        self.model = model

    def _call_llm(
        self,
        system_prompt: str,
        user_content: str,
        timeout: int = 40,
        retries: int = 2
    ) -> Dict[str, Any]:
        """
        Executes a real Groq API call requiring JSON output with bounded retries and timeout.
        """
        if not self.client:
            raise AgentExecutionError(self.name, "Groq client is not initialized.")

        last_error = None
        for attempt in range(1, retries + 2):
            try:
                logger.info("[%s] Calling Groq LLM (attempt %d/%d, model: %s)...", self.name, attempt, retries + 1, self.model)
                start_t = time.time()
                
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_content}
                    ],
                    response_format={"type": "json_object"},
                    temperature=0.1,
                    timeout=timeout
                )
                
                elapsed = time.time() - start_t
                logger.info("[%s] Groq response received in %.2fs", self.name, elapsed)

                content = response.choices[0].message.content
                if not content or not content.strip():
                    raise ValueError("Received empty content from Groq model")

                parsed = json.loads(content)
                if not isinstance(parsed, dict):
                    raise ValueError("LLM response did not parse into a JSON object dictionary")

                return parsed

            except json.JSONDecodeError as jde:
                logger.warning("[%s] JSON decode error on attempt %d: %s", self.name, attempt, jde)
                last_error = jde
            except Exception as e:
                err_type = type(e).__name__
                logger.warning("[%s] LLM call error (%s) on attempt %d: %s", self.name, err_type, attempt, e)
                last_error = e
                # Check for non-retryable authentication errors
                if "invalid_api_key" in str(e).lower() or "authenticationerror" in err_type.lower():
                    raise AgentExecutionError(self.name, f"Authentication failed with Groq API: {e}", e)
                
                # Backoff before retrying transient errors
                if attempt <= retries:
                    time.sleep(0.5 * attempt)

        raise AgentExecutionError(self.name, f"Exhausted {retries + 1} attempts. Last error: {last_error}", last_error)
