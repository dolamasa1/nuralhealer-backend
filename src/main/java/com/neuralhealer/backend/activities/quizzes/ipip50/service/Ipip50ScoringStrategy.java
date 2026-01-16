package com.neuralhealer.backend.activities.quizzes.ipip50.service;

import com.neuralhealer.backend.activities.quizzes.common.JsonLoader;
import com.neuralhealer.backend.activities.quizzes.common.QuizModels;
import com.neuralhealer.backend.activities.quizzes.common.ScoringStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class Ipip50ScoringStrategy implements ScoringStrategy {

    @Autowired
    private JsonLoader jsonLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Map<String, String>>> interpretations = new HashMap<>();

    private static final Map<Integer, String> TRAIT_MAP = Map.of(
            1, "Extraversion",
            2, "Agreeableness",
            3, "Conscientiousness",
            4, "Emotional Stability",
            5, "Openness to Experience");

    private static final Map<String, int[]> PERCENTILE_CUTOFFS = Map.of(
            "Extraversion", new int[] { 28, 38 },
            "Agreeableness", new int[] { 33, 41 },
            "Conscientiousness", new int[] { 31, 40 },
            "Neuroticism", new int[] { 28, 38 },
            "Openness to Experience", new int[] { 30, 39 });

    private static final Map<String, String> TRAIT_ARABIC_MAP = Map.of(
            "Extraversion", "الانبساطية",
            "Agreeableness", "الوداعة (المقبولية)",
            "Conscientiousness", "الضمير الحي (اليقظة)",
            "Neuroticism", "العصابية (القلق)",
            "Openness to Experience", "الانفتاح على التجارب");

    private static final Map<String, String> LEVEL_ARABIC_MAP = Map.of(
            "LOW", "منخفض",
            "AVERAGE", "متوسط",
            "HIGH", "مرتفع");

    @PostConstruct
    public void init() {
        try {
            loadInterpretations();
        } catch (Exception e) {
            createFallbackInterpretations();
        }
    }

    private void loadInterpretations() throws Exception {
        String jsonContent = jsonLoader.loadJsonAsString("quizzes/common/ipip50-interpretations.json");
        JsonNode root = objectMapper.readTree(jsonContent);

        root.fields().forEachRemaining(traitEntry -> {
            String trait = traitEntry.getKey();
            Map<String, Map<String, String>> levels = new HashMap<>();

            traitEntry.getValue().fields().forEachRemaining(levelEntry -> {
                String level = levelEntry.getKey();
                Map<String, String> descriptions = new HashMap<>();
                JsonNode levelNode = levelEntry.getValue();
                if (levelNode.has("User_Friendly"))
                    descriptions.put("en", levelNode.get("User_Friendly").asText());
                if (levelNode.has("Arabic_User_Friendly"))
                    descriptions.put("ar", levelNode.get("Arabic_User_Friendly").asText());
                levels.put(level.toUpperCase(), descriptions);
            });
            interpretations.put(trait, levels);
        });
    }

    private void createFallbackInterpretations() {
        String[] traits = { "Extraversion", "Agreeableness", "Conscientiousness", "Neuroticism",
                "Openness to Experience" };
        String[] levels = { "HIGH", "AVERAGE", "LOW" };
        for (String trait : traits) {
            Map<String, Map<String, String>> traitLevels = new HashMap<>();
            for (String level : levels) {
                Map<String, String> descriptions = new HashMap<>();
                descriptions.put("en", "Fallback for " + trait + " - " + level);
                descriptions.put("ar", "تفسير لـ " + trait + " - " + level);
                traitLevels.put(level, descriptions);
            }
            interpretations.put(trait, traitLevels);
        }
    }

    @Override
    public List<QuizModels.ScoreDetail> calculateScores(List<QuizModels.QuizQuestion> questions,
            List<QuizModels.QuizResponse> responses) {
        Map<Integer, Integer> rawScores = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            rawScores.put(i, 0);

        for (QuizModels.QuizResponse response : responses) {
            QuizModels.QuizQuestion question = questions.stream()
                    .filter(q -> q.getId() == response.getQuestionId())
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException("Invalid question ID: " + response.getQuestionId()));

            int factor = (int) question.getMeta("factor");
            String keying = (String) question.getMeta("keying");
            int score = response.getScore();
            int pointValue = "+".equals(keying) ? score : 6 - score;
            rawScores.put(factor, rawScores.get(factor) + pointValue);
        }

        List<QuizModels.ScoreDetail> results = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : rawScores.entrySet()) {
            QuizModels.ScoreDetail detail = new QuizModels.ScoreDetail();
            int factor = entry.getKey();
            int score = entry.getValue();
            String traitName = TRAIT_MAP.get(factor);

            if (factor == 4) {
                score = 60 - score;
                traitName = "Neuroticism";
            }

            detail.setTrait(traitName);
            detail.setScore(score);
            detail.setLevel(determineLevel(traitName, score));
            detail.setArabicTrait(TRAIT_ARABIC_MAP.get(traitName));
            detail.setArabicLevel(LEVEL_ARABIC_MAP.get(detail.getLevel()));
            results.add(detail);
        }
        return results;
    }

    private String determineLevel(String trait, int score) {
        int[] cutoffs = PERCENTILE_CUTOFFS.get(trait);
        if (cutoffs == null)
            return "AVERAGE";
        if (score <= cutoffs[0])
            return "LOW";
        if (score <= cutoffs[1])
            return "AVERAGE";
        return "HIGH";
    }

    @Override
    public void addInterpretations(List<QuizModels.ScoreDetail> scores, String language) {
        for (QuizModels.ScoreDetail detail : scores) {
            String trait = detail.getTrait();
            String level = detail.getLevel().toUpperCase();
            Map<String, Map<String, String>> traitInterpretations = interpretations.get(trait);
            if (traitInterpretations != null) {
                Map<String, String> levelInterpretations = traitInterpretations.get(level);
                if (levelInterpretations != null) {
                    String description = levelInterpretations.get(language.toLowerCase());
                    if (description != null) {
                        if ("ar".equalsIgnoreCase(language))
                            detail.setArabicDescription(description);
                        else
                            detail.setDescription(description);
                    }
                }
            }
            if (detail.getDescription() == null && detail.getArabicDescription() == null) {
                if ("ar".equalsIgnoreCase(language))
                    detail.setArabicDescription("لا يوجد تفسير متاح");
                else
                    detail.setDescription("No interpretation available");
            }
        }
    }
}
