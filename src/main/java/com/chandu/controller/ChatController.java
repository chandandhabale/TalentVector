package com.chandu.controller;

import org.apache.tika.Tika;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final Tika tika = new Tika();

    public ChatController(ChatClient chatClient,
                          QuestionAnswerAdvisor questionAnswerAdvisor) {
        this.chatClient = chatClient;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
    }

    /**
     * Pure AI Chat (no PDFs / no RAG)
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");

        if (isEmpty(message)) {
            return badRequest("Message cannot be empty");
        }

        try {
            String reply = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("response", reply));

        } catch (Exception e) {
            return internalError("Error in /chat: " + e.getMessage());
        }
    }

    /**
     * AI answer for direct GET
     */
    @GetMapping("/ask")
    public ResponseEntity<?> ask(@RequestParam("question") String question) {
        if (isEmpty(question)) {
            return badRequest("Question cannot be empty");
        }

        try {
            String reply = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("response", reply));

        } catch (Exception e) {
            return internalError("Error in /ask: " + e.getMessage());
        }
    }

    /**
     * RAG Chat using Vector Store + Advisor
     */
    @PostMapping("/rag/chat")
    public ResponseEntity<?> ragChat(@RequestBody Map<String, String> body) {
        String message = body.get("message");

        if (isEmpty(message)) {
            return badRequest("Message cannot be empty");
        }

        try {
            String reply = chatClient.prompt()
                    .advisors(questionAnswerAdvisor)
                    .system("""
                            Answer strictly from the PDF context.
                            If the information does not exist in the documents,
                            reply with: "I don't know based on the available documents."
                            """)
                    .user(message)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("response", reply));

        } catch (Exception e) {
            return internalError("Error in /rag/chat: " + e.getMessage());
        }
    }

    /**
     * Basic Health Endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "AI Chatbot"));
    }

    /**
     * Resume Analyzer Endpoint
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(@RequestParam("file") MultipartFile file) {
        try {
            String resumeText = tika.parseToString(file.getInputStream());

            String prompt = """
                    Analyze the following resume:

                    %s

                    Return strict JSON with:
                    - skills: extracted skills list
                    - rating: score from 1–10
                    - improvements: list of 3 suggestions
                    """.formatted(resumeText);

            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("analysis", result));

        } catch (Exception e) {
            return internalError("Error in /analyze: " + e.getMessage());
        }
    }

    /**
     * ATS Score Analyzer
     */
    @PostMapping("/ats-check")
    public ResponseEntity<?> atsCheck(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jd") String jobDescription) {

        try {
            String resumeText = tika.parseToString(file.getInputStream());

            String prompt = """
                    You are an ATS expert. Compare the resume with the job description.

                    Resume:
                    %s

                    Job Description:
                    %s

                    Return STRICT JSON with:
                    - atsScore (0-100)
                    - matchedKeywords (list)
                    - missingKeywords (list)
                    - summary (short paragraph)
                    """.formatted(resumeText, jobDescription);

            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("atsReport", result));

        } catch (Exception e) {
            return internalError("Error in /ats-check: " + e.getMessage());
        }
    }

    /* --------------------
       Utility Methods
    --------------------- */

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    private ResponseEntity<?> internalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }
}
