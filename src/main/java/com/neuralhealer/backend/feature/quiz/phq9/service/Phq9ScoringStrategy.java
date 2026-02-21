ackage com.neuralhealer.backend.feature.quiz.phq9.service.Phq9ScoringStrategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuralhealer.backend.feature.quiz.common.JsonLoader;
import com.neuralhealer.backend.feature.quiz.common.QuizModels;
import com.neuralhealer.backend.feature.quiz.common.ScoringStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class Phq9ScoringStrategy implements ScoringStrategy {

    @Autowired
    private JsonLoader jsonLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode interpretationsNode;

    @PostConstruct
    public void init() throws IOException {
        String jsonContent = jsonLoader.loadJsonAsString("quizzes/PHQ-9/phq9-interpretations.json");
        this.interpretationsNode = objectMapper.readTree(jsonContent);
    }

    @Override
    public List<QuizModels.ScoreDetail> calculateScores(List<QuizModels.QuizQuestion> questions,
            List<QuizModels.QuizResponse> responses) {
        int totalScore = responses.stream().mapToInt(QuizModels.QuizResponse::getScore).sum();

        QuizModels.ScoreDetail detail = new QuizModels.ScoreDetail();
        detail.setTrait("Depression Severity");
        detail.setArabicTrait("شدة الاكتئاب");
        detail.setScore(totalScore);

        // Find interpretation based on total score
        if (interpretationsNode != null && interpretationsNode.has("interpretations")) {
            for (JsonNode interp : interpretationsNode.get("interpretations")) {
                JsonNode range = interp.get("range");
                int min = range.get(0).asInt();
                int max = range.get(1).asInt();

                if (totalScore >= min && totalScore <= max) {
                    detail.setLevel(interp.get("severity_en").asText());
                    detail.setArabicLevel(interp.get("severity_ar").asText());
                    detail.setDescription(interp.get("action_en").asText());
                    detail.setArabicDescription(interp.get("action_ar").asText());
                    break;
                }
            }
        }

        // Check for item 9 critical alert
        if (interpretationsNode != null && interpretationsNode.has("critical_alert")) {
            JsonNode alertNode = interpretationsNode.get("critical_alert");
            int itemId = alertNode.get("item_id").asInt();
            int threshold = alertNode.get("threshold").asInt();

            responses.stream()
                    .filter(r -> r.getQuestionId() == itemId && r.getScore() >= threshold)
                    .findFirst()
                    .ifPresent(r -> {
                        // We can append this to description or handle it in metadata if we extend
                        // ScoreDetail
                        // For now, let's just make it part of the summary in controller or add a
                        // specialized field in ScoreDetail if needed.
                        // Actually, I'll add "hasCriticalAlert" to metadata.
                        detail.addMetadata("hasCriticalAlert", true);
                        detail.addMetadata("alertMessageEn", alertNode.get("message_en").asText());
                        detail.addMetadata("alertMessageAr", alertNode.get("message_ar").asText());
                    });
        }

        List<QuizModels.ScoreDetail> results = new ArrayList<>();
        results.add(detail);
        return results;
    }

    @Override
    public void addInterpretations(List<QuizModels.ScoreDetail> scores, String language) {
        // Interpretations are already added during calculation for PHQ-9 as it's a
        // single score
    }
}
