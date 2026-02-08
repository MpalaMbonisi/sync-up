package com.github.mpalambonisi.syncup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.mpalambonisi.syncup.exception.UserNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskList;
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

    @Test
    void getAccountDetails_withNonExistentUser_shouldThrowException() {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = Assertions.assertThrows(UserNotFoundException.class,
            () -> accountService.getAccountDetails(testUser));
        assertThat(exception.getMessage()).isEqualTo("User not found!");

        // Verify
        verify(userRepository, times(1)).findById(testUser.getId());
    }

    @Test
    void deleteAccount_withUserHavingNoTaskLists_shouldDeleteUser() {
        // Arrange
        when(taskListRepository.findAll()).thenReturn(new ArrayList<>());
        when(taskListRepository.findAllByOwner(testUser)).thenReturn(new ArrayList<>());
        doNothing().when(userRepository).deleteById(testUser.getId());

        // Act
        accountService.deleteAccount(testUser);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, userRepository);
        inOrder.verify(taskListRepository).findAll();
        inOrder.verify(taskListRepository).findAllByOwner(testUser);
        inOrder.verify(taskListRepository).deleteAll(any());
        inOrder.verify(userRepository).deleteById(testUser.getId());
    }

    @Test
    void deleteAccount_withUserOwningTaskLists_shouldDeleteTaskListsAndUser() {
        // Arrange
        TaskList taskList1 = new TaskList();
        taskList1.setId(1L);
        taskList1.setTitle("Shopping List");
        taskList1.setOwner(testUser);

        TaskList taskList2 = new TaskList();
        taskList2.setId(2L);
        taskList2.setTitle("Todo List");
        taskList2.setOwner(testUser);

        List<TaskList> ownedTaskLists = List.of(taskList1, taskList2);

        when(taskListRepository.findAll()).thenReturn(new ArrayList<>());
        when(taskListRepository.findAllByOwner(testUser)).thenReturn(ownedTaskLists);
        doNothing().when(taskListRepository).deleteAll(ownedTaskLists);
        doNothing().when(userRepository).deleteById(testUser.getId());

        // Act
        accountService.deleteAccount(testUser);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, userRepository);
        inOrder.verify(taskListRepository).findAll();
        inOrder.verify(taskListRepository).findAllByOwner(testUser);
        inOrder.verify(taskListRepository).deleteAll(ownedTaskLists);
        inOrder.verify(userRepository).deleteById(testUser.getId());
    }

    @Test
    void deleteAccount_withUserAsCollaborator_shouldRemoveFromCollaboratorsAndDeleteUser() {
        // Arrange
        TaskList sharedTaskList = new TaskList();
        sharedTaskList.setId(1L);
        sharedTaskList.setTitle("Shared List");
        sharedTaskList.setOwner(ownerUser);
        sharedTaskList.setCollaborators(new HashSet<>());
        sharedTaskList.getCollaborators().add(testUser);

        when(taskListRepository.findAll()).thenReturn(List.of(sharedTaskList));
        when(taskListRepository.findAllByOwner(testUser)).thenReturn(new ArrayList<>());
        when(taskListRepository.save(sharedTaskList)).thenReturn(sharedTaskList);
        doNothing().when(userRepository).deleteById(testUser.getId());

        // Act
        accountService.deleteAccount(testUser);

        // Assert
        assertThat(sharedTaskList.getCollaborators()).doesNotContain(testUser);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, userRepository);
        inOrder.verify(taskListRepository).findAll();
        inOrder.verify(taskListRepository).save(sharedTaskList);
        inOrder.verify(taskListRepository).findAllByOwner(testUser);
        inOrder.verify(taskListRepository).deleteAll(any());
        inOrder.verify(userRepository).deleteById(testUser.getId());
    }


}
