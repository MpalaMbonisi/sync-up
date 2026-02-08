package com.github.mpalambonisi.syncup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.impl.AccountServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskListRepository taskListRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User testUser;
    private User ownerUser;
    private User collaboratorUser;

    @BeforeEach
    void setup() {
        testUser = new User(1L, "nicole.smith", "Nicole", "Smith",
        "nicolesmith@example.com", "StrongPassword1234");
        ownerUser = new User(2L, "johndoe", "John", "Doe",
            "johndoe@example.com", "Password1234");
        collaboratorUser = new User(3L, "collabuser", "Collab", "User",
            "collab@example.com", "HashedPassword");
    }

    @Test
    void getAccountDetails_withValidUser_shouldReturnUserDetails() {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        User result = accountService.getAccountDetails(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getUsername()).isEqualTo("nicole.smith");
        assertThat(result.getFirstName()).isEqualTo("Nicole");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getEmail()).isEqualTo("nicolesmith@example.com");

        // Verify
        verify(userRepository, times(1)).findById(testUser.getId());
    }
}
