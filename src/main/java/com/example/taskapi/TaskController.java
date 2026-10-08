package com.example.taskapi;

import jakarta.validation.Valid;
import java.util.List;
import com.example.taskapi.api.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks(@AuthenticationPrincipal Jwt jwt) {
        return taskService.getAll(jwt.getClaimAsString("email"));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<Task>> searchTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Task.TaskStatus taskStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                taskStatus = Task.TaskStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(taskService.search(jwt.getClaimAsString("email"), q, taskStatus, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return taskService.getById(id, jwt.getClaimAsString("email"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task, @AuthenticationPrincipal Jwt jwt) {
        if (task.getLabels() == null) {
            task.setLabels(List.of());
        }
        if (task.getTags() == null) {
            task.setTags(List.of());
        }
        if (task.getCategories() == null) {
            task.setCategories(List.of());
        }
        if (task.getComments() == null) {
            task.setComments(List.of());
        }
        if (task.getSubtasks() == null) {
            task.setSubtasks(List.of());
        }
        if (task.getStatus() == null) {
            task.setStatus(Task.TaskStatus.TODO);
        }
        Task savedTask = taskService.create(task, jwt.getClaimAsString("email"));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task updatedTask,
            @AuthenticationPrincipal Jwt jwt) {
        return taskService.update(id, updatedTask, jwt.getClaimAsString("email"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (!taskService.delete(id, jwt.getClaimAsString("email"))) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}