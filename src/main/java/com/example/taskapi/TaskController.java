package com.example.taskapi;

// import java.util.ArrayList;
import java.util.List;
// import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//requestcontroller tells spring (handles web requests) this class is a 
// controller that will handle incoming http requests and send back responses
//all urls in this class start with /tasks, so if
//  we want to get all tasks, we would go to /tasks, and if we want to create 
// a new task, we would also go to /tasks
@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // temporary hard-coded list of tasks for demonstration purposes
    // temp storage just a list in memory
    // atomic long is a thread-safe way to generate unique IDs for each task
    // private final List<Task> tasks = new ArrayList<>();
    // private final AtomicLong idCounter = new AtomicLong();

    // get tasks handling /tasks -> returns a list of all tasks
    @GetMapping
    public List<Task> getAllTasks() {
        // return tasks;
        return taskRepository.findAll();
    }

    // post /tasks -> creates a new task
    // @RequestBody tells spring to take the json data from the request body and
    // convert it into a Task object
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        // task.setId(idCounter.incrementAndGet());
        // tasks.add(task);
        // return task;
        return taskRepository.save(task);

    }
}

// NOTE
// Test GET (works in browser)

// Visit: http://localhost:8080/tasks
// You'll see an empty list: []

// Step 6 — Test POST (needs a tool — browsers can't POST easily)

// Use curl in a new terminal tab:

// bash
// curl -X POST http://localhost:8080/tasks \
// -H "Content-Type: application/json" \
// -d '{"title": "Buy milk", "done": false}'

// Now refresh http://localhost:8080/tasks in your browser — you'll see your
// task appear!