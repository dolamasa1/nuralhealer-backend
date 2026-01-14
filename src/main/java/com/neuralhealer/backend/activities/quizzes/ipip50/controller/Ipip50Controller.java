package com.neuralhealer.backend.activities.quizzes.ipip50.controller;

import com.neuralhealer.backend.activities.quizzes.common.JsonLoader;
import com.neuralhealer.backend.activities.quizzes.ipip50.model.*;
import com.neuralhealer.backend.activities.quizzes.ipip50.service.Ipip50InterpretationService;
import com.neuralhealer.backend.activities.quizzes.ipip50.service.Ipip50ScoringService;
import com.neuralhealer.backend.activities.quizzes.ipip50.service.QuizSessionService;
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
    private Ipip50ScoringService scoringService;

    @Autowired
    private Ipip50InterpretationService interpretationService;

    @Autowired
    private QuizSessionService quizSessionService;

    private List<Ipip50Question> questions;

    @PostConstruct
    public void init() throws IOException {
        QuestionsData questionsData = jsonLoader.loadJson(
                "quizzes/ipip50/ipip50-questions.json",
                QuestionsData.class);
        this.questions = questionsData.getItems();
    }

    /**
     * Start a new quiz session
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            HttpServletResponse response,
            @RequestParam(required = false) String userId) {

        // Create new session in singleton service
        String sessionId = quizSessionService.createSession();

        // Create session cookie
        Cookie sessionCookie = new Cookie("quiz_session", sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/api/quizzes/ipip50");
        sessionCookie.setMaxAge(3600); // 1 hour
        response.addCookie(sessionCookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sessionId", sessionId);
        responseData.put("userId", userId);
        responseData.put("message", "Quiz session started");
        responseData.put("totalQuestions", 50);
        responseData.put("remainingQuestions", 50);

        return ResponseEntity.ok(responseData);
    }

    /**
     * Get all questions (or a specific question)
     */
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @RequestParam(required = false) Integer questionId,
            HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        if (questionId != null) {
            if (questionId < 1 || questionId > 50) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "Question ID must be between 1 and 50"));
            }

            Ipip50Question question = questions.stream()
                    .filter(q -> q.getId() == questionId)
                    .findFirst()
                    .orElse(null);

            if (question == null) {
                return ResponseEntity.notFound().build();
            }

            // Get saved response if exists
            Integer savedResponse = quizSessionService.getResponse(sessionId, questionId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("question", question);
            responseData.put("savedResponse", savedResponse);
            responseData.put("totalQuestions", 50);
            responseData.put("completedQuestions", quizSessionService.getResponseCount(sessionId));

            return ResponseEntity.ok(responseData);
        }

        // Return all questions
        return ResponseEntity.ok(questions);
    }

    /**
     * Submit response for a single question
     */
    @PostMapping("/submit-question")
    public ResponseEntity<Map<String, Object>> submitQuestion(
            @RequestBody QuestionSubmissionRequest request,
            HttpServletRequest httpRequest) {
        String sessionId = getSessionId(httpRequest);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        // Validate input
        if (request.getQuestionId() < 1 || request.getQuestionId() > 50) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Question ID must be between 1 and 50"));
        }
        if (request.getScore() < 1 || request.getScore() > 5) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Score must be between 1 and 5"));
        }

        try {
            // Save response to session
            quizSessionService.saveResponse(sessionId, request.getQuestionId(), request.getScore());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("questionId", request.getQuestionId());
            responseData.put("savedScore", request.getScore());
            responseData.put("completedQuestions", quizSessionService.getResponseCount(sessionId));
            responseData.put("totalQuestions", 50);

            // Check if all questions are answered
            if (quizSessionService.hasAllResponses(sessionId)) {
                responseData.put("allQuestionsCompleted", true);
                responseData.put("message", "All questions completed! You can now submit the quiz.");
            } else {
                responseData.put("allQuestionsCompleted", false);
                responseData.put("message", "Response saved successfully");
            }

            return ResponseEntity.ok(responseData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get current quiz progress
     */
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        Map<String, Object> progressData = new HashMap<>();
        progressData.put("sessionId", sessionId);
        progressData.put("completedQuestions", quizSessionService.getResponseCount(sessionId));
        progressData.put("totalQuestions", 50);
        progressData.put("percentage", (quizSessionService.getResponseCount(sessionId) * 100) / 50);
        progressData.put("isCompleted", quizSessionService.hasAllResponses(sessionId));

        return ResponseEntity.ok(progressData);
    }

    /**
     * Get saved responses for review
     */
    @GetMapping("/responses")
    public ResponseEntity<?> getSavedResponses(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session. Please start the quiz first."));
        }

        Map<Integer, Integer> responses = quizSessionService.getAllResponses(sessionId);

        // Map question IDs to question text for better UX
        List<Map<String, Object>> detailedResponses = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : responses.entrySet()) {
            int questionId = entry.getKey();
            int score = entry.getValue();

            Ipip50Question question = questions.stream()
                    .filter(q -> q.getId() == questionId)
                    .findFirst()
                    .orElse(null);

            if (question != null) {
                Map<String, Object> detailed = new HashMap<>();
                detailed.put("questionId", questionId);
                detailed.put("questionText", question.getText());
                detailed.put("score", score);
                detailed.put("factor", question.getFactor());
                detailed.put("keying", question.getKeying());
                detailedResponses.add(detailed);
            }
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("totalResponses", responses.size());
        responseData.put("responses", detailedResponses);

        return ResponseEntity.ok(responseData);
    }

    /**
     * Submit the completed quiz and get results
     */
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

        // Check if all questions are answered
        if (!quizSessionService.hasAllResponses(sessionId)) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error",
                            "All 50 questions must be answered. You have answered " +
                                    quizSessionService.getResponseCount(sessionId) + " questions."));
        }

        try {
            // Convert session responses to QuestionResponse objects
            Map<Integer, Integer> responses = quizSessionService.getAllResponses(sessionId);
            List<Ipip50UserResponse.QuestionResponse> responseList = new ArrayList<>();

            for (int i = 1; i <= 50; i++) {
                if (!responses.containsKey(i)) {
                    throw new IllegalStateException("Missing response for question " + i);
                }

                Ipip50UserResponse.QuestionResponse qr = new Ipip50UserResponse.QuestionResponse();
                qr.setQuestionId(i);
                qr.setScore(responses.get(i));
                responseList.add(qr);
            }

            // Calculate scores
            List<Ipip50ScoreResult> scores = scoringService.calculateScores(questions, responseList);

            // Add interpretations
            interpretationService.addInterpretations(scores, language);

            // Create final result
            Ipip50AssessmentResult result = new Ipip50AssessmentResult();
            result.setUserId(userId != null ? userId : "guest-" + sessionId);
            result.setScores(scores);
            result.setSummary("IPIP-50 Personality Assessment Completed");

            // Mark session as completed
            quizSessionService.setCompleted(sessionId, true);

            // Prepare response with additional metadata
            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("result", result);
            finalResponse.put("sessionId", sessionId);
            finalResponse.put("completionDate", new Date());
            finalResponse.put("totalScore", scores.stream().mapToInt(Ipip50ScoreResult::getScore).sum());

            return ResponseEntity.ok(finalResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to process quiz: " + e.getMessage()));
        }
    }

    /**
     * Reset the current quiz session
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetQuiz(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session"));
        }

        quizSessionService.clearSession(sessionId);

        // Clear the cookie
        Cookie cookie = new Cookie("quiz_session", "");
        cookie.setPath("/api/quizzes/ipip50");
        cookie.setMaxAge(0); // Delete cookie
        response.addCookie(cookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Quiz session reset successfully");

        return ResponseEntity.ok(responseData);
    }

    /**
     * Helper method to extract session ID from cookies or header
     */
    private String getSessionId(HttpServletRequest request) {
        // 1. Try Header (best for file:// or cross-origin testing)
        String headerId = request.getHeader("X-Quiz-Session");
        if (headerId != null && quizSessionService.isValidSession(headerId)) {
            return headerId;
        }

        // 2. Try Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("quiz_session".equals(cookie.getName())) {
                    if (quizSessionService.isValidSession(cookie.getValue())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }

    // Request models
    public static class QuestionSubmissionRequest {
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

    // Helper class to parse JSON
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class QuestionsData {
        private List<Ipip50Question> items;

        public List<Ipip50Question> getItems() {
            return items;
        }

        public void setItems(List<Ipip50Question> items) {
            this.items = items;
        }
    }
}