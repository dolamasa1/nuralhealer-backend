package com.neuralhealer.backend.feature.quiz.phq9.controller;

import com.neuralhealer.backend.feature.quiz.common.JsonLoader;
import com.neuralhealer.backend.feature.quiz.common.QuizModels;
import com.neuralhealer.backend.feature.quiz.common.QuizService;
import com.neuralhealer.backend.feature.quiz.phq9.service.Phq9ScoringStrategy;
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
@RequestMapping("/quizzes/phq9")
public class Phq9Controller {

    @Autowired
    private JsonLoader jsonLoader;

    @Autowired
    private QuizService quizService;

    @Autowired
    private Phq9ScoringStrategy scoringStrategy;

    private List<QuizModels.QuizQuestion> questions;
    private static final int TOTAL_QUESTIONS = 9;

    @PostConstruct
    public void init() throws IOException {
        QuestionsData questionsData = jsonLoader.loadJson(
                "quizzes/PHQ-9/phq9-questions.json",
                QuestionsData.class);
        this.questions = questionsData.getItems();
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            HttpServletResponse response,
            @RequestParam(required = false) String userId) {

        String sessionId = quizService.createSession();

        Cookie sessionCookie = new Cookie("quiz_session_phq", sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/api/quizzes/phq9");
        sessionCookie.setMaxAge(3600);
        response.addCookie(sessionCookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sessionId", sessionId);
        responseData.put("userId", userId);
        responseData.put("message", "PHQ-9 session started");
        responseData.put("totalQuestions", TOTAL_QUESTIONS);

        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions() {
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/submit-question")
    public ResponseEntity<Map<String, Object>> submitQuestion(
            @RequestBody QuizModels.QuizResponse submission,
            HttpServletRequest httpRequest) {
        String sessionId = getSessionId(httpRequest);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session."));
        }

        try {
            quizService.saveResponse(sessionId, submission.getQuestionId(), submission.getScore(), TOTAL_QUESTIONS, 0,
                    3);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("completedQuestions", quizService.getResponseCount(sessionId));
            responseData.put("totalQuestions", TOTAL_QUESTIONS);
            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
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
            Map<String, Object> r = new HashMap<>();
            r.put("questionId", id);
            r.put("score", score);
            detailedResponses.add(r);
        });
        return ResponseEntity.ok(Collections.singletonMap("responses", detailedResponses));
    }

    @PostMapping("/submit-quiz")
    public ResponseEntity<?> submitQuiz(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "No active session."));
        }

        if (!quizService.hasAllResponses(sessionId, TOTAL_QUESTIONS)) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Please answer all " + TOTAL_QUESTIONS + " questions before submitting.");
            errorBody.put("missingQuestions", quizService.getMissingQuestionIds(sessionId, TOTAL_QUESTIONS));
            return ResponseEntity.badRequest().body(errorBody);
        }

        List<QuizModels.QuizResponse> responseList = quizService.getResponsesAsList(sessionId);
        List<QuizModels.ScoreDetail> scores = scoringStrategy.calculateScores(questions, responseList);

        QuizModels.QuizResult result = new QuizModels.QuizResult();
        result.setUserId(userId != null ? userId : "guest-" + sessionId);
        result.setScores(scores);
        result.setSummary("PHQ-9 Depression Screening Completed");
        result.setArabicSummary("تم اكتمال فحص الاكتئاب (PHQ-9)");

        quizService.setCompleted(sessionId, true);

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("result", result);
        finalResponse.put("sessionId", sessionId);
        finalResponse.put("completionDate", new Date());

        return ResponseEntity.ok(finalResponse);
    }

    private String getSessionId(HttpServletRequest request) {
        String headerId = request.getHeader("X-Quiz-Session");
        if (headerId != null && quizService.isValidSession(headerId))
            return headerId;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("quiz_session_phq".equals(cookie.getName()) && quizService.isValidSession(cookie.getValue())) {
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
    }
}
