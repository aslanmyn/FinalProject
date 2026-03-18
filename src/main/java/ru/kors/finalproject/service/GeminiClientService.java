package ru.kors.finalproject.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class GeminiClientService {
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .build();

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${app.ai.locale:ru}")
    private String locale;

    public GeminiReply generate(
            String systemPrompt,
            String contextLabel,
            String context,
            String message,
            double temperature,
            int maxOutputTokens
    ) {
        if (!aiEnabled) {
            throw new IllegalStateException("AI assistant is disabled");
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String prompt = """
                %s:
                %s

                User question:
                %s
                """.formatted(contextLabel, context, message);

        GeminiGenerateRequest request = new GeminiGenerateRequest(
                new GeminiInstruction(List.of(new GeminiPart(systemPrompt))),
                List.of(new GeminiContent("user", List.of(new GeminiPart(prompt)))),
                new GeminiGenerationConfig(temperature, maxOutputTokens)
        );

        GeminiGenerateResponse response = restClient.post()
                .uri("/models/{model}:generateContent", geminiModel)
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiGenerateResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Empty AI response");
        }

        String text = response.candidates().stream()
                .map(GeminiCandidate::content)
                .filter(Objects::nonNull)
                .flatMap(content -> content.parts() != null ? content.parts().stream() : java.util.stream.Stream.empty())
                .map(GeminiPart::text)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AI returned no text"));

        return new GeminiReply(text, geminiModel, Instant.now());
    }

    public String getLocale() {
        return locale;
    }

    public record GeminiReply(String answer, String model, Instant generatedAt) {
    }

    private record GeminiGenerateRequest(
            @JsonProperty("system_instruction") GeminiInstruction systemInstruction,
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiInstruction(List<GeminiPart> parts) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerationConfig(Double temperature, Integer maxOutputTokens) {
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }
}
