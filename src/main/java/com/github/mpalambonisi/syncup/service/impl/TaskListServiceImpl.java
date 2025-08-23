package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TitleAlreadyExistsException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.TaskListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;

    @Override
    public List<TaskList> getAllListForOwner(User user) {
        return taskListRepository.findAllByOwner(user);
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
        Optional<TaskList> foundList = taskListRepository.findById(id);
        if(foundList.isEmpty()){
            throw new ListNotFoundException("List not found!");
        }
        boolean isOwner = foundList.get().getOwner().getUsername().equals(user.getUsername());
        boolean isCollaborator = foundList.get().getCollaborators().contains(user);

        if(isOwner || isCollaborator){
            return foundList.get();
        }
        else{
            throw new AccessDeniedException("User is not authorised to access this list!");
        }
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
