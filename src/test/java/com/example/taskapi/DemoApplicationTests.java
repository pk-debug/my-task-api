package com.example.taskapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateTaskWithNestedComplexData() {
        Task task = new Task(
                "Project kickoff",
                "Prepare sprint plan and backlog",
                false,
                "HIGH",
                List.of("work", "planning"),
                List.of(
                        new Task.Subtask("Define scope", false),
                        new Task.Subtask("Review backlog", true)
                )
        );

        ResponseEntity<Task> response = restTemplate.postForEntity("/tasks", task, Task.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Project kickoff", response.getBody().getTitle());
        assertEquals("HIGH", response.getBody().getPriority());
        assertEquals(2, response.getBody().getSubtasks().size());
        assertEquals("work", response.getBody().getTags().get(0));
    }
}
