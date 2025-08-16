package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskItemController {

    @PostMapping("/list/{listId}/task/create")
    public ResponseEntity<HttpStatus> createTask(@PathVariable Long listId, @Valid @RequestBody TaskItemCreateDTO dto,
                                                 @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/list/{listId}/task/{taskId}/update")
    public ResponseEntity<HttpStatus> updateTaskStatus(@PathVariable Long listId, @PathVariable Long taskId,
                                                       @Valid @RequestBody TaskItemStatusDTO dto, @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
