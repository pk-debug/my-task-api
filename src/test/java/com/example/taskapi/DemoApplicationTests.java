package com.example.taskapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import com.example.taskapi.auth.AuthEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-which-is-long-enough-for-hmac",
        "spring.datasource.url=jdbc:sqlite::memory:"
})
@AutoConfigureMockMvc
@Import(DemoApplicationTests.EmailTestConfiguration.class)
class DemoApplicationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private RecordingAuthEmailSender emailSender;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateTaskWithNestedComplexData() {
        Task task = new Task(
                "Project kickoff",
                "Prepare sprint plan and backlog",
                "Asha",
                "2026-10-05",
                Task.TaskStatus.IN_PROGRESS,
                false,
                "HIGH",
                List.of("backend", "planning"),
                List.of("work", "planning"),
                List.of(
                        new Task.Category("Engineering", "#3b82f6"),
                        new Task.Category("Product", "#10b981")
                ),
                List.of(
                        new Task.Comment("Priya", "Need final scope", "2026-09-23T09:30:00Z")
                ),
                List.of(
                        new Task.Subtask("Define scope", false),
                        new Task.Subtask("Review backlog", true)
                )
        );

        assertNotNull(task);
        assertEquals("Project kickoff", task.getTitle());
        assertEquals("Asha", task.getAssignee());
        assertEquals(Task.TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals("HIGH", task.getPriority());
        assertEquals(2, task.getSubtasks().size());
        assertEquals("backend", task.getLabels().get(0));
        assertEquals("Engineering", task.getCategories().get(0).getName());
        assertEquals("Need final scope", task.getComments().get(0).getMessage());
    }

    @Test
    void authFlowProtectsAndScopesTasks() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("task-api"));
        mockMvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/tasks/search")).andExpect(status().isUnauthorized());

        String firstEmail = "first-" + UUID.randomUUID() + "@example.com";
        register(firstEmail);
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\",\"password\":\"a-secure-test-password\"}"))
                .andExpect(status().isUnauthorized());

        String firstVerificationToken = emailSender.verificationTokens.get(firstEmail);
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\"}"))
                .andExpect(status().isOk());
        String currentVerificationToken = emailSender.verificationTokens.get(firstEmail);
        assertNotEquals(firstVerificationToken, currentVerificationToken);

        String first = verifyEmail(currentVerificationToken);
        String firstAccessToken = jsonField(first, "accessToken");
        String firstRefreshToken = jsonField(first, "refreshToken");
        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + firstVerificationToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(firstEmail));

        MvcResult taskResult = mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + firstAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"private task\",\"done\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerEmail").doesNotExist())
                .andReturn();
        long taskId = jsonLongField(taskResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + firstAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"planning checklist\",\"done\":false,\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/tasks/search")
                .param("page", "0")
                .param("size", "1")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/tasks/search")
                .param("q", "private")
                .param("status", "TODO")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("private task"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/tasks/search")
                .param("status", "IN_PROGRESS")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("planning checklist"));
        mockMvc.perform(get("/tasks/search")
                .param("size", "101")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/tasks/search")
                .param("page", "-1")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/tasks/search")
                .param("status", "UNKNOWN")
                .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isBadRequest());

        String secondEmail = "second-" + UUID.randomUUID() + "@example.com";
        register(secondEmail);
        String second = verifyEmail(emailSender.verificationTokens.get(secondEmail));
        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + jsonField(second, "accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        mockMvc.perform(get("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + jsonField(second, "accessToken")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/tasks/search")
                .header("Authorization", "Bearer " + jsonField(second, "accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", org.hamcrest.Matchers.is(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + firstRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedRefreshToken = jsonField(refreshResult.getResponse().getContentAsString(), "refreshToken");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + firstRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\",\"password\":\"a-secure-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String activeRefreshToken = jsonField(loginResult.getResponse().getContentAsString(), "refreshToken");

        String forgotResponse = mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String unknownForgotResponse = mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(forgotResponse, unknownForgotResponse);

        String resetToken = emailSender.resetTokens.get(firstEmail);
        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"a-new-secure-password\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + activeRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\",\"password\":\"a-secure-test-password\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + firstEmail + "\",\"password\":\"a-new-secure-password\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"another-secure-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"a-secure-test-password\",\"name\":\"Test User\"}"))
                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Check your email for a verification token"))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

        private String verifyEmail(String token) throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"" + token + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();
                return result.getResponse().getContentAsString();
        }

    private String jsonField(String json, String field) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing JSON field: " + field);
        }
        return matcher.group(1);
    }

    private long jsonLongField(String json, String field) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing JSON field: " + field);
        }
        return Long.parseLong(matcher.group(1));
    }

        @TestConfiguration
        static class EmailTestConfiguration {
                @Bean
                @Primary
                RecordingAuthEmailSender recordingAuthEmailSender() {
                        return new RecordingAuthEmailSender();
                }
        }

        static class RecordingAuthEmailSender implements AuthEmailSender {
                private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();
                private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

                @Override
                public void sendVerification(String email, String token, String endpoint) {
                        verificationTokens.put(email, token);
                }

                @Override
                public void sendPasswordReset(String email, String token, String endpoint) {
                        resetTokens.put(email, token);
                }
        }
}
