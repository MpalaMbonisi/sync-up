package com.github.mpalambonisi.syncup.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.mpalambonisi.syncup.dto.response.UserResponseDTO;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.service.impl.AccountServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountServiceImpl accountService;

    @GetMapping("/details")
    public ResponseEntity<UserResponseDTO> getAccountDetails(@AuthenticationPrincipal User currentUser) {
        User user = accountService.getAccountDetails(currentUser);
        UserResponseDTO responseDTO = convertToResponseDTO(user);
        return ResponseEntity.ok(responseDTO);
    }



     private UserResponseDTO convertToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
