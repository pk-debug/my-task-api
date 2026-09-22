package com.example.taskapi

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Long = 0L,
    val title: String,
    val done: Boolean = false
)

@Serializable
data class TaskRequest(
    val title: String,
    val done: Boolean = false
)
