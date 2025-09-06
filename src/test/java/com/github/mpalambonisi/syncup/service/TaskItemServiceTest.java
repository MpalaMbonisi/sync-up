package com.github.mpalambonisi.syncup.service;


import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.DescriptionAlreadyExistsException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TaskNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskItemServiceTest {
    @Mock
    private TaskItemRepository taskItemRepository;
    @Mock
    private TaskListRepository taskListRepository;
    @InjectMocks
    private TaskItemServiceImpl taskItemService;

    private User ownerUser;
    private User collaboratorUser;
    private User unauthorisedUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User(1L, "mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", "StrongPassword1234");
        collaboratorUser = new User(2L, "nicolencube", "Nicole", "Ncube",
        "nicolencube@outlook.com", "VeryStrongPassword1234");
        unauthorisedUser = new User(3L, "karensanders", "Karen", "Sanders",
                "karensanders@yahoo.com", "ReallyStrongPassword1234");
    }

    private TaskList createTaskList(long id, String title, User collaborator){
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle(title);
        taskList.setOwner(ownerUser);
        if (collaborator != null) taskList.getCollaborators().add(collaborator);
        return taskList;
    }

    private TaskItem createTaskItem(long id, String description, TaskList taskList){
        TaskItem taskItem = new TaskItem();
        taskItem.setId(id);
        taskItem.setCompleted(false);
        taskItem.setDescription(description);
        taskItem.setTaskList(taskList);
        return taskItem;
    }

    @Test
    void saveTask_whenUserIsOwner_shouldCreateAndReturnTask(){
        // Arrange
        String description = "1kg Banana";
        TaskItemCreateDTO dto = new TaskItemCreateDTO(description);
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findByDescriptionAndTaskList(description.trim(), taskList)).thenReturn(Optional.empty());
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> {
            TaskItem task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        // Act
        TaskItem savedTaskItem = taskItemService.saveTask(taskListId, dto, ownerUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getId()).isEqualTo(1L);
        assertThat(savedTaskItem.getDescription()).isEqualTo(description.trim());
        assertThat(savedTaskItem.getCompleted()).isFalse();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(description.trim(), taskList);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void saveTask_whenUserIsCollaborator_shouldCreateAndReturnTask(){
        // Arrange
        String description = "1kg Banana";
        TaskItemCreateDTO dto = new TaskItemCreateDTO(description);
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", collaboratorUser);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, collaboratorUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findByDescriptionAndTaskList(description.trim(), taskList)).thenReturn(Optional.empty());
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> {
            TaskItem task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        // Act
        TaskItem savedTaskItem = taskItemService.saveTask(taskListId, dto, collaboratorUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getId()).isEqualTo(1L);
        assertThat(savedTaskItem.getDescription()).isEqualTo(description.trim());
        assertThat(savedTaskItem.getCompleted()).isFalse();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getTaskList().getCollaborators().contains(collaboratorUser)).isTrue();

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, collaboratorUser);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(description.trim(), taskList);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void saveTask_whenUserIsUnauthorised_shouldThrowListNotFoundException(){
        // Arrange
        String description = "1kg Banana";
        TaskItemCreateDTO dto = new TaskItemCreateDTO(description);
        long taskListId = 1L;

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.saveTask(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void saveTask_withNonExistentTaskList_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent ID
        String description = "1kg Banana";
        TaskItemCreateDTO dto = new TaskItemCreateDTO(description);

        when(taskListRepository.findByIdAndUserHasAccess(invalidTaskListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.saveTask(invalidTaskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(invalidTaskListId, ownerUser);
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void saveTask_withDuplicateDescriptionInSameList_shouldThrowDescriptionAlreadyExistsException(){
        // Arrange
        String duplicateDescription = "1kg Banana";
        TaskItemCreateDTO dto = new TaskItemCreateDTO(duplicateDescription);
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItem existingTaskItem = createTaskItem(100L, duplicateDescription, taskList);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findByDescriptionAndTaskList(duplicateDescription.trim(), taskList)).thenReturn(Optional.of(existingTaskItem));

        // Act & Assert
        DescriptionAlreadyExistsException exception = Assertions.assertThrows(DescriptionAlreadyExistsException.class,
                () -> taskItemService.saveTask(taskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("A task with this description already exists in this list!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(duplicateDescription.trim(), taskList);
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemStatus_whenUserIsOwner_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L, taskItemId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut Butter", taskList);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem savedTaskItem = taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, ownerUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(savedTaskItem.getCompleted()).isTrue();

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemStatus_whenUserIsCollaborator_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L, taskItemId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", collaboratorUser);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut butter", taskList);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, collaboratorUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem savedTaskItem = taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, collaboratorUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(savedTaskItem.getCompleted()).isTrue();

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, collaboratorUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemStatus_whenUserIsUnauthorised_shouldThrowListNotFoundException(){
        // Arrange
        long taskListId = 1L, taskItemId = 1L;
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskItemStatus_withNonExistentTaskListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent ID
        long taskItemId = 1L;
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);
        when(taskListRepository.findByIdAndUserHasAccess(invalidTaskListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.updateTaskItemStatus(invalidTaskListId, taskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(invalidTaskListId, ownerUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemStatus_withNonExistentTaskItemId_shouldThrowTaskNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long invalidTaskItemId = 999L; // non-existent task item ID
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);
        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(invalidTaskItemId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = Assertions.assertThrows(TaskNotFoundException.class,
                () -> taskItemService.updateTaskItemStatus(taskListId, invalidTaskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task item not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(invalidTaskItemId);
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemStatus_whenTaskDoesNotBelongToList_shouldThrowAccessDeniedException(){
        // Arrange
        TaskList allowedList = createTaskList(1L, "Allowed List", null);
        TaskList forbiddenList = createTaskList(2L, "Forbidden List", null);
        TaskItem taskFromForbiddenList = createTaskItem(100L,"Peanut butter", forbiddenList);

        long taskListId = allowedList.getId();
        long taskItemId = taskFromForbiddenList.getId();

        when(taskListRepository.findByIdAndUserHasAccess(1L, ownerUser)).thenReturn(Optional.of(allowedList));
        when(taskItemRepository.findById(100L)).thenReturn(Optional.of(taskFromForbiddenList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.updateTaskItemStatus(1L, 100L, new TaskItemStatusDTO(true), ownerUser));
        assertThat(exception.getMessage()).contains("Task does not belong to the specified list");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void getTaskItemById_whenUserIsOwner_shouldReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut butter", taskList);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));

        // Act
        TaskItem resultTaskItem = taskItemService.getTaskItemById(taskListId, taskItemId, ownerUser);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void getTaskItemById_whenUserIsCollaborator_shouldReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", collaboratorUser);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut butter", taskList);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, collaboratorUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));

        // Act
        TaskItem resultTaskItem = taskItemService.getTaskItemById(taskListId, taskItemId, collaboratorUser);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, collaboratorUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void getTaskItemById_whenUserIsUnauthorised_shouldThrowListNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);
        createTaskItem(taskItemId, "Peanut butter", taskList);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.getTaskItemById(taskListId, taskItemId, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
    }

    @Test
    void getTaskItemById_withNonExistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent
        long taskItemId = 1L;
        when(taskListRepository.findByIdAndUserHasAccess(invalidTaskListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.getTaskItemById(invalidTaskListId, taskItemId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(invalidTaskListId, ownerUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
    }

    @Test
    void getTaskItemById_withNonExistentTaskItemId_shouldThrowTaskNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long invalidTaskItemId = 999L; // non-existent
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(invalidTaskItemId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = Assertions.assertThrows(TaskNotFoundException.class,
            () -> taskItemService.getTaskItemById(taskListId, invalidTaskItemId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task item not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(invalidTaskItemId);
    }

    @Test
    void getTaskItemById_whenTaskItemDoesNotBelongToTaskList_shouldThrowAccessDeniedException(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList allowedList = createTaskList(taskListId, "Allowed List", null);
        TaskList forbiddenList = createTaskList(2L, "Forbidden List", null);
        TaskItem taskFromForbiddenList = createTaskItem(taskItemId, "Peanut butter", forbiddenList);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(allowedList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskFromForbiddenList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.getTaskItemById(taskListId, taskItemId, ownerUser));
        assertThat(exception.getMessage()).contains("Task does not belong to the specified list");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void updateTaskItemDescription_whenUserIsOwner_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", null);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut butter", taskList);

        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.findByDescriptionAndTaskList(updatedDesc, taskList)).thenReturn(Optional.empty());
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem resultTaskItem = taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto,ownerUser);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(resultTaskItem.getDescription()).isEqualTo(updatedDesc);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(updatedDesc, taskList);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemDescription_whenUserIsCollaborator_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", collaboratorUser);
        TaskItem taskItem = createTaskItem(taskItemId, "Peanut butter", taskList);
        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, collaboratorUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.findByDescriptionAndTaskList(updatedDesc, taskList)).thenReturn(Optional.empty());
        when(taskItemRepository.saveAndFlush(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem resultTaskItem = taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto,collaboratorUser);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(resultTaskItem.getDescription()).isEqualTo(updatedDesc);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, collaboratorUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(updatedDesc, taskList);
        inOrder.verify(taskItemRepository).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemDescription_whenUserIsUnauthorised_shouldThrowListNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any(TaskItem.class));
    }

    @Test
    void updateTaskItemDescription_whenUpdatingToExistingDescriptionInSameList_shouldThrowDescriptionAlreadyExistsException(){
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", null);
        TaskItem taskItemToUpdate = createTaskItem(taskItemId, "Current Description", taskList);
        TaskItem existingTaskWithTargetDescription = createTaskItem(101L, "Target Description", taskList);

        String duplicateDesc = "Target Description";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(duplicateDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItemToUpdate));
        when(taskItemRepository.findByDescriptionAndTaskList(duplicateDesc.trim(), taskList))
                .thenReturn(Optional.of(existingTaskWithTargetDescription));

        // Act & Assert
        DescriptionAlreadyExistsException exception = Assertions.assertThrows(DescriptionAlreadyExistsException.class,
                () -> taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("A task with this description already exists in this list!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).findByDescriptionAndTaskList(duplicateDesc.trim(), taskList);
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskItemDescription_withNonExistentTaskListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent task list ID
        long taskItemId = 100L;

        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);
        when(taskListRepository.findByIdAndUserHasAccess(invalidTaskListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.updateTaskItemDescription(invalidTaskListId, taskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(invalidTaskListId, ownerUser);
        inOrder.verify(taskItemRepository, never()).findById(any());
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskItemDescription_withNonExistentTaskItemId_shouldThrowTaskNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long invalidTaskItemId = 999L; // non-existent task item id

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", null);
        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(invalidTaskItemId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = Assertions.assertThrows(TaskNotFoundException.class,
                () -> taskItemService.updateTaskItemDescription(taskListId, invalidTaskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task item not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(invalidTaskItemId);
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskItemDescription_whenTaskDoesNotBelongToList_shouldThrowAccessDeniedException(){
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList allowedList = createTaskList(taskListId, "Allowed List", null);
        TaskList forbiddenList = createTaskList(2L, "Forbidden List", null);
        TaskItem taskFromForbiddenList = createTaskItem(taskItemId, "Peanut butter", forbiddenList);

        String updatedDesc = "Jam";
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO(updatedDesc);

        when(taskListRepository.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(allowedList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskFromForbiddenList));
        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).contains("Task does not belong to the specified list!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository, never()).findByDescriptionAndTaskList(any(), any());
        inOrder.verify(taskItemRepository, never()).saveAndFlush(any());
    }
}
