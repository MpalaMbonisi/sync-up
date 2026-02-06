package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.DescriptionAlreadyExistsException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TaskNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskItemRepository;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.TaskItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskItemServiceImpl implements TaskItemService {

    private final TaskListRepository taskListRepository;
    private final TaskItemRepository taskItemRepository;

    @Override
    public TaskItem saveTask(long listId, TaskItemCreateDTO dto, User user) {
        TaskList foundList = checkListAvailabilityAndUserAccess(listId, user);
        String trimmedDescription = dto.getDescription().trim();

        if(taskItemRepository.findByDescriptionAndTaskList(trimmedDescription, foundList).isPresent()){
            throw new DescriptionAlreadyExistsException("A task with this description already exists in this list!");
        }

        TaskItem taskItem = new TaskItem();
        taskItem.setDescription(dto.getDescription().trim());
        taskItem.setTaskList(foundList);
        taskItem.setCompleted(false);

        return taskItemRepository.saveAndFlush(taskItem);
    }

    @Override
    public TaskItem updateTaskItemStatus(long listId, long taskId, TaskItemStatusDTO dto, User user) {

        TaskList foundList = checkListAvailabilityAndUserAccess(listId, user);

        TaskItem foundTaskItem = taskItemRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task item not found!"));

        // Security Check: Verify the task belongs to the specified list
        if(!foundTaskItem.getTaskList().getId().equals(listId)){
            throw new AccessDeniedException("Task does not belong to the specified list!");
        }

        foundTaskItem.setCompleted(dto.isCompleted());
        return taskItemRepository.saveAndFlush(foundTaskItem);
    }

    @Override
    public TaskItem updateTaskItemDescription(long listId, long taskId, TaskItemDescriptionDTO dto, User user) {
        TaskList foundList = checkListAvailabilityAndUserAccess(listId, user);

        TaskItem foundTaskItem = taskItemRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task item not found!"));

        if(!foundTaskItem.getTaskList().getId().equals(listId)){
            throw new AccessDeniedException("Task does not belong to the specified list!");
        }

        String trimmedDescription = dto.getDescription().trim();

        taskItemRepository.findByDescriptionAndTaskList(trimmedDescription, foundList)
                        .filter(existingTask -> !existingTask.getId().equals(taskId))
                                .ifPresent(existingTask -> {
                                    throw new DescriptionAlreadyExistsException("A task with this description already exists in this list!");
                                });

        foundTaskItem.setDescription(dto.getDescription());
        return taskItemRepository.saveAndFlush(foundTaskItem);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskItem getTaskItemById(long listId, long taskId, User user) {
        checkListAvailabilityAndUserAccess(listId, user);

        TaskItem foundTaskItem = taskItemRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task item not found!"));

        if(!foundTaskItem.getTaskList().getId().equals(listId)){
            throw new AccessDeniedException("Task does not belong to the specified list!");
        }

        return foundTaskItem;
    }

    private TaskList checkListAvailabilityAndUserAccess(long id, User user){

        return taskListRepository.findByIdAndUserHasAccess(id, user)
                .orElseThrow(() -> new ListNotFoundException("List not found or you don't have access to it!"));
    }
}
