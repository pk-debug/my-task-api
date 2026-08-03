package com.example.taskapi;

import org.springframework.data.jpa.repository.JpaRepository;
// no code needed inside! Spring automatically gives you save, findAll, findById, delete, etc.
public interface TaskRepository extends JpaRepository<Task, Long> {
}
