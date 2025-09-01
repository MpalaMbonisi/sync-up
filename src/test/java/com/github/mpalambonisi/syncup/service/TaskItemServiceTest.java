package com.github.mpalambonisi.syncup.service;


import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
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

    private TaskItem createTaskItem(long id, TaskList taskList){
        TaskItem taskItem = new TaskItem();
        taskItem.setId(id);
        taskItem.setCompleted(false);
        taskItem.setDescription("1kg Banana");
        taskItem.setTaskList(taskList);
        return taskItem;
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
        assertThat(savedTaskItem.getCompleted()).isFalse(); // Default should be false
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getTaskList().getTitle()).isEqualTo("Grocery Shopping List");

        // Verify interaction order
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void saveTask_whenUserIsCollaborator_shouldCreateAndReturnTask(){
        // Arrange
        User collaborator = new User(3L, "nicolencube", "Nicole", "Ncube",
                "nicolencube@outlook.com", "VeryStrongPassword1234");
        TaskItemCreateDTO dto = new TaskItemCreateDTO("1kg Banana");
        long taskListId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", collaborator);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> {
            TaskItem task = invocation.getArgument(0);
            task.setId(1L); // Simulate DB generating ID
            return task;
        });

        // Act
        TaskItem savedTaskItem = taskItemService.saveTask(taskListId, dto, collaborator);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getId()).isEqualTo(1L);
        assertThat(savedTaskItem.getDescription()).isEqualTo("1kg Banana");
        assertThat(savedTaskItem.getCompleted()).isFalse(); // Default should be false
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getTaskList().getTitle()).isEqualTo("Grocery Shopping List");
        assertThat(savedTaskItem.getTaskList().getCollaborators().contains(collaborator)).isTrue();

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

    @Test
    void updateTask_whenUserIsOwner_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L, taskItemId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem savedTaskItem = taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, ownerUser);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(savedTaskItem.getCompleted()).isTrue();

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void updateTask_whenUserIsCollaborator_shouldUpdateAndReturnTask(){
        // Arrange
        User collaborator = new User(3L, "nicolencube", "Nicole", "Ncube",
                "nicolencube@outlook.com", "VeryStrongPassword1234");

        long taskListId = 1L, taskItemId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", collaborator);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem savedTaskItem = taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, collaborator);

        // Assert
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(savedTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(savedTaskItem.getCompleted()).isTrue();

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void updateTask_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@yahoo.com", "ReallyStrongPassword1234");

        long taskListId = 1L, taskItemId = 1L;
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.updateTaskItemStatus(taskListId, taskItemId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access this list!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository, never()).findById(taskItemId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }

    @Test
    void updateTask_withNonExistentTaskListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent ID
        long taskItemId = 1L; // assume it exists
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);
        when(taskListRepository.findById(invalidTaskListId)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.updateTaskItemStatus(invalidTaskListId, taskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(invalidTaskListId);
        inOrder.verify(taskItemRepository, never()).findById(taskItemId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }

    @Test
    void updateTask_withNonExistentTaskItemId_shouldThrowTaskNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long invalidTaskItemId = 999L; // non-existent task item ID
        TaskList taskList = createTaskList(taskListId, "Grocery Shopping List", null);
        TaskItemStatusDTO dto = new TaskItemStatusDTO(true);
        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(invalidTaskItemId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = Assertions.assertThrows(TaskNotFoundException.class,
                () -> taskItemService.updateTaskItemStatus(taskListId, invalidTaskItemId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task item not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(invalidTaskItemId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }

    @Test
    void updateTask_whenTaskDoesNotBelongToList_shouldThrowAccessDeniedException(){
        // Arrange
        TaskList allowedList = createTaskList(1L, "Allowed List", null);
        TaskList forbiddenList = createTaskList(2L, "Forbidden List", null);
        TaskItem taskFromForbiddenList = createTaskItem(100L, forbiddenList);

        when(taskListRepository.findById(1L)).thenReturn(Optional.of(allowedList));
        when(taskItemRepository.findById(100L)).thenReturn(Optional.of(taskFromForbiddenList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.updateTaskItemStatus(1L, 100L, new TaskItemStatusDTO(true), ownerUser));
        assertThat(exception.getMessage()).contains("Task does not belong to the specified list");

    }

    @Test
    void getTaskItemById_whenUserIsOwner_shouldReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
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
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void getTaskItemById_whenUserIsCollaborator_shouldReturnTask(){
        // Arrange
        User collaborator = new User(3L, "nicolencube", "Nicole", "Ncube",
                "nicolencube@outlook.com", "VeryStrongPassword1234");

        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", collaborator);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));

        // Act
        TaskItem resultTaskItem = taskItemService.getTaskItemById(taskListId, taskItemId, collaborator);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getDescription()).isEqualTo(taskItem.getDescription());
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void getTaskItemById_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@yahoo.com", "ReallyStrongPassword1234");

        long taskListId = 1L;
        long taskItemId = 100L;
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.getTaskItemById(taskListId, taskItemId, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is unauthorised to get this task item!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository, never()).findById(taskItemId);
    }

    @Test
    void getTaskItemById_withNonExistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L; // non-existent
        long taskItemId = 1L;
        when(taskListRepository.findById(invalidTaskListId)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskItemService.getTaskItemById(invalidTaskListId, taskItemId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(invalidTaskListId);
        inOrder.verify(taskItemRepository, never()).findById(taskItemId);
    }

    @Test
    void getTaskItemById_withNonExistentTaskItemId_shouldThrowTaskNotFoundException(){
        // Arrange
        long taskListId = 1L;
        long invalidTaskItemId = 999L; // non-existent
        TaskList taskList = createTaskList(taskListId, "Grocery List", null);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(invalidTaskItemId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = Assertions.assertThrows(TaskNotFoundException.class,
            () -> taskItemService.getTaskItemById(taskListId, invalidTaskItemId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task item not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(invalidTaskItemId);
    }

    @Test
    void getTaskItemById_whenTaskItemDoesNotBelongToTaskList_shouldThrowAccessDeniedException(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList allowedList = createTaskList(taskListId, "Allowed List", null);
        TaskList forbiddenList = createTaskList(2L, "Forbidden List", null);
        TaskItem taskFromForbiddenList = createTaskItem(taskItemId, forbiddenList);

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(allowedList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskFromForbiddenList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.getTaskItemById(taskListId, taskItemId, ownerUser));
        assertThat(exception.getMessage()).contains("Task does not belong to the specified list");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
    }

    @Test
    void updateTaskItemDescription_whenUserIsOwner_shouldUpdateAndReturnTask(){
        // Arrange
        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", null);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO("Gucci Bag");

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem resultTaskItem = taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto,ownerUser);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(resultTaskItem.getDescription()).isEqualTo("Gucci Bag");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void updateTaskItemDescription_whenUserIsCollaborator_shouldUpdateAndReturnTask(){
        // Arrange
        User collaborator = new User(3L, "nicolencube", "Nicole", "Ncube",
                "nicolencube@outlook.com", "VeryStrongPassword1234");

        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", collaborator);
        TaskItem taskItem = createTaskItem(taskItemId, taskList);
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO("Gucci Bag");

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(taskItemRepository.findById(taskItemId)).thenReturn(Optional.of(taskItem));
        when(taskItemRepository.save(any(TaskItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskItem resultTaskItem = taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto,collaborator);

        // Assert
        assertThat(resultTaskItem).isNotNull();
        assertThat(resultTaskItem.getId()).isEqualTo(taskItemId);
        assertThat(resultTaskItem.getTaskList()).isEqualTo(taskList);
        assertThat(resultTaskItem.getDescription()).isEqualTo("Gucci Bag");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository).findById(taskItemId);
        inOrder.verify(taskItemRepository).save(any(TaskItem.class));
    }

    @Test
    void updateTaskItemDescription_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@yahoo.com", "ReallyStrongPassword1234");

        long taskListId = 1L;
        long taskItemId = 100L;

        TaskList taskList = createTaskList(taskListId, "Shopping wishlist", null);
        createTaskItem(taskItemId, taskList);
        TaskItemDescriptionDTO dto = new TaskItemDescriptionDTO("Gucci Bag");

        when(taskListRepository.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskItemService.updateTaskItemDescription(taskListId, taskItemId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access this list!!");

        // Verify
        InOrder inOrder = inOrder(taskListRepository, taskItemRepository);
        inOrder.verify(taskListRepository).findById(taskListId);
        inOrder.verify(taskItemRepository, never()).findById(taskItemId);
        inOrder.verify(taskItemRepository, never()).save(any(TaskItem.class));
    }
}
