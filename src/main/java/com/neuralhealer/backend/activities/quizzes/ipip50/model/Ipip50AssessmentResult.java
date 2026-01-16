package com.neuralhealer.backend.activities.quizzes.ipip50.model;

import java.util.List;

public class Ipip50AssessmentResult {
    private String userId;
    private List<Ipip50ScoreResult> scores;
    private String summary;
    private String arabicSummary;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Ipip50ScoreResult> getScores() {
        return scores;
    }

    public void setScores(List<Ipip50ScoreResult> scores) {
        this.scores = scores;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getArabicSummary() {
        return arabicSummary;
    }

    public void setArabicSummary(String arabicSummary) {
        this.arabicSummary = arabicSummary;
    }
}