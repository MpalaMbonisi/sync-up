package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.AuthRequestDTO;
import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<HttpStatus> registerUser(@RequestBody UserRegistrationDTO dto){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/login")
    public ResponseEntity<HttpStatus> authenticateUser(@RequestBody AuthRequestDTO dto){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
