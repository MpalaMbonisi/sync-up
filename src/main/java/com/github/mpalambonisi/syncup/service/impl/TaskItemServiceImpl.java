package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskItemRepository;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.TaskItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl implements TaskItemService {

    private final TaskListRepository taskListRepository;
    private final TaskItemRepository taskItemRepository;

    @Override
    public TaskItem saveTask(long listId, TaskItemCreateDTO dto, User user) {
        TaskList foundList = taskListRepository.findById(listId)
                .orElseThrow(() -> new ListNotFoundException("List not found!"));

        boolean isOwner = foundList.getOwner().getUsername().equals(user.getUsername());
        boolean isCollaborator = foundList.getCollaborators().contains(user);

        if(!(isCollaborator || isOwner))
            throw new AccessDeniedException("User is not authorised to access this list!");

        TaskItem taskItem = new TaskItem();
        taskItem.setDescription(dto.getDescription().trim());
        taskItem.setTaskList(foundList);
        taskItem.setCompleted(false);

        return taskItemRepository.save(taskItem);
    }

    @Override
    public TaskItem updateTask(long listId, String taskId, TaskItemStatusDTO dto, User user) {
        return null;
    }
}
