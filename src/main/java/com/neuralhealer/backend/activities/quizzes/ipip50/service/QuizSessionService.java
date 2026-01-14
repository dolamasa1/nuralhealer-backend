package com.neuralhealer.backend.activities.quizzes.ipip50.service;

import com.neuralhealer.backend.activities.quizzes.ipip50.model.Ipip50UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuizSessionService {

    private static class SessionData {
        private String sessionId;
        private Map<Integer, Integer> userResponses = new HashMap<>();
        private boolean isCompleted = false;

        public SessionData(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Map<Integer, Integer> getUserResponses() {
            return userResponses;
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public void setCompleted(boolean completed) {
            isCompleted = completed;
        }
    }

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new SessionData(sessionId));
        return sessionId;
    }

    public void saveResponse(String sessionId, int questionId, int score) {
        SessionData session = getSession(sessionId);
        if (questionId < 1 || questionId > 50) {
            throw new IllegalArgumentException("Question ID must be between 1 and 50");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
        session.getUserResponses().put(questionId, score);
    }

    public Integer getResponse(String sessionId, int questionId) {
        return getSession(sessionId).getUserResponses().get(questionId);
    }

    public Map<Integer, Integer> getAllResponses(String sessionId) {
        return new HashMap<>(getSession(sessionId).getUserResponses());
    }

    public int getResponseCount(String sessionId) {
        return getSession(sessionId).getUserResponses().size();
    }

    public boolean isCompleted(String sessionId) {
        return getSession(sessionId).isCompleted();
    }

    public void setCompleted(String sessionId, boolean completed) {
        getSession(sessionId).setCompleted(completed);
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public boolean hasAllResponses(String sessionId) {
        return getSession(sessionId).getUserResponses().size() == 50;
    }

    private SessionData getSession(String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            throw new IllegalStateException("Invalid or expired session: " + sessionId);
        }
        return sessions.get(sessionId);
    }

    public boolean isValidSession(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }
}