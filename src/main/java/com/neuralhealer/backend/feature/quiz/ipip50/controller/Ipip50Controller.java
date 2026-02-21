package com.neuralhealer.backend.feature.quiz.ipip50.controller;

import com.neuralhealer.backend.feature.quiz.common.JsonLoader;
import com.neuralhealer.backend.feature.quiz.common.QuizModels;
import com.neuralhealer.backend.feature.quiz.common.QuizService;
import com.neuralhealer.backend.feature.quiz.ipip50.service.Ipip50ScoringStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/quizzes/ipip50")
public class Ipip50Controller {

    @Autowired
    private JsonLoader jsonLoader;

    @Autowired
    private QuizService quizService;

    @Autowired
    private Ipip50ScoringStrategy scoringStrategy;

    private List<QuizModels.QuizQuestion> questions;
    private static final int TOTAL_QUESTIONS = 50;

    @PostConstruct
    public void init() throws IOException {
        QuestionsData questionsData = jsonLoader.loadJson(
                "quizzes/ipip50/ipip50-questions.json",
                QuestionsData.class);
        this.questions = questionsData.getItems();
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            HttpServletResponse response,
            @RequestParam(required = false) String userId) {

        String sessionId = quizService.createSession();

        Cookie sessionCookie = new Cookie("quiz_session", sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/api/quizzes/ipip50");
        sessionCookie.setMaxAge(3600);
        response.addCookie(sessionCookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sessionId", sessionId);
        responseData.put("userId", userId);
        responseData.put("message", "Quiz session started");
        responseData.put("totalQuestions", TOTAL_QUESTIONS);
        responseData.put("remainingQuestions", TOTAL_QUESTIONS);

        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @RequestParam(required = false) Integer questionId,
            HttpServletRequest request) {
        String sessionId = getSessionId(request);

        if (questionId != null) {
            if (questionId < 1 || questionId > TOTAL_QUESTIONS) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error",
                                "Question ID must be between 1 and " + TOTAL_QUESTIONS));
            }

            QuizModels.QuizQuestion question = questions.stream()
                    .filter(q -> q.getId() == questionId)
                    .findFirst()
                    .orElse(null);

            if (question == null)
                return ResponseEntity.notFound().build();

            Integer savedResponse = (sessionId != null) ? quizService.getResponse(sessionId, questionId) : null;

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("question", question);
            responseData.put("savedResponse", savedResponse);
            responseData.put("totalQuestions", TOTAL_QUESTIONS);
            responseData.put("completedQuestions", (sessionId != null) ? quizService.getResponseCount(sessionId) : 0);

            return ResponseEntity.ok(responseData);
        }

        return ResponseEntity.ok(questions);
    }

    @PostMapping("/submit-question")
    public ResponseEntity<Map<String, Object>> submitQuestion(
            @RequestBody QuizModels.QuizResponse submission,
            HttpServletRequest httpRequest) {
        String sessionId = getSessionId(httpRequest);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        try {
            quizService.saveResponse(sessionId, submission.getQuestionId(), submission.getScore(), TOTAL_QUESTIONS, 1,
                    5);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("questionId", submission.getQuestionId());
            responseData.put("savedScore", submission.getScore());
            responseData.put("completedQuestions", quizService.getResponseCount(sessionId));
            responseData.put("totalQuestions", TOTAL_QUESTIONS);

            boolean isDone = quizService.hasAllResponses(sessionId, TOTAL_QUESTIONS);
            responseData.put("allQuestionsCompleted", isDone);
            responseData.put("message",
                    isDone ? "All questions completed! You can now submit the quiz." : "Response saved successfully");

            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("completedQuestions", 0);
            progressData.put("totalQuestions", TOTAL_QUESTIONS);
            progressData.put("percentage", 0);
            progressData.put("isCompleted", false);
            return ResponseEntity.ok(progressData);
        }

        int completed = quizService.getResponseCount(sessionId);
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("sessionId", sessionId);
        progressData.put("completedQuestions", completed);
        progressData.put("totalQuestions", TOTAL_QUESTIONS);
        progressData.put("percentage", (completed * 100) / TOTAL_QUESTIONS);
        progressData.put("isCompleted", quizService.hasAllResponses(sessionId, TOTAL_QUESTIONS));

        return ResponseEntity.ok(progressData);
    }

    @GetMapping("/responses")
    public ResponseEntity<?> getSavedResponses(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.ok(Collections.singletonMap("responses", Collections.emptyList()));
        }

        Map<Integer, Integer> responses = quizService.getAllResponses(sessionId);
        List<Map<String, Object>> detailedResponses = new ArrayList<>();

        responses.forEach((id, score) -> {
            questions.stream().filter(q -> q.getId() == id).findFirst().ifPresent(q -> {
                Map<String, Object> detailed = new HashMap<>();
                detailed.put("questionId", id);
                detailed.put("questionText", q.getText());
                detailed.put("score", score);
                detailed.putAll(q.getMetadata());
                detailedResponses.add(detailed);
            });
        });

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("totalResponses", responses.size());
        responseData.put("responses", detailedResponses);

        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/submit-quiz")
    public ResponseEntity<?> submitQuiz(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        if (!quizService.hasAllResponses(sessionId, TOTAL_QUESTIONS)) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Please answer all " + TOTAL_QUESTIONS + " questions before submitting.");
            errorBody.put("missingQuestions", quizService.getMissingQuestionIds(sessionId, TOTAL_QUESTIONS));
            return ResponseEntity.badRequest().body(errorBody);
        }

        try {
            List<QuizModels.QuizResponse> responseList = quizService.getResponsesAsList(sessionId);
            List<QuizModels.ScoreDetail> scores = scoringStrategy.calculateScores(questions, responseList);
            scoringStrategy.addInterpretations(scores, language);

            QuizModels.QuizResult result = new QuizModels.QuizResult();
            result.setUserId(userId != null ? userId : "guest-" + sessionId);
            result.setScores(scores);
            result.setSummary("IPIP-50 Personality Assessment Completed");
            result.setArabicSummary("تم اكتمال تقييم الشخصية (IPIP-50)");

            quizService.setCompleted(sessionId, true);

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("result", result);
            finalResponse.put("sessionId", sessionId);
            finalResponse.put("completionDate", new Date());
            finalResponse.put("totalScore", scores.stream().mapToInt(QuizModels.ScoreDetail::getScore).sum());

            return ResponseEntity.ok(finalResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to process quiz: " + e.getMessage()));
        }
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetQuiz(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session"));
        }

        quizService.clearSession(sessionId);
        Cookie cookie = new Cookie("quiz_session", "");
        cookie.setPath("/api/quizzes/ipip50");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Quiz session reset successfully");

        return ResponseEntity.ok(responseData);
    }

    private String getSessionId(HttpServletRequest request) {
        String headerId = request.getHeader("X-Quiz-Session");
        if (headerId != null && quizService.isValidSession(headerId))
            return headerId;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("quiz_session".equals(cookie.getName()) && quizService.isValidSession(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class QuestionsData {
        private List<QuizModels.QuizQuestion> items;

        public List<QuizModels.QuizQuestion> getItems() {
            return items;
        }

        public void setItems(List<QuizModels.QuizQuestion> items) {
            this.items = items;
        }
    }
}
