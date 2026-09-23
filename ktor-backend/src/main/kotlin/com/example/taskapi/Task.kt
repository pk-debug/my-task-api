package com.example.taskapi

import kotlinx.serialization.Serializable

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    BLOCKED,
    DONE
}

@Serializable
data class Subtask(
    val title: String,
    val done: Boolean = false
)

@Serializable
data class Comment(
    val author: String = "",
    val message: String = "",
    val createdAt: String = ""
)

@Serializable
data class Category(
    val name: String = "",
    val color: String = "#808080"
)

@Serializable
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val assignee: String = "",
    val dueDate: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val done: Boolean = false,
    val priority: String = "MEDIUM",
    val labels: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)

@Serializable
data class TaskRequest(
    val title: String,
    val description: String = "",
    val assignee: String = "",
    val dueDate: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val done: Boolean = false,
    val priority: String = "MEDIUM",
    val labels: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)
