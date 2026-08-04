package com.example.taskapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Task.java
 * ---------
 * This class is a "blueprint" for a single Task. Every Task object built
 * from this blueprint will have exactly 3 pieces of information:
 *   1. id    - a unique number identifying this task
 *   2. title - the text describing the task (e.g. "Buy milk")
 *   3. done  - whether the task has been completed (true/false)
 *
 * @Entity
 *   This is the most important annotation here. It tells Spring:
 *   "This class isn't just a normal Java class - it represents a TABLE
 *   in the database." Because of this one annotation, Hibernate (the
 *   library that talks to the database on our behalf) automatically
 *   creates a table called "task" with columns matching our variables
 *   below (id, title, done).
 *
 * Why are the variables "private"?
 *   Making them private means no other class can reach in and change
 *   them directly (e.g. someController.task.title = "hack"). Instead,
 *   other classes must go through the public getter/setter methods
 *   below. This protects our data and is a core rule of Java called
 *   "encapsulation."
 */
@Entity
public class Task {

    /**
     * The unique ID for this task.
     *
     * @Id
     *   Tells Hibernate "this field is the primary key" - the unique
     *   fingerprint that identifies one row in the database table.
     *
     * @GeneratedValue(strategy = GenerationType.IDENTITY)
     *   Tells the database "you generate this number for me automatically
     *   every time a new task is saved (1, 2, 3, ...)." We never set this
     *   ourselves - the database handles it.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The text of the task, e.g. "Buy milk" or "Finish homework". */
    private String title;

    /** Whether this task has been completed yet. Defaults to false. */
    private boolean done;

    /**
     * Empty (no-argument) constructor.
     *
     * Spring and Hibernate require this to exist so they can create a
     * blank Task object internally (for example, when reading a row back
     * from the database) and then fill in the fields afterward using the
     * setter methods below. You will rarely call this yourself directly.
     */
    public Task() {
    }

    /**
     * Convenience constructor - lets you build a fully-filled Task in
     * one line instead of calling three separate setters.
     *
     * Example usage:
     *   Task t = new Task("Buy milk", false);
     *
     * Note: "id" is intentionally NOT a parameter here, because the
     * database assigns the id automatically when the task is saved.
     *
     * @param title the description of the task
     * @param done  whether the task is already completed
     */
    public Task(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    // -------------------------------------------------------------
    // Getters and setters
    // These are the "public doors" that let other classes (like
    // TaskController) read or update this task's private data safely.
    // -------------------------------------------------------------

    /** @return this task's unique database ID. */
    public Long getId() {
        return id;
    }

    /**
     * Updates this task's ID.
     * "void" means this method changes something but doesn't hand
     * back any data to whoever called it.
     *
     * @param id the new ID value
     */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the task's title text. */
    public String getTitle() {
        return title;
    }

    /** @param title the new title text for this task. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return true if the task is completed, false otherwise. */
    public boolean isDone() {
        return done;
    }

    /** @param done the new completion status for this task. */
    public void setDone(boolean done) {
        this.done = done;
    }
}