package com.studyapp.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyapp.backend.service.AccountEmailSender;
import com.studyapp.backend.service.LegacyDataCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
class StudyApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private LegacyDataCleanup legacyDataCleanup;

    @MockitoBean
    private AccountEmailSender emailSender;

    @Test
    void recordsStudyCreatesFirstReviewAndUpdatesSummary() throws Exception {
        String token = registerAccount();
        String payload = studyPayload(LocalDate.now().minusDays(1), "Java " + UUID.randomUUID());

        mockMvc.perform(post("/api/studies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").isNotEmpty());

        mockMvc.perform(get("/api/reviews").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dueDate").value(LocalDate.now().toString()));
        mockMvc.perform(get("/api/studies/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyStudyHours").isNumber());
    }

    @Test
    void rejectsStudyDatesInFuture() throws Exception {
        String token = registerAccount();
        mockMvc.perform(post("/api/studies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studyPayload(LocalDate.now().plusDays(1), "Java")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void flashcardReviewAdvancesByDifficultyAndDoesNotCreateStudy() throws Exception {
        String token = registerAccount();
        String subject = "Cards " + UUID.randomUUID();
        mockMvc.perform(post("/api/studies")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studyPayload(LocalDate.now().minusDays(1), subject)))
                .andExpect(status().isCreated());

        JsonNode reviews = objectMapper.readTree(mockMvc.perform(
                        get("/api/reviews").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString());
        String reviewId = null;
        for (JsonNode review : reviews) {
            if (subject.equals(review.get("subject").asText())) {
                reviewId = review.get("id").asText();
            }
        }
        assertThat(reviewId).isNotBlank();
        int studiesBefore = objectMapper.readTree(mockMvc.perform(
                        get("/api/studies").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString()).size();

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"EASY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(1))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(7).toString()));

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"HARD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(1))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(7).toString()));

        mockMvc.perform(post("/api/reviews/{id}/flashcards/complete", reviewId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficulty":"AGAIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalIndex").value(0))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(1).toString()));

        mockMvc.perform(post("/api/flashcards/generate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FlashcardRequest(subject, java.util.List.of("Resumo do estudo")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].answer").value("Resumo do estudo"));

        assertThat(objectMapper.readTree(mockMvc.perform(
                get("/api/studies").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString()).size()).isEqualTo(studiesBefore);
    }

    @Test
    void protectsDataAndIsolatesAccounts() throws Exception {
        mockMvc.perform(get("/api/studies")).andExpect(status().isUnauthorized());

        String firstToken = registerAccount();
        String secondToken = registerAccount();
        String subject = "Private " + UUID.randomUUID();

        mockMvc.perform(post("/api/studies")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studyPayload(LocalDate.now(), subject)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/studies")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void registersAndAuthenticatesAccounts() throws Exception {
        String email = UUID.randomUUID() + "@study.test";
        String password = "correct-horse-battery";
        String body = objectMapper.writeValueAsString(new AccountRequest(email, password));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        String code = verificationCode(email);
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CodeRequest(email, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AccountRequest(email, "incorrect-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotatesRefreshTokensAndPasswordResetRevokesPreviousSessions() throws Exception {
        String email = UUID.randomUUID() + "@study.test";
        String password = "correct-horse-battery";
        String session = registerAndVerify(email, password);
        String refreshToken = objectMapper.readTree(session).get("refreshToken").asText();

        String refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nextRefreshToken = objectMapper.readTree(refreshed).get("refreshToken").asText();
        assertThat(nextRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailRequest(email))))
                .andExpect(status().isOk());
        ArgumentCaptor<String> resetCode = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetCode(eq(email), resetCode.capture());
        String newPassword = "new-correct-horse-battery";

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(email, resetCode.getValue(), newPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(nextRefreshToken))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountRequest(email, password))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AccountRequest(email, newPassword))))
                .andExpect(status().isOk());
    }

    @Test
    void limitsVerificationCodeAttemptsAndKeepsAttemptCount() throws Exception {
        String email = UUID.randomUUID() + "@study.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AccountRequest(email, "correct-horse-battery"))))
                .andExpect(status().isCreated());
        String validCode = verificationCode(email);
        String wrongCode = "000000".equals(validCode) ? "000001" : "000000";

        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CodeRequest(email, wrongCode))))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CodeRequest(email, validCode))))
                .andExpect(status().isOk());
    }

    @Test
    void removesUnownedLegacyRowsAndDropsGlobalReviewConstraint() {
        String orphanStudyId = UUID.randomUUID().toString();
        String orphanScheduledId = UUID.randomUUID().toString();
        String orphanReviewId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO study_entries
                (id, user_id, study_date, subject, subject_key, description, duration_minutes, session_count)
                VALUES (?, NULL, ?, 'Legacy', 'legacy', '', 25, 1)
                """, orphanStudyId, LocalDate.now());
        jdbc.update("""
                INSERT INTO scheduled_studies
                (id, user_id, study_date, subject, session_count, study_minutes, break_minutes)
                VALUES (?, NULL, ?, 'Legacy', 1, 25, 5)
                """, orphanScheduledId, LocalDate.now());
        jdbc.update("""
                INSERT INTO review_schedules
                (id, user_id, subject, subject_key, due_date, interval_index)
                VALUES (?, NULL, 'Legacy', 'legacy', ?, 0)
                """, orphanReviewId, LocalDate.now());
        jdbc.execute("""
                ALTER TABLE review_schedules
                ADD CONSTRAINT uq_legacy_review_subject UNIQUE (subject_key)
                """);

        legacyDataCleanup.run(new DefaultApplicationArguments(new String[0]));

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM study_entries WHERE id = ?", Integer.class, orphanStudyId))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM scheduled_studies WHERE id = ?",
                Integer.class, orphanScheduledId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM review_schedules WHERE id = ?",
                Integer.class, orphanReviewId)).isZero();

        String firstOwner = UUID.randomUUID().toString();
        String secondOwner = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO review_schedules
                (id, user_id, subject, subject_key, due_date, interval_index)
                VALUES (?, ?, 'Same subject', 'same subject', ?, 0)
                """, UUID.randomUUID().toString(), firstOwner, LocalDate.now());
        jdbc.update("""
                INSERT INTO review_schedules
                (id, user_id, subject, subject_key, due_date, interval_index)
                VALUES (?, ?, 'Same subject', 'same subject', ?, 0)
                """, UUID.randomUUID().toString(), secondOwner, LocalDate.now());
    }

    @Test
    void syncIsIdempotentAndRejectsStaleVersions() throws Exception {
        String token = registerAccount();
        String changeId = UUID.randomUUID().toString();
        String studyId = UUID.randomUUID().toString();
        String payload = """
                {"cursor":0,"changes":[{
                  "changeId":"%s","entityType":"STUDY","entityId":"%s",
                  "operation":"UPSERT","baseVersion":0,
                  "payload":{"date":"%s","subject":"Sync","description":"Offline",
                    "durationMinutes":25,"sessionCount":1}
                }]}
                """.formatted(changeId, studyId, LocalDate.now());

        mockMvc.perform(post("/api/sync")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0].version").value(1))
                .andExpect(jsonPath("$.changes[0].entityId").value(studyId));

        mockMvc.perform(post("/api/sync")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0].version").value(1));

        String staleChange = """
                {"cursor":1,"changes":[{
                  "changeId":"%s","entityType":"STUDY","entityId":"%s",
                  "operation":"DELETE","baseVersion":0
                }]}
                """.formatted(UUID.randomUUID(), studyId);
        mockMvc.perform(post("/api/sync")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(staleChange))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.serverPayload.subject").value("Sync"));
    }

    private String studyPayload(LocalDate date, String subject) throws Exception {
        return objectMapper.writeValueAsString(
                new StudyRequestBody(date, subject, "Resumo", 25, 1));
    }

    private String registerAccount() throws Exception {
        String email = UUID.randomUUID() + "@study.test";
        return objectMapper.readTree(registerAndVerify(email, "correct-horse-battery"))
                .get("accessToken").asText();
    }

    private String registerAndVerify(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountRequest(email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CodeRequest(email, verificationCode(email)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String verificationCode(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(eq(email), code.capture());
        return code.getValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record StudyRequestBody(
            LocalDate date, String subject, String description, int durationMinutes, int sessionCount) {}

    private record FlashcardRequest(String subject, java.util.List<String> studySummaries) {}

    private record AccountRequest(String email, String password) {}

    private record EmailRequest(String email) {}

    private record CodeRequest(String email, String code) {}

    private record ResetPasswordRequest(String email, String code, String newPassword) {}

    private record RefreshRequest(String refreshToken) {}
}
