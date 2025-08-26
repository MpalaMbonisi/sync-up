package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.impl.TaskListServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskListServiceTest {

    @Mock
    private TaskListRepository taskListRepo;
    @Mock
    private UserRepository userRepository;
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
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.getListById(id, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access this list!");

        // Verify that the repo was still called to try and fetch the list
        verify(taskListRepo, times(1)).findById(id);

    }

    @Test
    void getListById_withNonexistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        when(taskListRepo.findById(invalidId)).thenReturn(Optional.empty());

        // Assert & Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class, () -> {
            taskListService.getListById(invalidId, ownerUser);
        });
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
    }

    @Test
    void removeListById_whenUserIsOwner_shouldDeleteListSuccessfully() {
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));
        doNothing().when(taskListRepo).deleteById(id);

        // Act
        taskListService.removeListById(id, ownerUser);

        // Verify
        verify(taskListRepo, times(1)).findById(id);
        verify(taskListRepo, times(1)).deleteById(id);
    }

    @Test
    void removeListById_UserIsNotOwner_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));

        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.removeListById(id, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access to delete this list!");

        // Verify
        verify(taskListRepo, times(1)).findById(id);
        verify(taskListRepo, never()).deleteById(id);
    }

    @Test
    void removeListById_whenUserIsCollaborator_shouldThrowAccessDeniedException(){
        // Arrange
        User collaborator = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));

        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.removeListById(id, collaborator));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to access to delete this list!");

        // Verify
        verify(taskListRepo, times(1)).findById(id);
        verify(taskListRepo, never()).deleteById(id);
    }

    @Test
    void removeListById_withNonexistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;

        when(taskListRepo.findById(invalidId)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeListById(invalidId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
        verify(taskListRepo, never()).deleteById(invalidId);
    }

    @Test
    void addCollaboratorsByUsername_whenUserIsOwner_shouldReturnList(){
        // Arrange
        User collaborator01 = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));

        User collaborator02 = new User(3L, "nicolencube", "Nicole", "Ncube",
                "nicolencube@gmail.com", encoder.encode("SuperStrongPassword1234"));

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        Set<String> usernames = new HashSet<>();
        usernames.add(collaborator01.getUsername());
        usernames.add(collaborator02.getUsername());
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(usernames);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername(collaborator01.getUsername())).thenReturn(Optional.of(collaborator01));
        when(userRepository.findByUsername(collaborator02.getUsername())).thenReturn(Optional.of(collaborator02));

        // Act
        List<String> savedCollaborators = taskListService.addCollaborators(taskListId, dto, ownerUser);

        // Assert
        assertThat(savedCollaborators)
                        .hasSize(2)
                        .containsExactlyInAnyOrder("johnsmith", "nicolencube");

        // Verify
        InOrder inOrder = inOrder(taskListRepo, userRepository);
        inOrder.verify(taskListRepo).findById(taskListId);
        inOrder.verify(userRepository).findByUsername("johnsmith");
        inOrder.verify(userRepository).findByUsername("nicolencube");
    }

    @Test
    void addCollaboratorsByUsername_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));

        Set<String> usernames = new HashSet<>();
        usernames.add("johnsmith");
        usernames.add("nicolencube");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(usernames);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.addCollaborators(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to add collaborators!");

        // verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(userRepository, never()).findByUsername("nicolencube");
    }

    @Test
    void addCollaboratorsByUsername_whenCollaboratorUsernameIsNonexistent_shouldThrowUsernameNotFoundException(){
        // Arrange
        Set<String> usernames = new HashSet<>();
        usernames.add("karensanders"); // non-existent user
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(usernames);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername("karensanders")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = Assertions.assertThrows(UsernameNotFoundException.class,
                () -> taskListService.addCollaborators(taskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Collaborator username not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepo, userRepository);
        inOrder.verify(taskListRepo).findById(taskListId);
        inOrder.verify(userRepository).findByUsername("karensanders");
    }

    @Test
    void addCollaboratorsByUsername_whenTaskListIdIsNonexistent_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        Set<String> usernames = new HashSet<>();
        usernames.add("johnsmith");
        usernames.add("nicolencube");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(usernames);

        when(taskListRepo.findById(invalidId)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.addCollaborators(invalidId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(userRepository, never()).findByUsername("nicolencube");
    }
}
