package com.neuralhealer.backend.activities.quizzes.common;

import java.util.List;

public interface ScoringStrategy {
    List<QuizModels.ScoreDetail> calculateScores(List<QuizModels.QuizQuestion> questions,
            List<QuizModels.QuizResponse> responses);

    void addInterpretations(List<QuizModels.ScoreDetail> scores, String language);
}
