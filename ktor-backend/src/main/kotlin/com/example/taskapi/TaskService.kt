package com.example.taskapi

class TaskService(private val repository: TaskRepository) {
    fun getAll(ownerId: Long): List<Task> = repository.getAll(ownerId)

    fun getById(id: Long, ownerId: Long): Task? = repository.getById(id, ownerId)

    fun create(request: TaskRequest, ownerId: Long): Task = repository.create(request, ownerId)

    fun update(id: Long, request: TaskRequest, ownerId: Long): Task? = repository.update(id, request, ownerId)

    fun delete(id: Long, ownerId: Long): Boolean = repository.delete(id, ownerId)
}