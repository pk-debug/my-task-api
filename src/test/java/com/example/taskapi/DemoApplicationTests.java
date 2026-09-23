package com.example.taskapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

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

        assertNotNull(task);
        assertEquals("Project kickoff", task.getTitle());
        assertEquals("HIGH", task.getPriority());
        assertEquals(2, task.getSubtasks().size());
        assertEquals("work", task.getTags().get(0));
    }
}
