package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    Optional<TaskList> findByTitleAndOwner(String title, User owner);
    List<TaskList> findAllByOwner(User owner);

    @Query("SELECT DISTINCT tl FROM TaskList tl WHERE tl.owner = :user OR :user MEMBER OF tl.collaborators")
    List<TaskList> findAllByOwnerOrCollaborator(@Param("user") User user);
}
