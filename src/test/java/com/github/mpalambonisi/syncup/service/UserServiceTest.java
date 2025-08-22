package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.exception.UsernameExistsException;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerNewUser_withNewUsername_shouldSaveAndReturnUser(){
        // Arrange
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .username("mbonisimpala")
                .email("mbonisim123@gmail.com")
                .password("StrongPassword1234")
                .build();
        // pretend the username does not exist in the database
        when(userRepository.findByUsername("mbonisimpala")).thenReturn(Optional.empty());
        // encode password
        when(passwordEncoder.encode("StrongPassword1234")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        User savedUser = userService.registerUser(registrationDTO);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(registrationDTO.getEmail());
        assertThat(savedUser.getUsername()).isEqualTo(registrationDTO.getUsername());
        assertThat(savedUser.getPassword()).isEqualTo("hashedPassword");

        // Verify
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("StrongPassword1234");
    }

    @Test
    void registerNewUser_withExistingUsername_shouldThrowUsernameExistsException(){
        // Arrange
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .username("mbonisimpala")
                .email("mbonisim123@gmail.com")
                .password("StrongPassword1234")
                .build();
        // pretend the username does exist in the database
        when(userRepository.findByUsername("mbonisimpala")).thenReturn(Optional.of(new User()));

        // Act & Assert
        Assertions.assertThrows(UsernameExistsException.class, () -> {
            userService.registerUser(registrationDTO);
        }, "Username already in use.");

        // Verify
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    void registerUser_withUnsanitisedData_shouldNormaliseAndSaveUser(){
        // Arrange
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .firstName("MBONISI") // dirty data
                .lastName("mPaLa") 
                .username("mboNisiMPALA") 
                .email("mBONISIm123@gmaIL.COM")
                .password("StrongPassword1234") // except for the password
                .build();
        // pretend the username does not exist in the database
        when(userRepository.findByUsername("mbonisimpala")).thenReturn(Optional.empty());
        // encode password
        when(passwordEncoder.encode("StrongPassword1234")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        User savedUser = userService.registerUser(registrationDTO);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("mbonisim123@gmail.com");
        assertThat(savedUser.getUsername()).isEqualTo("mbonisimpala");
        assertThat(savedUser.getFirstName()).isEqualTo("Mbonisi");
        assertThat(savedUser.getLastName()).isEqualTo("Mpala");
        assertThat(savedUser.getPassword()).isEqualTo("hashedPassword");

        // Verify
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("StrongPassword1234");
        
    }

}
