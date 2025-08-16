package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskListRepository extends JpaRepository<TaskList, Long> {
}
