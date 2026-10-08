package com.example.taskapi

import com.example.taskapi.api.PageResponse

class InvalidTaskSearchException(message: String) : RuntimeException(message)

class TaskService(private val repository: TaskRepository) {
    fun getAll(ownerId: Long): List<Task> = repository.getAll(ownerId)

    fun getById(id: Long, ownerId: Long): Task? = repository.getById(id, ownerId)

    fun search(ownerId: Long, query: String?, status: TaskStatus?, page: Int, size: Int): PageResponse<Task> {
        if (page < 0 || size !in 1..100) {
            throw InvalidTaskSearchException("page must be >= 0 and size must be between 1 and 100")
        }
        val allMatches = repository.search(ownerId, query?.trim()?.takeIf { it.isNotEmpty() }, status)
        val offset = (page.toLong() * size).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val pageItems = allMatches.drop(offset).take(size)
        val pageCount = if (allMatches.isEmpty()) 0 else (allMatches.size + size - 1) / size
        return PageResponse(pageItems, page, size, allMatches.size.toLong(), pageCount)
    }

    fun create(request: TaskRequest, ownerId: Long): Task = repository.create(request, ownerId)

    fun update(id: Long, request: TaskRequest, ownerId: Long): Task? = repository.update(id, request, ownerId)

    fun delete(id: Long, ownerId: Long): Boolean = repository.delete(id, ownerId)
}