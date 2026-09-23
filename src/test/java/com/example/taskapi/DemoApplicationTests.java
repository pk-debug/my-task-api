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
}
