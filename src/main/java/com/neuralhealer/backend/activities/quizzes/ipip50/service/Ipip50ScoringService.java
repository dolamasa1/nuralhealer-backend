package com.neuralhealer.backend.activities.quizzes.ipip50.service;

import com.neuralhealer.backend.activities.quizzes.ipip50.model.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class Ipip50ScoringService {

    private static final Map<Integer, String> TRAIT_MAP = Map.of(
            1, "Extraversion",
            2, "Agreeableness",
            3, "Conscientiousness",
            4, "Emotional Stability", // Will convert to Neuroticism
            5, "Openness to Experience");

    private static final Map<String, int[]> PERCENTILE_CUTOFFS = Map.of(
            "Extraversion", new int[] { 28, 38 }, // low ≤ 28, average ≤ 38, high > 38
            "Agreeableness", new int[] { 33, 41 },
            "Conscientiousness", new int[] { 31, 40 },
            "Neuroticism", new int[] { 28, 38 },
            "Openness to Experience", new int[] { 30, 39 });

    public List<Ipip50ScoreResult> calculateScores(
            List<Ipip50Question> questions,
            List<Ipip50UserResponse.QuestionResponse> responses) {
        // Initialize scores
        Map<Integer, Integer> rawScores = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            rawScores.put(i, 0);
        }

        // Calculate raw scores
        for (Ipip50UserResponse.QuestionResponse response : responses) {
            Ipip50Question question = questions.stream()
                    .filter(q -> q.getId() == response.getQuestionId())
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException("Invalid question ID: " + response.getQuestionId()));

            int pointValue = calculatePointValue(response.getScore(), question.getKeying());
            rawScores.put(question.getFactor(), rawScores.get(question.getFactor()) + pointValue);
        }

        // Convert to result objects
        List<Ipip50ScoreResult> results = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : rawScores.entrySet()) {
            Ipip50ScoreResult result = new Ipip50ScoreResult();
            int factor = entry.getKey();
            int score = entry.getValue();

            String traitName = TRAIT_MAP.get(factor);

            // Convert Emotional Stability to Neuroticism
            if (factor == 4) {
                score = 60 - score; // Convert to Neuroticism
                traitName = "Neuroticism";
            }

            result.setTrait(traitName);
            result.setScore(score);
            result.setLevel(determineLevel(traitName, score));

            results.add(result);
        }

        return results;
    }

    private int calculatePointValue(int userScore, String keying) {
        if ("+".equals(keying)) {
            return userScore; // Direct scoring
        } else if ("-".equals(keying)) {
            return 6 - userScore; // Reverse scoring
        }
        throw new IllegalArgumentException("Invalid keying: " + keying);
    }

    private String determineLevel(String trait, int score) {
        int[] cutoffs = PERCENTILE_CUTOFFS.get(trait);
        if (cutoffs == null) {
            return "AVERAGE";
        }

        if (score <= cutoffs[0]) {
            return "LOW";
        } else if (score <= cutoffs[1]) {
            return "AVERAGE";
        } else {
            return "HIGH";
        }
    }
}