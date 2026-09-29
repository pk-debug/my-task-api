package com.example.taskapi

import java.util.concurrent.atomic.AtomicLong

class TaskRepository {
    private val tasks = linkedMapOf<Long, Task>()
    private val nextId = AtomicLong(1L)

    fun getAll(ownerId: Long): List<Task> = tasks.values.filter { it.ownerId == ownerId }

    fun getById(id: Long, ownerId: Long): Task? = tasks[id]?.takeIf { it.ownerId == ownerId }

    fun create(taskRequest: TaskRequest, ownerId: Long): Task {
        val id = nextId.getAndIncrement()
        val task = Task(
            id = id,
            ownerId = ownerId,
            title = taskRequest.title.trim(),
            description = taskRequest.description.trim(),
            assignee = taskRequest.assignee.trim(),
            dueDate = taskRequest.dueDate.trim(),
            status = taskRequest.status,
            done = taskRequest.done,
            priority = taskRequest.priority.ifBlank { "MEDIUM" },
            labels = taskRequest.labels.map { it.trim() }.filter { it.isNotEmpty() },
            tags = taskRequest.tags.map { it.trim() }.filter { it.isNotEmpty() },
            categories = taskRequest.categories.map { it.copy(name = it.name.trim()) },
            comments = taskRequest.comments.map { it.copy(message = it.message.trim()) },
            subtasks = taskRequest.subtasks.map { it.copy(title = it.title.trim()) }
        )
        tasks[id] = task
        return task
    }

    fun update(id: Long, taskRequest: TaskRequest, ownerId: Long): Task? {
        val existing = getById(id, ownerId) ?: return null
        val updated = existing.copy(
            title = taskRequest.title.trim(),
            description = taskRequest.description.trim(),
            assignee = taskRequest.assignee.trim(),
            dueDate = taskRequest.dueDate.trim(),
            status = taskRequest.status,
            done = taskRequest.done,
            priority = taskRequest.priority.ifBlank { "MEDIUM" },
            labels = taskRequest.labels.map { it.trim() }.filter { it.isNotEmpty() },
            tags = taskRequest.tags.map { it.trim() }.filter { it.isNotEmpty() },
            categories = taskRequest.categories.map { it.copy(name = it.name.trim()) },
            comments = taskRequest.comments.map { it.copy(message = it.message.trim()) },
            subtasks = taskRequest.subtasks.map { it.copy(title = it.title.trim()) }
        )
        tasks[id] = updated
        return updated
    }

    fun delete(id: Long, ownerId: Long): Boolean = getById(id, ownerId)?.let { tasks.remove(id) != null } ?: false
}
