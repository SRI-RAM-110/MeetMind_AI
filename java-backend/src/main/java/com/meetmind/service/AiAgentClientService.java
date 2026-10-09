package com.meetmind.service;

import com.meetmind.dto.AiAgentRequest;
import com.meetmind.dto.AiAgentResponse;
import com.meetmind.dto.TranscriptionResponseDto;
import com.meetmind.exception.AiAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiAgentClientService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentClientService.class);

    private final RestTemplate restTemplate;

    @Value("${meetmind.ai-agent.url:http://localhost:5001}")
    private String agentBaseUrl;

    public AiAgentClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AiAgentResponse analyzeMeeting(String meetingNotes) {
        String endpoint = agentBaseUrl + "/agent/analyze";
        log.info("Dispatching meeting analysis request to AI agent endpoint: {}", endpoint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiAgentRequest requestPayload = new AiAgentRequest(meetingNotes);
        HttpEntity<AiAgentRequest> entity = new HttpEntity<>(requestPayload, headers);

        try {
            ResponseEntity<AiAgentResponse> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    AiAgentResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("AI agent returned invalid response status: {}", response.getStatusCode());
                throw new AiAgentException("AI agent returned invalid or empty response.", 502);
            }

            AiAgentResponse result = response.getBody();
            validateAgentResponse(result);
            return result;

        } catch (ResourceAccessException rae) {
            log.error("Failed to connect or timeout calling AI agent service at {}: {}", endpoint, rae.getMessage());
            throw new AiAgentException("AI Agent service is unreachable or timed out. Please ensure the Python service is running on " + agentBaseUrl, rae, 504);
        } catch (HttpClientErrorException hce) {
            log.error("Client error from AI agent ({}): {}", hce.getStatusCode(), hce.getResponseBodyAsString());
            throw new AiAgentException("AI Agent validation error: " + hce.getResponseBodyAsString(), hce.getStatusCode().value());
        } catch (HttpServerErrorException hse) {
            log.error("Server error from AI agent ({}): {}", hse.getStatusCode(), hse.getResponseBodyAsString());
            throw new AiAgentException("AI Agent internal error: " + hse.getResponseBodyAsString(), 502);
        } catch (Exception ex) {
            if (ex instanceof AiAgentException aae) {
                throw aae;
            }
            log.error("Unexpected error calling AI agent service: {}", ex.getMessage(), ex);
            throw new AiAgentException("Failed to analyze meeting with AI agent: " + ex.getMessage(), 500);
        }
    }

    public TranscriptionResponseDto transcribeAudio(org.springframework.web.multipart.MultipartFile audioFile) {
        String endpoint = agentBaseUrl + "/agent/transcribe";
        log.info("Dispatching audio transcription request to AI agent endpoint: {}", endpoint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        try {
            org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio_recording.mp3";
                }
            };
            body.add("file", fileResource);
        } catch (Exception e) {
            log.error("Failed to read audio file bytes: {}", e.getMessage(), e);
            throw new AiAgentException("Could not read uploaded audio file: " + e.getMessage(), 400);
        }

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TranscriptionResponseDto> response = restTemplate.postForEntity(
                    endpoint,
                    requestEntity,
                    TranscriptionResponseDto.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("AI agent transcription returned invalid status: {}", response.getStatusCode());
                throw new AiAgentException("AI agent returned invalid transcription response.", 502);
            }

            return response.getBody();
        } catch (ResourceAccessException rae) {
            log.error("Failed to connect or timeout calling AI agent transcription at {}: {}", endpoint, rae.getMessage());
            throw new AiAgentException("AI Agent transcription service is unreachable. Please ensure the Python service is running.", rae, 504);
        } catch (HttpClientErrorException hce) {
            log.error("Client error from AI agent transcription: {}", hce.getResponseBodyAsString());
            throw new AiAgentException("Audio transcription error: " + hce.getResponseBodyAsString(), hce.getStatusCode().value());
        } catch (HttpServerErrorException hse) {
            log.error("Server error from AI agent transcription: {}", hse.getResponseBodyAsString());
            throw new AiAgentException("AI Agent internal error during transcription: " + hse.getResponseBodyAsString(), 502);
        } catch (Exception ex) {
            if (ex instanceof AiAgentException aae) {
                throw aae;
            }
            log.error("Unexpected error during transcription: {}", ex.getMessage(), ex);
            throw new AiAgentException("Failed to transcribe audio file: " + ex.getMessage(), 500);
        }
    }

    private void validateAgentResponse(AiAgentResponse response) {
        if (response.getSummary() == null || response.getSummary().isBlank()) {
            throw new AiAgentException("AI agent response missing summary field.", 502);
        }
        if (response.getActionItems() == null) {
            throw new AiAgentException("AI agent response missing action_items list.", 502);
        }
        if (response.getFollowUpPlan() == null) {
            throw new AiAgentException("AI agent response missing follow_up_plan object.", 502);
        }
    }
}
