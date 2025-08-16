package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskListServiceImp implements TaskListService{
    @Override
    public List<TaskList> getAllListForCurrentUser(User user) {
        return null;
    }

    @Override
    public TaskList saveTaskList(User user, TaskListCreateDTO dto) {
        return null;
    }

    @Override
    public TaskList getListById(Long id, User user) {
        return null;
    }

    @Override
    public void removeListById(Long id, User user) {

    }

    @Override
    public List<String> addCollaborators(Long id, AddCollaboratorsRequestDTO dto, User user) {
        return null;
    }

    @Override
    public void removeCollaboratorByUsername(Long id, RemoveCollaboratorRequestDTO dto, User user) {

    }

    @Override
    public List<String> getAllCollaborators(Long id, User user) {
        return null;
    }
}
