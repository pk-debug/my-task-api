package com.example.taskapi;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TaskController.java
 * -------------------
 * This class is the "front door" of our API. It listens for incoming
 * HTTP requests (the kind your browser or an app like Postman sends)
 * and decides what to do with them.
 *
 * @RestController
 *                 Tells Spring: "this class handles web requests, and every
 *                 method's
 *                 return value should be converted straight into JSON and sent
 *                 back
 *                 to whoever asked for it." Without this annotation, Spring
 *                 would
 *                 have no idea this class is meant to serve web traffic.
 *
 *                 @RequestMapping("/tasks")
 *                 Sets a shared URL prefix for every method in this class.
 *                 Because of
 *                 this, every endpoint below automatically starts with
 *                 "/tasks":
 *                 GET /tasks -> handled by getAllTasks()
 *                 POST /tasks -> handled by createTask()
 *
 *                 How data flows through this class:
 *                 Browser/curl --HTTP request--> TaskController
 *                 |
 *                 v
 *                 TaskRepository
 *                 |
 *                 v
 *                 SQLite database (tasks.db)
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    /**
     * "final" means this reference can never be reassigned after the
     * constructor sets it once. Combined with constructor injection
     * below, this guarantees taskRepository is never null and never
     * swapped out accidentally later in the code.
     */
    private final TaskRepository taskRepository;

    /**
     * Constructor injection.
     * Spring automatically calls this constructor when creating a
     * TaskController, and automatically supplies a working
     * TaskRepository as the argument. No @Autowired annotation is
     * needed here because there's only one constructor - Spring uses
     * it by default.
     */
    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Handles: GET /tasks
     *
     * Returns every task currently stored in the database.
     * Example: visiting http://localhost:8080/tasks in a browser
     * triggers this method and returns something like:
     * [{"id":1,"title":"Buy milk","done":false}]
     *
     * @return a list of every Task row in the database
     */
    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Handles: POST /tasks
     *
     * Creates and saves a brand new task.
     *
     * @RequestBody Task task
     *              Tells Spring: "take the raw JSON text sent in the request body
     *              (e.g. {"title":"Buy milk","done":false}) and automatically
     *              convert it into a real Task object for me." This conversion is
     *              done using the getters/setters defined in Task.java.
     *
     *              Example request (using curl):
     *              curl -X POST http://localhost:8080/tasks \
     *              -H "Content-Type: application/json" \
     *              -d '{"title": "Buy milk", "done": false}'
     *
     * @param task the new task's data, parsed automatically from JSON
     * @return the same task, now including the auto-generated ID
     *         assigned by the database
     */
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskRepository.save(task);
    }
}