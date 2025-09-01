package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.User;

public interface TaskItemService {

    TaskItem saveTask(long listId, TaskItemCreateDTO dto, User user);
    TaskItem updateTask(long listId, long taskId, TaskItemStatusDTO dto, User user);
    TaskItem updateTask(long listId, long taskId, TaskItemDescriptionDTO dto, User user);
    TaskItem getTaskItemById(long listId, long taskId, User user);
}
