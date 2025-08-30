package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.request.AuthRequestDTO;
import com.github.mpalambonisi.syncup.dto.response.AuthResponseDto;
import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.JwtService;
import com.github.mpalambonisi.syncup.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserServiceImpl userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO dto){
        userService.registerUser(dto);
        return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> authenticateUser(@Valid @RequestBody AuthRequestDTO dto){
        String normalisedUsername = dto.getUsername().toLowerCase().trim();

        // 1. Authenticate the user using the AuthenticationManager
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalisedUsername, dto.getPassword()));

        // 2. If authentication is successful, find the user to generate a token
        var user = userRepository.findByUsername(normalisedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Username does not exist."));

        // 3. Generate the JWT for the found user
        var jwtToken = jwtService.generateToken(user);

        // 4. Return the token in the response
        return ResponseEntity.ok(AuthResponseDto.builder().token(jwtToken).build());
    }

}
