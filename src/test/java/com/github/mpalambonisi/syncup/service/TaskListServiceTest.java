package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.service.impl.TaskListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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

}
