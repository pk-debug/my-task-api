package com.example.taskapi

import java.util.concurrent.atomic.AtomicLong

class TaskRepository {
    private val tasks = linkedMapOf<Long, Task>()
    private val nextId = AtomicLong(1L)

    fun getAll(): List<Task> = tasks.values.toList()

    fun getById(id: Long): Task? = tasks[id]

    fun create(taskRequest: TaskRequest): Task {
        val id = nextId.getAndIncrement()
        val task = Task(
            id = id,
            title = taskRequest.title.trim(),
            description = taskRequest.description.trim(),
            done = taskRequest.done,
            priority = taskRequest.priority.ifBlank { "MEDIUM" },
            tags = taskRequest.tags.map { it.trim() }.filter { it.isNotEmpty() },
            subtasks = taskRequest.subtasks.map { it.copy(title = it.title.trim()) }
        )
        tasks[id] = task
        return task
    }

    fun update(id: Long, taskRequest: TaskRequest): Task? {
        val existing = tasks[id] ?: return null
        val updated = existing.copy(
            title = taskRequest.title.trim(),
            description = taskRequest.description.trim(),
            done = taskRequest.done,
            priority = taskRequest.priority.ifBlank { "MEDIUM" },
            tags = taskRequest.tags.map { it.trim() }.filter { it.isNotEmpty() },
            subtasks = taskRequest.subtasks.map { it.copy(title = it.title.trim()) }
        )
        tasks[id] = updated
        return updated
    }

    fun delete(id: Long): Boolean = tasks.remove(id) != null
}
