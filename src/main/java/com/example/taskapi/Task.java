package com.example.taskapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

//creating a new blue print called Task
//This is just a "shape" for our data — every Task has an id, a title, and whether it's done.
// @Entity tells Spring "this class = a database table." 
// @Id + @GeneratedValue means the database auto-generates the id.
@Entity
public class Task {
    // variables below define the properties of the Task class what information
    // a single task will actullay hold private means that these variables can only
    // be accessed within this class
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // create a unique identifier for each task
    private String title;
    private boolean done;

    // constructor is a special method that is called when an object of the class is
    // created
    // special functions used to build a real object using this class blueprint
    // empty constructor or default constructor it allows java or spring to create
    // a completely blank task object first and then fill in the details later with
    // the setter methods
    public Task() {}

    // parameterized constuctor lets u create a fully filled-out
    // task all at once by passing data into the parentheses
    public Task(Long id, String title, boolean done) {
        this.title = title;
        this.done = done;

        // this means the variable belonging to this specific blueprint this line takes
        // th data u passed into thr
        // parantheses and assigns it to the variable belonging to this specific task
        // object(above created variables)
    }

    // our var are private,other files can't access them directly, so we need to
    // create public getter and setter methods to allow other files to read and
    // write the values of these variables
    // When another part of your app calls getId(), this code hands over the task's
    // ID number.
    public Long getId() {
        return id;
    }
    // The word void means "this action changes something but does not return any
    // data back to you". It updates the task's ID with a new value.
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }
    public void setDone(boolean done) {
        this.done = done;
    }
}
