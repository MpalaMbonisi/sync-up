package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/list")
public class TaskListController {

    @GetMapping("/all")
    public ResponseEntity<HttpStatus> getAllLists(@AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createList(@Valid @RequestBody TaskListCreateDTO dto,
                                                 @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpStatus> getListById(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteListById(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{id}/collaborator/add")
    public ResponseEntity<HttpStatus> addCollaborators(@PathVariable Long id,@Valid @RequestBody AddCollaboratorsRequestDTO dto,
                                                       @AuthenticationPrincipal User currentUser){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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

}
