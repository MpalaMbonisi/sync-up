package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListTitleUpdateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TitleAlreadyExistsException;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.impl.TaskListServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList taskListResult = taskListService.saveTaskList(ownerUser, dto);

        // Assert
        assertThat(taskListResult).isNotNull();
        assertThat(taskListResult.getTitle()).isEqualTo(dto.getTitle());
        assertThat(taskListResult.getOwner()).isEqualTo(ownerUser);

        // Verify
        verify(taskListRepo, times(1)).saveAndFlush(any(TaskList.class));
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
        when(taskListRepo.saveAndFlush(taskList)).thenReturn(taskList);
        when(userRepository.findByUsername(collaborator01.getUsername())).thenReturn(Optional.of(collaborator01));
        when(userRepository.findByUsername(collaborator02.getUsername())).thenReturn(Optional.of(collaborator02));

        // Act
        Set<User> savedCollaborators = taskListService.addCollaboratorsByUsername(taskListId, dto, ownerUser);

        // Assert
        assertThat(savedCollaborators)
                        .hasSize(2)
                        .containsExactlyInAnyOrder(collaborator01, collaborator02);

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
                () -> taskListService.addCollaboratorsByUsername(taskListId, dto, unauthorisedUser));
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
                () -> taskListService.addCollaboratorsByUsername(taskListId, dto, ownerUser));
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
                () -> taskListService.addCollaboratorsByUsername(invalidId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(userRepository, never()).findByUsername("nicolencube");
    }

    @Test
    void removeCollaboratorByUsername_whenUserIsOwner_shouldRemoveCollaborator() {
        // Arrange
        String collaboratorUsername = "johnsmith";
        User collaborator = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));

        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(collaboratorUsername);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername(collaboratorUsername)).thenReturn(Optional.of(collaborator));

        // Act
        taskListService.removeCollaboratorByUsername(taskListId, dto, ownerUser);

        // Assert
        assertThat(taskList.getCollaborators()).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(userRepository, times(1)).findByUsername(collaboratorUsername);
        verify(taskListRepo, times(1)).saveAndFlush(taskList);
    }

    @Test
    void removeCollaboratorByUsername_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User collaborator = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.removeCollaboratorByUsername(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to remove collaborators!");

        // verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void removeCollaboratorByUsername_whenCollaboratorIsNonExistent_shouldThrowUsernameNotFoundException(){
        // Arrange
        String collaboratorUsername = "johnsmith"; // non-existent username for collaborator

        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(collaboratorUsername);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername(collaboratorUsername)).thenReturn(Optional.empty());

        // Act
        UsernameNotFoundException exception = Assertions.assertThrows(UsernameNotFoundException.class,
                () -> taskListService.removeCollaboratorByUsername(taskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Collaborator username not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(userRepository, times(1)).findByUsername(collaboratorUsername);
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void removeCollaboratorByUsername_whenTaskListIdIsNonExistent_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        when(taskListRepo.findById(invalidId)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeCollaboratorByUsername(invalidId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void getAllCollaborators_whenUserIsOwner_shouldReturnList(){
        // Arrange
        List<User> collaborators = List.of(new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234")),
                new User(3L, "nicolencube", "Nicole", "Ncube",
                        "nicolencube@gmail.com", encoder.encode("SuperStrongPassword1234")));

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().addAll(collaborators);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act
        Set<User> retrievedList = taskListService.getAllCollaborators(taskListId, ownerUser);

        // Assert
        assertThat(retrievedList)
                .hasSize(2)
                .containsExactlyInAnyOrder(collaborators.get(0), collaborators.get(1));

        // Verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void getAllCollaborators_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));

        List<User> collaborators = List.of(new User(2L, "johnsmith", "John", "Smith",
                        "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234")),
                new User(3L, "nicolencube", "Nicole", "Ncube",
                        "nicolencube@gmail.com", encoder.encode("SuperStrongPassword1234")));

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().addAll(collaborators);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.getAllCollaborators(taskListId, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User is not authorised to retrieve all collaborators!");

        // Verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void getAllCollaborators_whenTaskListIdIsNonexistent_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        when(taskListRepo.findById(invalidId)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.getAllCollaborators(invalidId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        verify(taskListRepo, times(1)).findById(invalidId);
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void getAllCollaborators_whenTaskListHasNoCollaborators_shouldReturnEmptyList(){
        // Arrange
        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findById(taskListId)).thenReturn(Optional.of(taskList));

        // Act
        Set<User> retrievedList = taskListService.getAllCollaborators(taskListId, ownerUser);

        // Assert
        assertThat(retrievedList).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findById(taskListId);
        verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_whenUserIsOwner_shouldUpdateAndReturnTaskList(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        String updatedTitle = "Shoe wishlist";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));
        when(taskListRepo.findByTitle(updatedTitle)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList resultTaskList = taskListService.updateTaskListTitle(id, dto, ownerUser);

        // Assert
        assertThat(resultTaskList).isNotNull();
        assertThat(resultTaskList.getTitle()).isEqualTo(updatedTitle);
        assertThat(resultTaskList.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findById(id);
        inorder.verify(taskListRepo).findByTitle(updatedTitle);
        inorder.verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_whenUserIsCollaborator_shouldThrowAccessDeniedException(){
        // Arrange
        User collaborator = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", encoder.encode("ReallyStrongPassword1234"));

        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);
        taskList.setId(id);

        String updatedTitle = "Shoe wishlist";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.updateTaskListTitle(id, dto, collaborator));
        assertThat(exception.getMessage()).isEqualTo("User not authorised to update this task list title!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findById(id);
        inorder.verify(taskListRepo, never()).findByTitle(updatedTitle);
        inorder.verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_UserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        User unauthorisedUser = new User(2L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", encoder.encode("VeryStrongPassword"));

        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        String updatedTitle = "Shoe wishlist";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.updateTaskListTitle(id, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("User not authorised to update this task list title!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findById(id);
        inorder.verify(taskListRepo, never()).findByTitle(updatedTitle);
        inorder.verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_withDuplicateTitle_shouldThrowTitleAlreadyExistsException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        String updatedTitle = "Grocery Shopping List";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findById(id)).thenReturn(Optional.of(taskList));
        when(taskListRepo.findByTitle(updatedTitle)).thenReturn(Optional.of(taskList));

        // Act & Assert
        TitleAlreadyExistsException exception = Assertions.assertThrows(TitleAlreadyExistsException.class,
                () -> taskListService.updateTaskListTitle(id, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Task list title already exists!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findById(id);
        inorder.verify(taskListRepo).findByTitle(updatedTitle);
        inorder.verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_withNonExistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L;

        String updatedTitle = "Grocery Shopping List";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findById(invalidTaskListId)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.updateTaskListTitle(invalidTaskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findById(invalidTaskListId);
        inorder.verify(taskListRepo, never()).findByTitle(updatedTitle);
        inorder.verify(taskListRepo, never()).saveAndFlush(any(TaskList.class));
    }
}
