package com.neuralhealer.backend.activities.quizzes.ipip50.model;

public class Ipip50ScoreResult {
    private String trait;
    private String arabicTrait;
    private int score; // 10-50
    private String level; // LOW, AVERAGE, HIGH
    private String arabicLevel;
    private String description;
    private String arabicDescription;

    // Getters and Setters
    public String getTrait() {
        return trait;
    }

    public void setTrait(String trait) {
        this.trait = trait;
    }

    public String getArabicTrait() {
        return arabicTrait;
    }

    public void setArabicTrait(String arabicTrait) {
        this.arabicTrait = arabicTrait;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getArabicLevel() {
        return arabicLevel;
    }

    public void setArabicLevel(String arabicLevel) {
        this.arabicLevel = arabicLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArabicDescription() {
        return arabicDescription;
    }

    public void setArabicDescription(String arabicDescription) {
        this.arabicDescription = arabicDescription;
    }
}