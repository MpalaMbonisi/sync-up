package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
}
