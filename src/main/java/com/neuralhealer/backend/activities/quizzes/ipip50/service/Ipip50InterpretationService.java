package com.neuralhealer.backend.activities.quizzes.ipip50.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.activities.quizzes.common.JsonLoader;
import com.neuralhealer.backend.activities.quizzes.ipip50.model.Ipip50ScoreResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Ipip50InterpretationService {

    @Autowired
    private JsonLoader jsonLoader;

    private Map<String, Map<String, Map<String, String>>> interpretations = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            loadInterpretations();
            System.out.println("IPIP-50 interpretations loaded successfully. Traits: " + interpretations.keySet());
        } catch (Exception e) {
            System.err.println("Failed to load interpretations: " + e.getMessage());
            // Use fallback interpretations if file not found
            createFallbackInterpretations();
        }
    }

    private void loadInterpretations() throws Exception {
        // Try to load from JSON file
        String jsonContent = jsonLoader.loadJsonAsString(
                "quizzes/ipip50/ipip50-interpretations.json");

        JsonNode root = objectMapper.readTree(jsonContent);

        root.fields().forEachRemaining(traitEntry -> {
            String trait = traitEntry.getKey();
            Map<String, Map<String, String>> levels = new HashMap<>();

            traitEntry.getValue().fields().forEachRemaining(levelEntry -> {
                String level = levelEntry.getKey();
                Map<String, String> descriptions = new HashMap<>();

                JsonNode levelNode = levelEntry.getValue();
                if (levelNode.has("User_Friendly")) {
                    descriptions.put("en", levelNode.get("User_Friendly").asText());
                }
                if (levelNode.has("Arabic_User_Friendly")) {
                    descriptions.put("ar", levelNode.get("Arabic_User_Friendly").asText());
                }

                levels.put(level.toUpperCase(), descriptions);
            });

            interpretations.put(trait, levels);
        });
    }

    private void createFallbackInterpretations() {
        System.out.println("Creating fallback interpretations...");

        // Fallback interpretations in case JSON file is missing
        String[] traits = { "Extraversion", "Agreeableness", "Conscientiousness", "Neuroticism",
                "Openness to Experience" };
        String[] levels = { "HIGH", "AVERAGE", "LOW" };

        for (String trait : traits) {
            Map<String, Map<String, String>> traitLevels = new HashMap<>();

            for (String level : levels) {
                Map<String, String> descriptions = new HashMap<>();
                descriptions.put("en", "This is a fallback interpretation for " + trait + " - " + level);
                descriptions.put("ar", "هذا تفسير احتياطي لـ " + trait + " - " + level);
                traitLevels.put(level, descriptions);
            }

            interpretations.put(trait, traitLevels);
        }
    }

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

    public void addInterpretations(List<Ipip50ScoreResult> scoreResults, String language) {
        for (Ipip50ScoreResult result : scoreResults) {
            String trait = result.getTrait();
            String level = result.getLevel().toUpperCase();

            // Set Arabic names for Trait and Level
            result.setArabicTrait(TRAIT_ARABIC_MAP.getOrDefault(trait, trait));
            result.setArabicLevel(LEVEL_ARABIC_MAP.getOrDefault(level, level));

            Map<String, Map<String, String>> traitInterpretations = interpretations.get(trait);
            if (traitInterpretations != null) {
                Map<String, String> levelInterpretations = traitInterpretations.get(level);
                if (levelInterpretations != null) {
                    String description = levelInterpretations.get(language.toLowerCase());
                    if (description != null) {
                        if ("ar".equalsIgnoreCase(language)) {
                            result.setArabicDescription(description);
                        } else {
                            result.setDescription(description);
                        }
                    }
                }
            }

            // If no interpretation found, set a default message
            if (result.getDescription() == null && result.getArabicDescription() == null) {
                String defaultMsg = "No interpretation available for " + trait + " level: " + level;
                if ("ar".equalsIgnoreCase(language)) {
                    result.setArabicDescription(
                            "لا يوجد تفسير متاح لـ " + result.getArabicTrait() + " مستوى: " + result.getArabicLevel());
                } else {
                    result.setDescription(defaultMsg);
                }
            }
        }
    }
}