package com.example.taskapi;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        BLOCKED,
        DONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    private String assignee = "";

    private String dueDate = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    private boolean done;

    @Column(nullable = false)
    private String priority = "MEDIUM";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_labels", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "label")
    private List<String> labels = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_categories", joinColumns = @JoinColumn(name = "task_id"))
    private List<Category> categories = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_comments", joinColumns = @JoinColumn(name = "task_id"))
    private List<Comment> comments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_subtasks", joinColumns = @JoinColumn(name = "task_id"))
    private List<Subtask> subtasks = new ArrayList<>();

    public Task() {
    }

    public Task(String title, String description, String assignee, String dueDate, TaskStatus status,
                boolean done, String priority, List<String> labels, List<String> tags,
                List<Category> categories, List<Comment> comments, List<Subtask> subtasks) {
        this.title = title;
        this.description = description;
        this.assignee = assignee == null ? "" : assignee;
        this.dueDate = dueDate == null ? "" : dueDate;
        this.status = status == null ? TaskStatus.TODO : status;
        this.done = done;
        this.priority = priority == null || priority.isBlank() ? "MEDIUM" : priority;
        this.labels = labels == null ? new ArrayList<>() : new ArrayList<>(labels);
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
        this.subtasks = subtasks == null ? new ArrayList<>() : new ArrayList<>(subtasks);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels == null ? new ArrayList<>() : labels;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories == null ? new ArrayList<>() : categories;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments == null ? new ArrayList<>() : comments;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks == null ? new ArrayList<>() : subtasks;
    }

    @Embeddable
    public static class Category {
        private String name;
        private String color;

        public Category() {
        }

        public Category(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    @Embeddable
    public static class Comment {
        private String author;
        private String message;
        private String createdAt;

        public Comment() {
        }

        public Comment(String author, String message, String createdAt) {
            this.author = author;
            this.message = message;
            this.createdAt = createdAt;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    @Embeddable
    public static class Subtask {
        private String title;
        private boolean done;

        public Subtask() {
        }

        public Subtask(String title, boolean done) {
            this.title = title;
            this.done = done;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }
}