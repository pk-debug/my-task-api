package com.example.taskapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * TaskRepository.java
 * -------------------
 * This interface is the "middleman" between our Java code and the
 * actual database. It handles all the raw SQL work for us so we never
 * have to write queries like:
 *   SELECT * FROM task;
 *   INSERT INTO task (title, done) VALUES (?, ?);
 * by hand.
 *
 * How does this work with NO code written inside it?
 *   By extending JpaRepository<Task, Long>, we tell Spring Data JPA:
 *     - Task  -> the type of object this repository manages
 *     - Long  -> the data type of that object's ID field
 *
 *   Spring then automatically generates a working implementation
 *   behind the scenes at startup, giving us free methods such as:
 *     - findAll()        -> get every row in the "task" table
 *     - findById(id)      -> get one row by its ID
 *     - save(task)         -> insert a new row OR update an existing one
 *     - deleteById(id)     -> remove a row by its ID
 *     - count()            -> how many rows exist
 *
 * We just declare the interface - Spring does the heavy lifting.
 * This is one of the most "magical" parts of Spring Boot for beginners,
 * but it saves an enormous amount of repetitive database code.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    java.util.List<Task> findByDoneFalse();

    java.util.List<Task> findAllByOwnerEmail(String ownerEmail);

    java.util.Optional<Task> findByIdAndOwnerEmail(Long id, String ownerEmail);

        @Query("select task from Task task where task.ownerEmail = :ownerEmail "
            + "and (:query is null or lower(task.title) like lower(concat('%', :query, '%')) "
            + "or lower(task.description) like lower(concat('%', :query, '%'))) "
            + "and (:status is null or task.status = :status)")
        Page<Task> searchOwnedTasks(
            @Param("ownerEmail") String ownerEmail,
            @Param("query") String query,
            @Param("status") Task.TaskStatus status,
            Pageable pageable);
}