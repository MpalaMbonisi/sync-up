package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import java.util.List;

public interface TaskListService {

    List<TaskList> getAllListForOwner(User user);
    TaskList saveTaskList(User user, TaskListCreateDTO dto);
    TaskList getListById(Long id, User user);
    void removeListById(Long id, User user);
    List<String> addCollaborators(Long id, AddCollaboratorsRequestDTO dto, User user);
    void removeCollaboratorByUsername(Long id, RemoveCollaboratorRequestDTO dto, User user);
    List<String> getAllCollaborators(Long id, User user);
}
