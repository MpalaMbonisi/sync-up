package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.response.TaskListResponseDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.service.impl.TaskListServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/list")
@RequiredArgsConstructor
public class TaskListController {

    private final TaskListServiceImpl taskListService;

    @GetMapping("/all")
    public ResponseEntity<List<TaskListResponseDTO>> getAllLists(@AuthenticationPrincipal User currentUser){
        List<TaskList> tasklist = taskListService.getAllListForOwner(currentUser);
        List<TaskListResponseDTO> listDTO = tasklist.stream().map(this::convertIntoDto).toList();
        return ResponseEntity.ok(listDTO);
    }

    @PostMapping("/create")
    public ResponseEntity<TaskListResponseDTO> createList(@Valid @RequestBody TaskListCreateDTO dto,
                                                          @AuthenticationPrincipal User currentUser){
        TaskList savedTaskList = taskListService.saveTaskList(currentUser, dto);
        TaskListResponseDTO responseDTO = TaskListResponseDTO.builder()
                .title(savedTaskList.getTitle())
                .owner(savedTaskList.getOwner().getUsername())
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskListResponseDTO> getListById(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        TaskListResponseDTO responseDTO = convertIntoDto(taskListService.getListById(id, currentUser));
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteListById(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        taskListService.removeListById(id, currentUser);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/collaborator/add")
    public ResponseEntity<List<String>> addCollaboratorsByUsername(@PathVariable Long id,@Valid @RequestBody AddCollaboratorsRequestDTO dto,
                                                       @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(taskListService.addCollaboratorsByUsername(id, dto, currentUser));
    }

    @DeleteMapping("/{id}/collaborator/remove")
    public ResponseEntity<HttpStatus> removeCollaboratorByUsername(@PathVariable Long id, @Valid @RequestBody RemoveCollaboratorRequestDTO dto,
                                                                   @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}/collaborator/all")
    public ResponseEntity<HttpStatus> getAllCollaborators(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private TaskListResponseDTO convertIntoDto(TaskList taskList){
        return TaskListResponseDTO.builder()
                .title(taskList.getTitle())
                .owner(taskList.getOwner().getUsername())
                .collaborators(taskList.getCollaborators()
                        .stream()
                        .map(User::getUsername)
                        .collect(Collectors.toSet()))
                .build();
    }

}
