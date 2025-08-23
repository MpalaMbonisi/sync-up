package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.impl.TaskListServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskListServiceTest {

    @Mock
    private TaskListRepository taskListRepo;
    @InjectMocks
    private TaskListServiceImpl taskListService;
    @Mock
    private PasswordEncoder encoder;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User(1L, "mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", "StrongPassword1234");
    }

    @Test
    void saveTaskList_withValidTitle_shouldCreateAndReturnList(){
        // Arrange
        TaskListCreateDTO dto = TaskListCreateDTO.builder().title("Grocery Shopping List").build();

        // the save method should return the TaskList object it was passed
        when(taskListRepo.save(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList taskListResult = taskListService.saveTaskList(ownerUser, dto);

        // Assert
        assertThat(taskListResult).isNotNull();
        assertThat(taskListResult.getTitle()).isEqualTo(dto.getTitle());
        assertThat(taskListResult.getOwner()).isEqualTo(ownerUser);

        // Verify
        verify(taskListRepo, times(1)).save(any(TaskList.class));
    }

    @Test
    void getAllListForOwner_whenOwnerHasLists_shouldReturnAllLists(){
        // Arrange
        TaskList taskList01 = new TaskList();
        taskList01.setOwner(ownerUser);
        taskList01.setTitle("Grocery Shopping List");
        TaskList taskList02 = new TaskList();
        taskList02.setOwner(ownerUser);
        taskList02.setTitle("Clothing Wishlist");

        List<TaskList> list = List.of(taskList01, taskList02);
        when(taskListRepo.findAllByOwner(ownerUser)).thenReturn(list);

        // Act
        List<TaskList> result = taskListService.getAllListForOwner(ownerUser);

        // Assert
        assertThat(result).isNotEmpty();

        // Verify
        verify(taskListRepo, times(1)).findAllByOwner(ownerUser);

    }

    @Test
    void getAllListForOwner_whenOwnerHasNoList_shouldReturnEmptyList(){
        // Arrange
        when(taskListRepo.findAllByOwner(ownerUser)).thenReturn(List.of());

        // Act
        List<TaskList> result = taskListService.getAllListForOwner(ownerUser);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findAllByOwner(ownerUser);

    }

    @Test
    void getListById_whenUserIsOwner_shouldReturnList(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act
        TaskList result = taskListService.getListById(1L, ownerUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(taskList.getTitle());
        assertThat(result.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        verify(taskListRepo, times(1)).findById(1L);

    }

    @Test
    void getListById_whenUserIsCollaborator_shouldReturnList(){
        // Arrange
        User collaborator = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));

        long id = 1L;
        TaskList tasklist = new TaskList();
        tasklist.setOwner(ownerUser);
        tasklist.setTitle("Grocery Shopping List");
        tasklist.getCollaborators().add(collaborator);
        tasklist.setId(id);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(tasklist));

        // Act
        TaskList result = taskListService.getListById(id, collaborator);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCollaborators().contains(collaborator)).isTrue();
        assertThat(result.getTitle()).isEqualTo(tasklist.getTitle());
        assertThat(result.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        verify(taskListRepo, times(1)).findById(id);
    }

    @Test
    void getListById_whenUserIsNotAuthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));

        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setOwner(ownerUser);
        taskList.setTitle("Grocery Shopping List");
        taskList.setId(id);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class, () ->{
            taskListService.getListById(id, unauthorisedUser);
        });
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access this list!");

        // Verify that the repo was still called to try and fetch the list
        verify(taskListRepo, times(1)).findById(id);

    }

}
