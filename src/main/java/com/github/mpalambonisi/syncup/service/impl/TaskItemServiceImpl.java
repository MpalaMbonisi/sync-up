package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.service.TaskItemService;
import org.springframework.stereotype.Service;

@Service
public class TaskItemServiceImp implements TaskItemService {

    @Override
    public TaskItem saveTask(String listId, TaskItemCreateDTO dto, User user) {
        return null;
    }

    @Override
    public TaskItem updateTask(String listId, String taskId, TaskItemStatusDTO dto, User user) {
        return null;
    }
}
