package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
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
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl implements TaskItemService {

    private final TaskListRepository taskListRepository;
    private final TaskItemRepository taskItemRepository;

    @Override
    public TaskItem saveTask(long listId, TaskItemCreateDTO dto, User user) {
        TaskList foundList = checkListAvailabilityAndAccess(listId, user);

        TaskItem taskItem = new TaskItem();
        taskItem.setDescription(dto.getDescription().trim());
        taskItem.setTaskList(foundList);
        taskItem.setCompleted(false);

        return taskItemRepository.save(taskItem);
    }

    @Override
    public TaskItem updateTask(long listId, long taskId, TaskItemStatusDTO dto, User user) {
        TaskList foundList = checkListAvailabilityAndAccess(listId, user);

        TaskItem foundTaskItem = taskItemRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task item not found!"));

        // Security Check: Verify the task belongs to the specified list
        if(!foundTaskItem.getTaskList().getId().equals(listId)){
            throw new AccessDeniedException("Task does not belong to the specified list!");
        }

        foundTaskItem.setCompleted(dto.isCompleted());
        return taskItemRepository.save(foundTaskItem);
    }

    @Override
    public TaskItem updateTask(long listId, long taskId, TaskItemDescriptionDTO dto, User user) {
        return null;
    }

    @Override
    public TaskItem getTaskItemById(long listId, long taskId, User user) {
        return null;
    }

    private TaskList checkListAvailabilityAndAccess(long id, User user){
        TaskList foundList = taskListRepository.findById(id)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        boolean isOwner = foundList.getOwner().getUsername().equals(user.getUsername());
        boolean isCollaborator = foundList.getCollaborators().contains(user);

        if(!(isCollaborator || isOwner))
            throw new AccessDeniedException("User is not authorised to access this list!");

        return foundList;
    }
}
