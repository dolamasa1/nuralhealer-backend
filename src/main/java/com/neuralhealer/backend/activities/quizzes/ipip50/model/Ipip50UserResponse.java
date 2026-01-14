package com.neuralhealer.backend.activities.quizzes.ipip50.model;

import java.util.List;

public class Ipip50UserResponse {
    private String userId;
    private String sessionId;
    private List<QuestionResponse> responses;

    public static class QuestionResponse {
        private int questionId;
        private int score;  // 1-5

        public QuestionResponse() {}

        public QuestionResponse(int questionId, int score) {
            this.questionId = questionId;
            this.score = score;
        }

        // Getters and setters
        public int getQuestionId() { return questionId; }
        public void setQuestionId(int questionId) { this.questionId = questionId; }

        public int getScore() { return score; }
        public void setScore(int score) {
            if (score < 1 || score > 5) {
                throw new IllegalArgumentException("Score must be between 1 and 5");
            }
            this.score = score;
        }
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public List<QuestionResponse> getResponses() { return responses; }
    public void setResponses(List<QuestionResponse> responses) {
        if (responses.size() != 50) {
            throw new IllegalArgumentException("Exactly 50 responses required");
        }
        this.responses = responses;
    }
}