package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.exception.TitleAlreadyExistsException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.TaskListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;

    @Override
    public List<TaskList> getAllListForOwner(User user) {
        return null;
    }

    @Override
    public TaskList saveTaskList(User user, TaskListCreateDTO dto) {

        if(taskListRepository.findByTitle(dto.getTitle()).isPresent()){
            throw new TitleAlreadyExistsException("Title is already being used!");
        }

        TaskList taskList = new TaskList();
        taskList.setTitle(dto.getTitle().trim());
        taskList.setOwner(user);

        return taskListRepository.save(taskList);
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
