package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    Optional<TaskItem> findByDescription(String description);
}
