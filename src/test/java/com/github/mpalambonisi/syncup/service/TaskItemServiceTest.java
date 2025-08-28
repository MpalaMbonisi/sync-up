package com.github.mpalambonisi.syncup.service;


import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskItemRepository;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.impl.TaskItemServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskItemServiceTest {
    @Mock
    private TaskItemRepository taskItemRepository;
    @Mock
    private TaskListRepository taskListRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TaskItemServiceImpl taskItemService;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User(1L, "mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", "StrongPassword1234");
    }

    private TaskList createTaskList(long id, String title, User collaborator){
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle(title);
        taskList.setOwner(ownerUser);
        if (collaborator != null) taskList.getCollaborators().add(collaborator);
        return taskList;
    }

    @Test
    void saveTask_whenUserIsOwner_shouldCreateAndReturnTask(){
        // Arrange
        TaskItemCreateDTO dto = new TaskItemCreateDTO("1kg Banana");
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> {
            TaskItem task = invocation.getArgument(0);
            task.setId(1L); // Simulate DB generating ID
            return task;
        });

        // Act
        TaskItem savedTaskItem = taskItemService.saveTask(taskListId, dto, ownerUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getId()).isEqualTo(1L);
        assertThat(savedTaskItem.getDescription()).isEqualTo("1kg Banana");
        assertThat(savedTaskItem.isCompleted()).isFalse(); // Default should be false
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getTaskList().getTitle()).isEqualTo("Grocery Shopping List");

        // Verify interaction order
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void saveTask_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@yahoo.com", "ReallyStrongPassword1234");

        TaskItemCreateDTO dto = new TaskItemCreateDTO("1kg Banana");
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.saveTask(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access this list!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }

    @Test
    void saveTask_withNonExistentTaskList_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent ID
        TaskItemCreateDTO dto = new TaskItemCreateDTO("1kg Banana");
        when(taskListRepository.findById(invalidTaskListId)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.saveTask(invalidTaskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(invalidTaskListId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }

}
