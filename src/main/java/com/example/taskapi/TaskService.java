package com.example.taskapi;

import java.util.List;
import java.util.Optional;
import com.example.taskapi.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getAll(String ownerEmail) {
        return taskRepository.findAllByOwnerEmail(ownerEmail);
    }

    @Transactional(readOnly = true)
    public Optional<Task> getById(Long id, String ownerEmail) {
        return taskRepository.findByIdAndOwnerEmail(id, ownerEmail);
    }

    @Transactional(readOnly = true)
    public PageResponse<Task> search(String ownerEmail, String query, Task.TaskStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be between 1 and 100");
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(taskRepository.searchOwnedTasks(
                ownerEmail,
                normalizedQuery,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))));
    }

    @Transactional
    public Task create(Task task, String ownerEmail) {
        task.setOwnerEmail(ownerEmail);
        return taskRepository.save(task);
    }

    @Transactional
    public Optional<Task> update(Long id, Task updatedTask, String ownerEmail) {
        return taskRepository.findByIdAndOwnerEmail(id, ownerEmail).map(existingTask -> {
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setAssignee(updatedTask.getAssignee());
            existingTask.setDueDate(updatedTask.getDueDate());
            existingTask.setStatus(updatedTask.getStatus());
            existingTask.setDone(updatedTask.isDone());
            existingTask.setPriority(updatedTask.getPriority());
            existingTask.setLabels(updatedTask.getLabels());
            existingTask.setTags(updatedTask.getTags());
            existingTask.setCategories(updatedTask.getCategories());
            existingTask.setComments(updatedTask.getComments());
            existingTask.setSubtasks(updatedTask.getSubtasks());
            return taskRepository.save(existingTask);
        });
    }

    @Transactional
    public boolean delete(Long id, String ownerEmail) {
        return taskRepository.findByIdAndOwnerEmail(id, ownerEmail).map(task -> {
            taskRepository.delete(task);
            return true;
        }).orElse(false);
    }
}