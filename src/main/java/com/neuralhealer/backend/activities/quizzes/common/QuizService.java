package com.neuralhealer.backend.activities.quizzes.common;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuizService {

    private static class SessionData {
        private final String sessionId;
        private final Map<Integer, Integer> userResponses = new HashMap<>();
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

    public void saveResponse(String sessionId, int questionId, int score, int totalQuestions, int minScore,
            int maxScore) {
        SessionData session = getSession(sessionId);
        if (questionId < 1 || questionId > totalQuestions) {
            throw new IllegalArgumentException("Question ID must be between 1 and " + totalQuestions);
        }
        if (score < minScore || score > maxScore) {
            throw new IllegalArgumentException("Score must be between " + minScore + " and " + maxScore);
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

    public boolean hasAllResponses(String sessionId, int totalQuestions) {
        return getSession(sessionId).getUserResponses().size() == totalQuestions;
    }

    public boolean isValidSession(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    private SessionData getSession(String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            throw new IllegalStateException("Invalid or expired session: " + sessionId);
        }
        return sessions.get(sessionId);
    }

    public List<QuizModels.QuizResponse> getResponsesAsList(String sessionId) {
        Map<Integer, Integer> responses = getAllResponses(sessionId);
        List<QuizModels.QuizResponse> list = new ArrayList<>();
        responses.forEach((id, score) -> {
            QuizModels.QuizResponse qr = new QuizModels.QuizResponse();
            qr.setQuestionId(id);
            qr.setScore(score);
            list.add(qr);
        });
        return list;
    }
}
