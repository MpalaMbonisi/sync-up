package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.ShareRequestDTO;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/list")
public class TaskItemController {

    @GetMapping("/all")
    public ResponseEntity<HttpStatus> getAllLists(@AuthenticationPrincipal UserDetails userDetails){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create")
    public ResponseEntity<HttpStatus> createList(@RequestBody TaskListCreateDTO dto, @AuthenticationPrincipal UserDetails userDetails){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpStatus> getListById(@PathVariable Long id){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteListById(@PathVariable Long id){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<HttpStatus> shareList(@PathVariable Long id, @RequestBody ShareRequestDTO dto){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
