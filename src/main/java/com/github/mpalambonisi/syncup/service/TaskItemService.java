package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.User;

public interface TaskItemService {

    TaskItem saveTask(String listId, TaskItemCreateDTO dto, User user);
    TaskItem updateTask(String listId, String taskId, TaskItemStatusDTO dto, User user);
}
