package com.taskplatform.task.repository;

import com.taskplatform.task.model.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByUserId(UUID userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(UUID userId, Task.TaskStatus status, Pageable pageable);

    List<Task> findByStatus(Task.TaskStatus status);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, Task.TaskStatus status);
}
