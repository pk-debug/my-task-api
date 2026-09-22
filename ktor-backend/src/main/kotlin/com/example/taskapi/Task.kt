package com.example.taskapi

import kotlinx.serialization.Serializable

@Serializable
data class Subtask(
    val title: String,
    val done: Boolean = false
)

@Serializable
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val priority: String = "MEDIUM",
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)

@Serializable
data class TaskRequest(
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val priority: String = "MEDIUM",
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)
