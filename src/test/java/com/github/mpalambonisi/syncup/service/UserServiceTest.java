package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    // Don't forget to mock BcryptPasswordEncoder

    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserServiceImp userService;

    @Test
    void registerNewUser_whenEmailIsNew_shouldCreateAndReturnUser(){
        // Arrange
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .username("mbonisimpala")
                .email("mbonisimpala63@gmail.com")
                .password("mbonisimpala41")
                .build();
        // pretend the username does not exist in the database
        when(userRepository.existsByUsername("mbonisimpala")).thenReturn(false);
        // encode password
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        User savedUser = userService.registerUser(registrationDTO);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(registrationDTO.getEmail());
        assertThat(savedUser.getUsername()).isEqualTo(registrationDTO.getUsername());
        // change with hashed password
        assertThat(savedUser.getPassword()).isEqualTo(registrationDTO.getPassword());

        // Verify
        verify(userRepository, times(1)).save(any(User.class));
        // verify also the password encoder
    }

    @Test
    void registerNewUser_whenEmailAlreadyExists_shouldThrowException(){
        // Arrange
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .username("mbonisimpala")
                .email("mbonisimpala63@gmail.com")
                .password("mbonisimpala41")
                .build();
        // pretend the username does exist in the database
        when(userRepository.existsByUsername("mbonisimpala")).thenReturn(true);

        // Act & Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {
            userService.registerUser(registrationDTO);
        }, "Username already in use.");

        // Verify
        verify(userRepository, never()).save(any());
        // verify password encoder
    }

}
