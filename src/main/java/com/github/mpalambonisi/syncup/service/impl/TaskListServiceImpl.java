package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListDuplicateDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListTitleUpdateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TitleAlreadyExistsException;
import com.github.mpalambonisi.syncup.exception.UsernameExistsException;
import com.github.mpalambonisi.syncup.model.TaskItem;
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
        return checkListAvailabilityAndUserAccess(id, user);
    }

    @Override
    public void removeListById(Long id, User user) {
        TaskList foundList = taskListRepository.findByIdAndUserHasAccess(id, user)
                .orElseThrow(() -> new ListNotFoundException("List not found or you don't have access to it!"));

        if(!foundList.getOwner().getUsername().equals(user.getUsername())){
            throw new AccessDeniedException("Only the list owner can delete this list!");
        }
        taskListRepository.deleteById(id);
    }

    @Override
    public Set<User> addCollaboratorsByUsername(Long id, AddCollaboratorsRequestDTO dto, User user) {
        TaskList taskList = checkListAvailabilityAndUserAccess(id, user);

        if(!taskList.getOwner().getUsername().equals(user.getUsername()))
            throw new AccessDeniedException("Only the list owner can add collaborators!");

        for(String collaboratorUsername: dto.getCollaborators()){
            String normalisedUsername = collaboratorUsername.toLowerCase().trim();

            // Don't add the owner as a collaborator
            if(normalisedUsername.equals(user.getUsername())){
                throw new UsernameExistsException("You cannot add owner as a collaborator!");
            }
            User collabUser = userRepository.findByUsername(normalisedUsername)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Collaborator username '" + normalisedUsername + "' not found!"));
            taskList.getCollaborators().add(collabUser);
        }

        return taskListRepository.saveAndFlush(taskList).getCollaborators();
    }

    @Override
    public void removeCollaboratorByUsername(Long id, RemoveCollaboratorRequestDTO dto, User user) {
        TaskList foundTask = checkListAvailabilityAndUserAccess(id, user);

        if (!foundTask.getOwner().getUsername().equals(user.getUsername()))
            throw new AccessDeniedException("Only the list owner can remove collaborators!");

        String normalisedUsername = dto.getUsername().toLowerCase().trim();
        User collaborator = userRepository.findByUsername(normalisedUsername)
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
        TaskList foundList = checkListAvailabilityAndUserAccess(id, user);

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

    @Override
    public TaskList duplicateList(Long id, TaskListDuplicateDTO dto, User user) {
        // Find the original list - user must have access (owner or collaborator)
        TaskList originalList = checkListAvailabilityAndUserAccess(id, user);

        // Generate new title
        String newTitle;
        if (dto.getNewTitle() != null && !dto.getNewTitle().trim().isEmpty()) {
            newTitle = dto.getNewTitle().trim();
        } else {
            newTitle = generateCopyTitle(originalList.getTitle());
        }

        // Check if user already has a list with this title
        if(taskListRepository.findByTitleAndOwner(newTitle, user).isPresent()){
            throw new TitleAlreadyExistsException("You already have a list with this title!");
        }

        TaskList duplicatedList = new TaskList();
        duplicatedList.setTitle(newTitle);
        duplicatedList.setOwner(user);

        // Create tasks and link them in memory
        for (TaskItem originalTask : originalList.getTasks()) {
            TaskItem newTask = new TaskItem();
            newTask.setDescription(originalTask.getDescription());
            newTask.setCompleted(false);
            newTask.setTaskList(duplicatedList); // This sets the FK relationship
            duplicatedList.getTasks().add(newTask);
        }

        // Save EVERYTHING in one single database round-trip
        return taskListRepository.saveAndFlush(duplicatedList);
    }

    private TaskList checkListAvailabilityAndUserAccess(long id, User user){

        return taskListRepository.findByIdAndUserHasAccess(id, user)
                .orElseThrow(() -> new ListNotFoundException("List not found or you don't have access to it!"));
    }

    /**
     * Generates a title for duplicated list with "(Copy)" suffix
     * Handles existing copies by incrementing: "(Copy 2)", "(Copy 3)", etc.
     */
    private String generateCopyTitle(String originalTitle) {
        // Check if title already ends with (Copy X) pattern
        if (originalTitle.matches(".*\\(Copy \\d+\\)$")) {
            // Extract the number and increment
            String numberPart = originalTitle.substring(originalTitle.lastIndexOf("(Copy ") + 6, originalTitle.length() - 1);
            int copyNumber = Integer.parseInt(numberPart);
            String basePart = originalTitle.substring(0, originalTitle.lastIndexOf("(Copy "));
            return basePart + "(Copy " + (copyNumber + 1) + ")";
        } else if (originalTitle.endsWith("(Copy)")) {
            // Change (Copy) to (Copy 2)
            return originalTitle.substring(0, originalTitle.length() - 6) + "(Copy 2)";
        } else {
            // Add (Copy) for the first time
            return originalTitle + " (Copy)";
        }
    }
}
