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
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.TaskListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;
    private final UserRepository userRepository;

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

        TaskList foundList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        boolean isOwner = foundList.getOwner().getUsername().equals(user.getUsername());
        boolean isCollaborator = foundList.getCollaborators().contains(user);

        if(isOwner || isCollaborator){
            return foundList;
        }
        else{
            throw new AccessDeniedException("User is not authorised to access this list!");
        }
    }

    @Override
    public void removeListById(Long id, User user) {

        TaskList foundList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        if(foundList.getOwner().getUsername().equals(user.getUsername())){
            taskListRepository.deleteById(id);
        }
        else{
            throw new AccessDeniedException("User is not authorised to access to delete this list!");
        }
    }

    @Override
    public List<String> addCollaboratorsByUsername(Long id, AddCollaboratorsRequestDTO dto, User user) {
        List<String> collabUserList = new ArrayList<>();

        TaskList taskList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        String username = taskList.getOwner().getUsername();
        if(!username.equals(user.getUsername()))
            throw new AccessDeniedException("User is not authorised to add collaborators!");

        for(String collaborator: dto.getCollaborators()){
            User collabUser = userRepository.findByUsername(collaborator)
                    .orElseThrow(() -> new UsernameNotFoundException("Collaborator username not found!"));
            taskList.getCollaborators().add(collabUser);
            collabUserList.add(collaborator);
        }

        return collabUserList;
    }

    @Override
    public TaskList removeCollaboratorByUsername(Long id, RemoveCollaboratorRequestDTO dto, User user) {
        return null;
    }

    @Override
    public List<String> getAllCollaborators(Long id, User user) {
        return null;
    }
}
