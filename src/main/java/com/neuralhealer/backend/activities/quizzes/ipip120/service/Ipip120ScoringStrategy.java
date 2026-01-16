package com.neuralhealer.backend.activities.quizzes.ipip120.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.activities.quizzes.common.JsonLoader;
import com.neuralhealer.backend.activities.quizzes.common.QuizModels;
import com.neuralhealer.backend.activities.quizzes.common.ScoringStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Ipip120ScoringStrategy implements ScoringStrategy {

    @Autowired
    private JsonLoader jsonLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Map<String, String>>> facetInterpretations = new HashMap<>();

    private static final String[] DOMAINS = {
            "Neuroticism", "Extraversion", "Openness to Experience", "Agreeableness", "Conscientiousness"
    };

    private static final Map<String, String> DOMAIN_ARABIC_MAP = Map.of(
            "Neuroticism", "العصابية (القلق)",
            "Extraversion", "الانبساطية",
            "Openness to Experience", "الانفتاح على التجارب",
            "Agreeableness", "الوداعة (المقبولية)",
            "Conscientiousness", "الضمير الحي (اليقظة)");

    private static final String[] FACET_NAMES = {
            "Anxiety", "Friendliness", "Imagination", "Trust", "Self-Efficacy",
            "Anger", "Gregariousness", "Artistic Interests", "Morality", "Orderliness",
            "Depression", "Assertiveness", "Emotionality", "Altruism", "Dutifulness",
            "Self-Consciousness", "Activity Level", "Adventurousness", "Cooperation", "Achievement-Striving",
            "Immoderation", "Excitement-Seeking", "Intellect", "Modesty", "Self-Discipline",
            "Vulnerability", "Cheerfulness", "Liberalism", "Sympathy", "Cautiousness"
    };

    static {
        // We'll populate this from the JSON if needed, but for now, we can define a
        // fallback or wait for JSON
    }

    @PostConstruct
    public void init() {
        try {
            loadInterpretations();
        } catch (Exception e) {
            System.err.println("Failed to load IPIP-120 interpretations: " + e.getMessage());
        }
    }

    private void loadInterpretations() throws Exception {
        String jsonContent = jsonLoader.loadJsonAsString("quizzes/ipip120/facets.json");
        JsonNode root = objectMapper.readTree(jsonContent);

        root.fields().forEachRemaining(domainEntry -> {
            domainEntry.getValue().fields().forEachRemaining(facetEntry -> {
                String facetName = facetEntry.getKey();
                Map<String, Map<String, String>> levels = new HashMap<>();

                facetEntry.getValue().fields().forEachRemaining(langEntry -> {
                    String lang = langEntry.getKey();
                    Map<String, String> descByLevel = new HashMap<>();
                    langEntry.getValue().fields().forEachRemaining(levelVal -> {
                        descByLevel.put(levelVal.getKey().toUpperCase(), levelVal.getValue().asText());
                    });
                    levels.put(lang, descByLevel);
                });
                facetInterpretations.put(facetName, levels);
            });
        });
    }

    @Override
    public List<QuizModels.ScoreDetail> calculateScores(List<QuizModels.QuizQuestion> questions,
            List<QuizModels.QuizResponse> responses) {
        // 30 Facets, 4 items each
        int[] facetScores = new int[30];
        for (QuizModels.QuizResponse resp : responses) {
            int id = resp.getQuestionId();
            int score = resp.getScore();

            // This assumes the questions.json contains keying metadata
            QuizModels.QuizQuestion q = questions.stream()
                    .filter(question -> question.getId() == id)
                    .findFirst()
                    .orElse(null);

            if (q != null) {
                String keying = (String) q.getMeta("keying");
                int pointValue = "-".equals(keying) ? (6 - score) : score;
                int facetIdx = (id - 1) % 30;
                facetScores[facetIdx] += pointValue;
            }
        }

        List<QuizModels.ScoreDetail> domainResults = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            QuizModels.ScoreDetail domainDetail = new QuizModels.ScoreDetail();
            domainDetail.setTrait(DOMAINS[d]);
            domainDetail.setArabicTrait(DOMAIN_ARABIC_MAP.get(DOMAINS[d]));

            int domainScore = 0;
            List<QuizModels.ScoreDetail> facets = new ArrayList<>();

            for (int f = 0; f < 6; f++) {
                int facetIdx = d + (f * 5);
                int score = facetScores[facetIdx];
                domainScore += score;

                QuizModels.ScoreDetail facetDetail = new QuizModels.ScoreDetail();
                facetDetail.setTrait(FACET_NAMES[facetIdx]);
                facetDetail.setScore(score);
                facetDetail.setLevel(determineLevel(score, 4, 20)); // Facet range 4-20
                facets.add(facetDetail);
            }

            domainDetail.setScore(domainScore);
            domainDetail.setLevel(determineLevel(domainScore, 24, 120)); // Domain range 24-120
            domainDetail.setFacets(facets);
            domainResults.add(domainDetail);
        }

        return domainResults;
    }

    private String determineLevel(int score, int min, int max) {
        double percent = (double) (score - min) / (max - min);
        if (percent < 0.33)
            return "LOW";
        if (percent < 0.66)
            return "AVERAGE";
        return "HIGH";
    }

    @Override
    public void addInterpretations(List<QuizModels.ScoreDetail> scores, String language) {
        String lang = language.toLowerCase();
        for (QuizModels.ScoreDetail domain : scores) {
            // Domain level summary could be added here if needed

            if (domain.getFacets() != null) {
                for (QuizModels.ScoreDetail facet : domain.getFacets()) {
                    String trait = facet.getTrait();
                    String level = facet.getLevel();

                    Map<String, Map<String, String>> traitData = facetInterpretations.get(trait);
                    if (traitData != null) {
                        Map<String, String> langData = traitData.get(lang);
                        if (langData != null) {
                            String desc = langData.get(level);
                            if (desc != null) {
                                if ("ar".equals(lang))
                                    facet.setArabicDescription(desc);
                                else
                                    facet.setDescription(desc);
                            }
                        }
                    }

                    if ("ar".equals(lang)) {
                        facet.setArabicLevel(translateLevel(level));
                        // Ideally we'd have an Arabic facet name map too
                    }
                }
            }
        }
    }

    private String translateLevel(String level) {
        return switch (level) {
            case "LOW" -> "منخفض";
            case "AVERAGE" -> "متوسط";
            case "HIGH" -> "مرتفع";
            default -> level;
        };
    }
}
