package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.dto.response.TaskItemResponseDTO;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.service.impl.TaskItemServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskItemController {

    private final TaskItemServiceImpl taskItemService;

    @GetMapping("/list/{listId}/task/{taskId}")
    public ResponseEntity<TaskItemResponseDTO> getTaskById(@PathVariable Long listId, @PathVariable Long taskId, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.getTaskItemById(listId, taskId, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    @PostMapping("/list/{listId}/task/create")
    public ResponseEntity<TaskItemResponseDTO> createTask(@PathVariable Long listId, @Valid @RequestBody TaskItemCreateDTO dto,
                                                 @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.saveTask(listId, dto, currentUser);
        return new ResponseEntity<>(convertToResponseDTO(taskItem), HttpStatus.CREATED);
    }

    @PatchMapping("/list/{listId}/task/{taskId}/status")
    public ResponseEntity<TaskItemResponseDTO> updateTaskStatus(@PathVariable Long listId, @PathVariable Long taskId,
                                                       @Valid @RequestBody TaskItemStatusDTO dto, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.updateTaskItemStatus(listId, taskId, dto, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    @PatchMapping("/list/{listId}/task/{taskId}/description")
    public ResponseEntity<TaskItemResponseDTO> updateTaskDescription(@PathVariable Long listId, @PathVariable Long taskId,
                                                                     @Valid @RequestBody TaskItemDescriptionDTO dto, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.updateTaskItemDescription(listId, taskId, dto, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    private TaskItemResponseDTO convertToResponseDTO(TaskItem taskItem){
        return TaskItemResponseDTO.builder()
                .id(taskItem.getId())
                .description(taskItem.getDescription())
                .completed(taskItem.getCompleted())
                .taskListTitle(taskItem.getTaskList().getTitle())
                .build();
    }

}
