package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListDuplicateDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListTitleUpdateDTO;
import com.github.mpalambonisi.syncup.exception.AccessDeniedException;
import com.github.mpalambonisi.syncup.exception.ListNotFoundException;
import com.github.mpalambonisi.syncup.exception.TitleAlreadyExistsException;
import com.github.mpalambonisi.syncup.model.TaskItem;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private User collaboratorUser;
    private User unauthorisedUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User(1L, "mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", "StrongPassword1234");
        collaboratorUser = new User(2L, "johnsmith", "John", "Smith",
                "johnsmith@yahoo.com", "ReallyStrongPassword1234");
        unauthorisedUser = new User(3L, "karensanders", "Karen", "Sanders",
                "karensanders@outlook.com", "VeryStrongPassword");
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
        when(taskListRepo.findAllByOwnerOrCollaborator(ownerUser)).thenReturn(list);

        // Act
        List<TaskList> result = taskListService.getAllListForOwner(ownerUser);

        // Assert
        assertThat(result).isNotEmpty();

        // Verify
        verify(taskListRepo, times(1)).findAllByOwnerOrCollaborator(ownerUser);

    }

    @Test
    void getAllListForOwner_whenOwnerHasNoList_shouldReturnEmptyList(){
        // Arrange
        when(taskListRepo.findAllByOwnerOrCollaborator(ownerUser)).thenReturn(List.of());

        // Act
        List<TaskList> result = taskListService.getAllListForOwner(ownerUser);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findAllByOwnerOrCollaborator(ownerUser);

    }

    @Test
    void getAllListForOwner_whenUserIsCollaboratorOnly_shouldReturnCollaboratorLists(){
        // Arrange
        User anotherOwner = new User(4L, "kamilnowaski", "Kamil", "Nowaski",
                "kamilnowaski45@yahoo.com", "Password987");

        TaskList collaboratorList01 = new TaskList();
        collaboratorList01.setOwner(anotherOwner);
        collaboratorList01.setTitle("Project Alpha");
        collaboratorList01.getCollaborators().add(ownerUser);

        TaskList collaboratorList02 = new TaskList();
        collaboratorList02.setOwner(anotherOwner);
        collaboratorList02.setTitle("Project Beta");
        collaboratorList02.getCollaborators().add(ownerUser);

        List<TaskList> collaboratorLists = List.of(collaboratorList01, collaboratorList02);
        when(taskListRepo.findAllByOwnerOrCollaborator(ownerUser)).thenReturn(collaboratorLists);

        // Act
        List<TaskList> resultTaskList = taskListService.getAllListForOwner(ownerUser);

        // Assert
        assertThat(resultTaskList).hasSize(2);
        assertThat(resultTaskList).containsExactlyInAnyOrder(collaboratorList01, collaboratorList02);
        assertThat(resultTaskList.stream().noneMatch(list -> list.getOwner().equals(ownerUser))).isTrue();
        assertThat(resultTaskList.stream().allMatch(list -> list.getCollaborators().contains(ownerUser)));

        // Verify
        verify(taskListRepo).findAllByOwnerOrCollaborator(ownerUser);
    }

    @Test
    void getListById_whenUserIsOwner_shouldReturnList(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        when(taskListRepo.findByIdAndUserHasAccess(id, ownerUser)).thenReturn(Optional.of(taskList));

        // Act
        TaskList result = taskListService.getListById(1L, ownerUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(taskList.getTitle());
        assertThat(result.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, ownerUser);

    }

    @Test
    void getListById_whenUserIsCollaborator_shouldReturnList(){
        // Arrange
        long id = 1L;
        TaskList tasklist = new TaskList();
        tasklist.setOwner(ownerUser);
        tasklist.setTitle("Grocery Shopping List");
        tasklist.getCollaborators().add(collaboratorUser);
        tasklist.setId(id);

        when(taskListRepo.findByIdAndUserHasAccess(id, collaboratorUser)).thenReturn(Optional.of(tasklist));

        // Act
        TaskList result = taskListService.getListById(id, collaboratorUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCollaborators().contains(collaboratorUser)).isTrue();
        assertThat(result.getTitle()).isEqualTo(tasklist.getTitle());
        assertThat(result.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, collaboratorUser);
    }

    @Test
    void getListById_whenUserIsNotAuthorised_shouldListNotFoundException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setOwner(ownerUser);
        taskList.setTitle("Grocery Shopping List");
        taskList.setId(id);

        when(taskListRepo.findByIdAndUserHasAccess(id, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.getListById(id, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify that the repo was still called to try and fetch the list
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, unauthorisedUser);
    }

    @Test
    void getListById_withNonexistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        when(taskListRepo.findByIdAndUserHasAccess(invalidId, ownerUser)).thenReturn(Optional.empty());

        // Assert & Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class, () -> {
            taskListService.getListById(invalidId, ownerUser);
        });
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(invalidId, ownerUser);
    }

    @Test
    void removeListById_whenUserIsOwner_shouldDeleteListSuccessfully() {
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findByIdAndUserHasAccess(id, ownerUser)).thenReturn(Optional.of(taskList));
        doNothing().when(taskListRepo).deleteById(id);

        // Act
        taskListService.removeListById(id, ownerUser);

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, ownerUser);
        verify(taskListRepo, times(1)).deleteById(id);
    }

    @Test
    void removeListById_UserIsNotOwner_shouldThrowAccessDeniedException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findByIdAndUserHasAccess(id, unauthorisedUser)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeListById(id, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, unauthorisedUser);
        verify(taskListRepo, never()).deleteById(id);
    }

    @Test
    void removeListById_whenUserIsCollaborator_shouldThrowAccessDeniedException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(id);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaboratorUser);

        when(taskListRepo.findByIdAndUserHasAccess(id, collaboratorUser)).thenReturn(Optional.of(taskList));

        // Act
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.removeListById(id, collaboratorUser));
        assertThat(exception.getMessage()).isEqualTo("Only the list owner can delete this list!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(id, collaboratorUser);
        verify(taskListRepo, never()).deleteById(id);
    }

    @Test
    void removeListById_withNonexistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;

        when(taskListRepo.findByIdAndUserHasAccess(invalidId, ownerUser)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeListById(invalidId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(invalidId, ownerUser);
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

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
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
        inOrder.verify(taskListRepo).findByIdAndUserHasAccess(taskListId, ownerUser);
        inOrder.verify(userRepository).findByUsername("johnsmith");
        inOrder.verify(userRepository).findByUsername("nicolencube");
    }

    @Test
    void addCollaboratorsByUsername_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        Set<String> usernames = new HashSet<>();
        usernames.add("johnsmith");
        usernames.add("nicolencube");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(usernames);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.addCollaboratorsByUsername(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("Only the list owner can add collaborators!");

        // verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
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

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername("karensanders")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = Assertions.assertThrows(UsernameNotFoundException.class,
                () -> taskListService.addCollaboratorsByUsername(taskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Collaborator username 'karensanders' not found!");

        // Verify
        InOrder inOrder = inOrder(taskListRepo, userRepository);
        inOrder.verify(taskListRepo).findByIdAndUserHasAccess(taskListId, ownerUser);
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

        when(taskListRepo.findByIdAndUserHasAccess(invalidId, ownerUser)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.addCollaboratorsByUsername(invalidId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(invalidId, ownerUser);
        verify(userRepository, never()).findByUsername("johnsmith");
        verify(userRepository, never()).findByUsername("nicolencube");
    }

    @Test
    void removeCollaboratorByUsername_whenUserIsOwner_shouldRemoveCollaborator() {
        // Arrange
        String collaboratorUsername = "johnsmith";
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(collaboratorUsername);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaboratorUser);

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername(collaboratorUsername)).thenReturn(Optional.of(collaboratorUser));

        // Act
        taskListService.removeCollaboratorByUsername(taskListId, dto, ownerUser);

        // Assert
        assertThat(taskList.getCollaborators()).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, ownerUser);
        verify(userRepository, times(1)).findByUsername(collaboratorUsername);
        verify(taskListRepo, times(1)).saveAndFlush(taskList);
    }

    @Test
    void removeCollaboratorByUsername_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        String collaboratorUsername = "johnsmith";
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(collaboratorUsername);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaboratorUser);

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeCollaboratorByUsername(taskListId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        verify(userRepository, never()).findByUsername(any());
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void removeCollaboratorByUsername_whenCollaboratorIsNonExistent_shouldThrowUsernameNotFoundException(){
        // Arrange
        String collaboratorUsername = "nicolencube"; // non-existent username for collaborator

        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(collaboratorUsername);

        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));
        when(userRepository.findByUsername(collaboratorUsername)).thenReturn(Optional.empty());

        // Act
        UsernameNotFoundException exception = Assertions.assertThrows(UsernameNotFoundException.class,
                () -> taskListService.removeCollaboratorByUsername(taskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("Collaborator username not found!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, ownerUser);
        verify(userRepository, times(1)).findByUsername(collaboratorUsername);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void removeCollaboratorByUsername_whenTaskListIdIsNonExistent_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        when(taskListRepo.findByIdAndUserHasAccess(invalidId, ownerUser)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.removeCollaboratorByUsername(invalidId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(invalidId, ownerUser);
        verify(userRepository, never()).findByUsername(any());
        verify(taskListRepo, never()).saveAndFlush(any());
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

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));

        // Act
        Set<User> retrievedList = taskListService.getAllCollaborators(taskListId, ownerUser);

        // Assert
        assertThat(retrievedList)
                .hasSize(2)
                .containsExactlyInAnyOrder(collaborators.get(0), collaborators.get(1));

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void getAllCollaborators_whenUserIsUnauthorised_shouldThrowAccessDeniedException(){
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

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.getAllCollaborators(taskListId, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, unauthorisedUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void getAllCollaborators_whenTaskListIdIsNonexistent_shouldThrowListNotFoundException(){
        // Arrange
        long invalidId = 999L;
        when(taskListRepo.findByIdAndUserHasAccess(invalidId, ownerUser)).thenReturn(Optional.empty());

        // Act
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.getAllCollaborators(invalidId, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(invalidId, ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void getAllCollaborators_whenTaskListHasNoCollaborators_shouldReturnEmptyList(){
        // Arrange
        long taskListId = 1L;
        TaskList taskList = new TaskList();
        taskList.setId(taskListId);
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        when(taskListRepo.findByIdAndUserHasAccess(taskListId, ownerUser)).thenReturn(Optional.of(taskList));

        // Act
        Set<User> retrievedList = taskListService.getAllCollaborators(taskListId, ownerUser);

        // Assert
        assertThat(retrievedList).isEmpty();

        // Verify
        verify(taskListRepo, times(1)).findByIdAndUserHasAccess(taskListId, ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
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

        when(taskListRepo.findByIdAndUserHasAccess(id, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskListRepo.findByTitleAndOwner(updatedTitle, ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList resultTaskList = taskListService.updateTaskListTitle(id, dto, ownerUser);

        // Assert
        assertThat(resultTaskList).isNotNull();
        assertThat(resultTaskList.getTitle()).isEqualTo(updatedTitle);
        assertThat(resultTaskList.getOwner().getUsername()).isEqualTo(ownerUser.getUsername());

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findByIdAndUserHasAccess(id, ownerUser);
        inorder.verify(taskListRepo).findByTitleAndOwner(updatedTitle, ownerUser);
        inorder.verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void updateTaskListTitle_whenUserIsCollaborator_shouldThrowAccessDeniedException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaboratorUser);
        taskList.setId(id);

        String updatedTitle = "Shoe wishlist";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findByIdAndUserHasAccess(id, collaboratorUser)).thenReturn(Optional.of(taskList));

        // Act & Assert
        AccessDeniedException exception = Assertions.assertThrows(AccessDeniedException.class,
                () -> taskListService.updateTaskListTitle(id, dto, collaboratorUser));
        assertThat(exception.getMessage()).isEqualTo("Only the list owner can update the list title!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findByIdAndUserHasAccess(id, collaboratorUser);
        inorder.verify(taskListRepo, never()).findByTitleAndOwner(any(), any());
        inorder.verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskListTitle_UserIsUnauthorised_shouldThrowAccessDeniedException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        String updatedTitle = "Shoe wishlist";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findByIdAndUserHasAccess(id, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.updateTaskListTitle(id, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findByIdAndUserHasAccess(id, unauthorisedUser);
        inorder.verify(taskListRepo, never()).findByTitleAndOwner(any(), any());
        inorder.verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskListTitle_withDuplicateTitle_shouldThrowTitleAlreadyExistsException(){
        // Arrange
        long id = 1L;
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.setId(id);

        TaskList otherList = new TaskList();
        otherList.setTitle("Grocery Shopping List");
        otherList.setOwner(ownerUser);
        otherList.setId(2L);

        String updatedTitle = "Grocery Shopping List";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findByIdAndUserHasAccess(id, ownerUser)).thenReturn(Optional.of(taskList));
        when(taskListRepo.findByTitleAndOwner(updatedTitle, ownerUser)).thenReturn(Optional.of(otherList));

        // Act & Assert
        TitleAlreadyExistsException exception = Assertions.assertThrows(TitleAlreadyExistsException.class,
                () -> taskListService.updateTaskListTitle(id, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("You already have a list with this title!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findByIdAndUserHasAccess(id, ownerUser);
        inorder.verify(taskListRepo).findByTitleAndOwner(updatedTitle, ownerUser);
        inorder.verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void updateTaskListTitle_withNonExistentListId_shouldThrowListNotFoundException(){
        // Arrange
        long invalidTaskListId = 999L;

        String updatedTitle = "Grocery Shopping List";
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);

        when(taskListRepo.findByIdAndUserHasAccess(invalidTaskListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.updateTaskListTitle(invalidTaskListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        InOrder inorder = inOrder(taskListRepo);
        inorder.verify(taskListRepo).findByIdAndUserHasAccess(invalidTaskListId, ownerUser);
        inorder.verify(taskListRepo, never()).findByTitleAndOwner(any(), any());
        inorder.verify(taskListRepo, never()).saveAndFlush(any());
    }

     @Test
    void duplicateList_asOwner_withDefaultTitle_success(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskItem task1 = new TaskItem();
        task1.setId(1L);
        task1.setDescription("Buy milk");
        task1.setCompleted(true);
        task1.setTaskList(originalList);

        TaskItem task2 = new TaskItem();
        task2.setId(2L);
        task2.setDescription("Buy bread");
        task2.setCompleted(false);
        task2.setTaskList(originalList);

        originalList.getTasks().add(task1);
        originalList.getTasks().add(task2);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO(); // empty DTO - use default title

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> {
            TaskList savedList = invocation.getArgument(0);
            savedList.setId(2L);
            return savedList;
        });

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList).isNotNull();
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy)");
        assertThat(duplicatedList.getOwner()).isEqualTo(ownerUser);
        assertThat(duplicatedList.getTasks()).hasSize(2);
        assertThat(duplicatedList.getTasks().stream().allMatch(task -> !task.getCompleted())).isTrue();
        assertThat(duplicatedList.getCollaborators()).isEmpty();
        assertThat(duplicatedList.getId()).isNotEqualTo(originalListId);

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, ownerUser);
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy)", ownerUser);
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_asCollaborator_becomesOwner(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shared List");
        originalList.setOwner(ownerUser);
        originalList.getCollaborators().add(collaboratorUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, collaboratorUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shared List (Copy)", collaboratorUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> {
            TaskList savedList = invocation.getArgument(0);
            savedList.setId(2L);
            return savedList;
        });

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, collaboratorUser);

        // Assert
        assertThat(duplicatedList).isNotNull();
        assertThat(duplicatedList.getOwner()).isEqualTo(collaboratorUser);
        assertThat(duplicatedList.getOwner()).isNotEqualTo(ownerUser);
        assertThat(duplicatedList.getCollaborators()).isEmpty();

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, collaboratorUser);
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_withCustomTitle_success(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("My Custom List");

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("My Custom List", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> {
            TaskList savedList = invocation.getArgument(0);
            savedList.setId(2L);
            return savedList;
        });

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList).isNotNull();
        assertThat(duplicatedList.getTitle()).isEqualTo("My Custom List");
        assertThat(duplicatedList.getOwner()).isEqualTo(ownerUser);

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, ownerUser);
        verify(taskListRepo).findByTitleAndOwner("My Custom List", ownerUser);
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_emptyList_success(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Empty List");
        originalList.setOwner(ownerUser);
        originalList.setTasks(new ArrayList<>());

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Empty List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> {
            TaskList savedList = invocation.getArgument(0);
            savedList.setId(2L);
            return savedList;
        });

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList).isNotNull();
        assertThat(duplicatedList.getTasks()).isEmpty();
        assertThat(duplicatedList.getTitle()).isEqualTo("Empty List (Copy)");

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, ownerUser);
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_tasksResetToIncomplete(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Task List");
        originalList.setOwner(ownerUser);

        TaskItem completedTask = new TaskItem();
        completedTask.setId(1L);
        completedTask.setDescription("Completed Task");
        completedTask.setCompleted(true);
        completedTask.setTaskList(originalList);

        TaskItem incompleteTask = new TaskItem();
        incompleteTask.setId(2L);
        incompleteTask.setDescription("Incomplete Task");
        incompleteTask.setCompleted(false);
        incompleteTask.setTaskList(originalList);

        originalList.getTasks().add(completedTask);
        originalList.getTasks().add(incompleteTask);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Task List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTasks()).hasSize(2);
        assertThat(duplicatedList.getTasks().stream().allMatch(task -> !task.getCompleted())).isTrue();
        assertThat(duplicatedList.getTasks().stream().map(TaskItem::getDescription))
                .containsExactlyInAnyOrder("Completed Task", "Incomplete Task");

        // Verify
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_collaboratorsNotCopied(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shared List");
        originalList.setOwner(ownerUser);
        originalList.getCollaborators().add(collaboratorUser);
        originalList.getCollaborators().add(unauthorisedUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shared List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getCollaborators()).isEmpty();
        assertThat(originalList.getCollaborators()).hasSize(2); // Original unchanged

        // Verify
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_generatesCorrectTitleWithCopy(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy)");

        // Verify
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy)", ownerUser);
    }

    @Test
    void duplicateList_generatesCorrectTitleWithCopy2(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List (Copy)");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy 2)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy 2)");

        // Verify
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy 2)", ownerUser);
    }

    @Test
    void duplicateList_incrementsCopyNumber(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List (Copy 5)");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy 6)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy 6)");

        // Verify
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy 6)", ownerUser);
    }

    @Test
    void duplicateList_handlesEdgeCaseTitles(){
        // Arrange - Test with very long title
        long originalListId = 1L;
        String longTitle = "A".repeat(200);
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle(longTitle);
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner(longTitle + " (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo(longTitle + " (Copy)");

        // Verify
        verify(taskListRepo).saveAndFlush(any(TaskList.class));
    }

    @Test
    void duplicateList_listNotFound_throwsListNotFoundException(){
        // Arrange
        long invalidListId = 999L;
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(invalidListId, ownerUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.duplicateList(invalidListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(invalidListId, ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateList_userHasNoAccess_throwsListNotFoundException(){
        // Arrange
        long listId = 1L;
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(listId, unauthorisedUser)).thenReturn(Optional.empty());

        // Act & Assert
        ListNotFoundException exception = Assertions.assertThrows(ListNotFoundException.class,
                () -> taskListService.duplicateList(listId, dto, unauthorisedUser));
        assertThat(exception.getMessage()).isEqualTo("List not found or you don't have access to it!");

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(listId, unauthorisedUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateList_titleAlreadyExists_throwsTitleAlreadyExistsException(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskList existingList = new TaskList();
        existingList.setId(2L);
        existingList.setTitle("Shopping List (Copy)");
        existingList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy)", ownerUser)).thenReturn(Optional.of(existingList));

        // Act & Assert
        TitleAlreadyExistsException exception = Assertions.assertThrows(TitleAlreadyExistsException.class,
                () -> taskListService.duplicateList(originalListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("You already have a list with this title!");

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, ownerUser);
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy)", ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateList_customTitleAlreadyExists_throwsTitleAlreadyExistsException(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskList existingList = new TaskList();
        existingList.setId(2L);
        existingList.setTitle("My Custom Title");
        existingList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("My Custom Title");

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("My Custom Title", ownerUser)).thenReturn(Optional.of(existingList));

        // Act & Assert
        TitleAlreadyExistsException exception = Assertions.assertThrows(TitleAlreadyExistsException.class,
                () -> taskListService.duplicateList(originalListId, dto, ownerUser));
        assertThat(exception.getMessage()).isEqualTo("You already have a list with this title!");

        // Verify
        verify(taskListRepo).findByIdAndUserHasAccess(originalListId, ownerUser);
        verify(taskListRepo).findByTitleAndOwner("My Custom Title", ownerUser);
        verify(taskListRepo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateList_emptyCustomTitle_usesAutoGenerated(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("");

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy)");

        // Verify
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy)", ownerUser);
    }

    @Test
    void duplicateList_whitespaceOnlyTitle_usesAutoGenerated(){
        // Arrange
        long originalListId = 1L;
        TaskList originalList = new TaskList();
        originalList.setId(originalListId);
        originalList.setTitle("Shopping List");
        originalList.setOwner(ownerUser);

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("   ");

        when(taskListRepo.findByIdAndUserHasAccess(originalListId, ownerUser)).thenReturn(Optional.of(originalList));
        when(taskListRepo.findByTitleAndOwner("Shopping List (Copy)", ownerUser)).thenReturn(Optional.empty());
        when(taskListRepo.saveAndFlush(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskList duplicatedList = taskListService.duplicateList(originalListId, dto, ownerUser);

        // Assert
        assertThat(duplicatedList.getTitle()).isEqualTo("Shopping List (Copy)");

        // Verify
        verify(taskListRepo).findByTitleAndOwner("Shopping List (Copy)", ownerUser);
    }
}
