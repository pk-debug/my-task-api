package com.example.taskapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    private boolean done;

    @Column(nullable = false)
    private String priority = "MEDIUM";

    @Convert(converter = TagListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    @Convert(converter = SubtaskListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Subtask> subtasks = new ArrayList<>();

    public Task() {
    }

    public Task(String title, String description, boolean done, String priority,
                List<String> tags, List<Subtask> subtasks) {
        this.title = title;
        this.description = description;
        this.done = done;
        this.priority = priority == null || priority.isBlank() ? "MEDIUM" : priority;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks == null ? new ArrayList<>() : subtasks;
    }

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

    public static class TagListConverter implements AttributeConverter<List<String>, String> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return "[]";
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to convert tags to JSON", e);
            }
        }

        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to convert tags from JSON", e);
            }
        }
    }

    public static class SubtaskListConverter implements AttributeConverter<List<Subtask>, String> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<Subtask> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return "[]";
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to convert subtasks to JSON", e);
            }
        }

        @Override
        public List<Subtask> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<Subtask>>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to convert subtasks from JSON", e);
            }
        }
    }
}