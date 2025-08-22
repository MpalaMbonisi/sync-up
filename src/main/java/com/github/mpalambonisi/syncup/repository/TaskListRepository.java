package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    Optional<TaskList> findByTitle(String title);
}
