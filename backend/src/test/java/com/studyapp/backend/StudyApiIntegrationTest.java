package com.studyapp.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudyApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recordsStudyCreatesFirstReviewAndUpdatesSummary() throws Exception {
        String payload = studyPayload(LocalDate.now().minusDays(1), "Java " + UUID.randomUUID());

        mockMvc.perform(post("/api/studies")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").isNotEmpty());

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dueDate").value(LocalDate.now().toString()));
        mockMvc.perform(get("/api/studies/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyStudyHours").isNumber());
    }

    @Test
    void rejectsStudyDatesInFuture() throws Exception {
        mockMvc.perform(post("/api/studies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studyPayload(LocalDate.now().plusDays(1), "Java")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void flashcardReviewAdvancesByDifficultyAndDoesNotCreateStudy() throws Exception {
        String subject = "Cards " + UUID.randomUUID();
        mockMvc.perform(post("/api/studies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studyPayload(LocalDate.now().minusDays(1), subject)))
                .andExpect(status().isCreated());

        JsonNode reviews = objectMapper.readTree(mockMvc.perform(get("/api/reviews"))
                .andReturn().getResponse().getContentAsString());
        String reviewId = null;
        for (JsonNode review : reviews) {
            if (subject.equals(review.get("subject").asText())) {
                reviewId = review.get("id").asText();
            }
        }
        assertThat(reviewId).isNotBlank();
        int studiesBefore = objectMapper.readTree(mockMvc.perform(get("/api/studies"))
                .andReturn().getResponse().getContentAsString()).size();

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"EASY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(1))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(7).toString()));

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"HARD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(1))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(7).toString()));

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"AGAIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(0))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(1).toString()));

        mockMvc.perform(post("/api/flashcards/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FlashcardRequest(subject, java.util.List.of("Resumo do estudo")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].answer").value("Resumo do estudo"));

        assertThat(objectMapper.readTree(mockMvc.perform(get("/api/studies"))
                .andReturn().getResponse().getContentAsString()).size()).isEqualTo(studiesBefore);
    }

    private String studyPayload(LocalDate date, String subject) throws Exception {
        return objectMapper.writeValueAsString(
                new StudyRequestBody(date, subject, "Resumo", 25, 1));
    }

    private record StudyRequestBody(
            LocalDate date, String subject, String description, int durationMinutes, int sessionCount) {}

    private record FlashcardRequest(String subject, java.util.List<String> studySummaries) {}
}
