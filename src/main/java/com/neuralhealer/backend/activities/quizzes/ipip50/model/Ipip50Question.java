package com.neuralhealer.backend.activities.quizzes.ipip50.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ipip50Question {
    private int id;

    @JsonProperty("text")
    private String text;

    @JsonProperty("factor")
    private int factor; // 1-5

    @JsonProperty("keying")
    private String keying; // "+" or "-"

    // Getters and Setters
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

    public int getFactor() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor = factor;
    }

    public String getKeying() {
        return keying;
    }

    public void setKeying(String keying) {
        this.keying = keying;
    }
}