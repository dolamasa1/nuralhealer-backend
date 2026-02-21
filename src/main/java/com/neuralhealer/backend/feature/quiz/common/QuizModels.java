package com.neuralhealer.backend.feature.quiz.common;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizModels {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuizQuestion {
        private int id;
        private String text;

        @JsonProperty("arabic_text")
        private String arabicText;

        private Map<String, Object> metadata = new HashMap<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getArabicText() {
            return arabicText;
        }

        public void setArabicText(String arabicText) {
            this.arabicText = arabicText;
        }

        @JsonAnySetter
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Object getMeta(String key) {
            return metadata.get(key);
        }
    }

    public static class QuizResponse {
        private int questionId;
        private int score;

        public int getQuestionId() {
            return questionId;
        }

        public void setQuestionId(int questionId) {
            this.questionId = questionId;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class ScoreDetail {
        private String trait;
        private String arabicTrait;
        private int score;
        private String level;
        private String arabicLevel;
        private String description;
        private String arabicDescription;
        private List<ScoreDetail> facets;
        private Map<String, Object> metadata = new HashMap<>();

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

        public List<ScoreDetail> getFacets() {
            return facets;
        }

        public void setFacets(List<ScoreDetail> facets) {
            this.facets = facets;
        }

        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Object getMeta(String key) {
            return metadata.get(key);
        }
    }

    public static class QuizResult {
        private String userId;
        private List<ScoreDetail> scores;
        private String summary;
        private String arabicSummary;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public List<ScoreDetail> getScores() {
            return scores;
        }

        public void setScores(List<ScoreDetail> scores) {
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
}
