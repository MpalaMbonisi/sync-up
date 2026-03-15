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
@RequestMapping("/list")
public class TaskItemController {

    private final TaskItemServiceImpl taskItemService;

    @GetMapping("/{listId}/task/{taskId}")
    public ResponseEntity<TaskItemResponseDTO> getTaskById(@PathVariable Long listId, @PathVariable Long taskId, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.getTaskItemById(listId, taskId, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    @PostMapping("/{listId}/task/create")
    public ResponseEntity<TaskItemResponseDTO> createTask(@PathVariable Long listId, @Valid @RequestBody TaskItemCreateDTO dto,
                                                 @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.saveTask(listId, dto, currentUser);
        return new ResponseEntity<>(convertToResponseDTO(taskItem), HttpStatus.CREATED);
    }

    @PatchMapping("/{listId}/task/{taskId}/status")
    public ResponseEntity<TaskItemResponseDTO> updateTaskStatus(@PathVariable Long listId, @PathVariable Long taskId,
                                                       @Valid @RequestBody TaskItemStatusDTO dto, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.updateTaskItemStatus(listId, taskId, dto, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    @PatchMapping("/{listId}/task/{taskId}/description")
    public ResponseEntity<TaskItemResponseDTO> updateTaskDescription(@PathVariable Long listId, @PathVariable Long taskId,
                                                                     @Valid @RequestBody TaskItemDescriptionDTO dto, @AuthenticationPrincipal User currentUser){
        TaskItem taskItem = taskItemService.updateTaskItemDescription(listId, taskId, dto, currentUser);
        return ResponseEntity.ok(convertToResponseDTO(taskItem));
    }

    @DeleteMapping("/{listId}/task/{taskId}")
    public ResponseEntity<HttpStatus> deleteTask(@PathVariable Long listId, @PathVariable Long taskId,
                                                @AuthenticationPrincipal User currentUser){

        taskItemService.deleteTask(listId, taskId, currentUser);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

    protected static TaskItemResponseDTO convertToResponseDTO(TaskItem taskItem){
        return TaskItemResponseDTO.builder()
                .id(taskItem.getId())
                .description(taskItem.getDescription())
                .completed(taskItem.getCompleted())
                .taskListTitle(taskItem.getTaskList().getTitle())
                .build();
    }

}
