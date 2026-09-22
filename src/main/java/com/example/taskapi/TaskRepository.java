package com.example.taskapi;

import org.springframework.data.jpa.repository.JpaRepository;

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
}