package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListTitleUpdateDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository taskListRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskList> getAllListForOwner(User user) {
        return taskListRepository.findAllByOwnerOrCollaborator(user);
    }

    @Override
    public TaskList saveTaskList(User user, TaskListCreateDTO dto) {
        String trimmedTitle = dto.getTitle().trim();

        if(taskListRepository.findByTitleAndOwner(trimmedTitle, user).isPresent()){
            throw new TitleAlreadyExistsException("You already have a list with this title!");
        }

        TaskList taskList = new TaskList();
        taskList.setTitle(trimmedTitle);
        taskList.setOwner(user);

        return taskListRepository.saveAndFlush(taskList);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskList getListById(Long id, User user) {
        return taskListRepository.findByIdAndUserHasAccess(id, user)
                .orElseThrow(() -> new ListNotFoundException("List not found or you don't have access to it!"));
    }

    @Override
    public void removeListById(Long id, User user) {
        TaskList foundList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        if(!foundList.getOwner().getUsername().equals(user.getUsername())){
            throw new AccessDeniedException("Only the list owner can delete this list!");
        }
        taskListRepository.deleteById(id);
    }

    @Override
    public Set<User> addCollaboratorsByUsername(Long id, AddCollaboratorsRequestDTO dto, User user) {
        TaskList taskList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        String username = taskList.getOwner().getUsername();
        if(!username.equals(user.getUsername()))
            throw new AccessDeniedException("User is not authorised to add collaborators!");

        for(String collaborator: dto.getCollaborators()){
            User collabUser = userRepository.findByUsername(collaborator)
                    .orElseThrow(() -> new UsernameNotFoundException("Collaborator username not found!"));
            taskList.getCollaborators().add(collabUser);
        }

        return taskListRepository.save(taskList).getCollaborators();
    }

    @Override
    public void removeCollaboratorByUsername(Long id, RemoveCollaboratorRequestDTO dto, User user) {
        TaskList foundTask = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        if (!foundTask.getOwner().getUsername().equals(user.getUsername()))
            throw new AccessDeniedException("Only the list owner can remove collaborators!");

        User collaborator = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Collaborator username not found!"));

        foundTask.getCollaborators().remove(collaborator);
        taskListRepository.saveAndFlush(foundTask);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<User> getAllCollaborators(Long id, User user) {
        TaskList foundTask = taskListRepository.findByIdAndUserHasAccess(id, user)
                .orElseThrow(() -> new ListNotFoundException("List not found or you don't have access to it!"));

        return foundTask.getCollaborators();
    }

    @Override
    public TaskList updateTaskListTitle(Long id, TaskListTitleUpdateDTO dto, User user) {
        TaskList foundList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        if (!foundList.getOwner().getUsername().equals(user.getUsername()))
            throw new AccessDeniedException("Only the list owner can update the list title!");

        String trimmedTitle = dto.getTitle().trim();
        taskListRepository.findByTitleAndOwner(trimmedTitle, user)
                        .filter(existingList -> !existingList.getId().equals(id))
                                .ifPresent(existingList ->{
                                    throw new TitleAlreadyExistsException("You already have a list with this title!");
                                });

        foundList.setTitle(trimmedTitle);

        return taskListRepository.saveAndFlush(foundList);
    }
}
