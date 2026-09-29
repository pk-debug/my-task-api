package com.example.taskapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.jwt.secret=test-only-secret-which-is-long-enough-for-hmac")
@AutoConfigureMockMvc
class DemoApplicationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

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
        mockMvc.perform(get("/tasks")).andExpect(status().isUnauthorized());

        String firstEmail = "first-" + UUID.randomUUID() + "@example.com";
        JsonNode first = register(firstEmail);
        String firstAccessToken = first.path("accessToken").asText();
        String firstRefreshToken = first.path("refreshToken").asText();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(firstEmail));

        mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + firstAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"private task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerEmail").doesNotExist());

        String secondEmail = "second-" + UUID.randomUUID() + "@example.com";
        JsonNode second = register(secondEmail);
        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + second.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", "a-secure-test-password",
                        "name", "Test User"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
