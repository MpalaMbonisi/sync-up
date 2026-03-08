package com.github.mpalambonisi.syncup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListDuplicateDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskListTitleUpdateDTO;
import com.github.mpalambonisi.syncup.model.TaskItem;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskItemRepository;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@Transactional // To ensure each test run in a transaction is rolled back
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TaskListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskListRepository taskListRepository;
    @Autowired
    private TaskItemRepository taskItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder encoder;
    private final User ownerUser = new User();

    // Configure Test containers
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeEach
    void setUp(){
        ownerUser.setUsername("mbonisimpala");
        ownerUser.setFirstName("Mbonisi");
        ownerUser.setLastName("Mpala");
        ownerUser.setEmail("mbonisim12@gmail.com");
        ownerUser.setPassword(encoder.encode("StrongPassword1234"));
        userRepository.save(ownerUser);
    }

    private User createUserAndSave(String firstName, String lastName, String password){
        String username = (firstName + lastName).toLowerCase();
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(username + "@yahoo.com");
        user.setPassword(encoder.encode(password));
        return userRepository.save(user);
    }

    private TaskList createTaskListAndSave(String title, List<User> collaborators){
        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setOwner(ownerUser);
        if(!collaborators.isEmpty())
            taskList.getCollaborators().addAll(collaborators);

        return taskListRepository.save(taskList);
    }

    private TaskList assertValidTaskListCreation(TaskList savedTaskList, User expectedUser, List<User> expectedCollaborators){
        assertThat(savedTaskList).isNotNull();
        assertThat(savedTaskList.getId()).isNotNull();
        assertThat(savedTaskList.getOwner()).isEqualTo(expectedUser);
        assertThat(savedTaskList.getTitle()).isEqualTo("Shopping List");
        if(!expectedCollaborators.isEmpty()){
            assertThat(savedTaskList.getCollaborators()).containsAll(expectedCollaborators);
        }
        return savedTaskList;
    }

    private TaskList assertValidTaskListCreation(String taskTitleExpected, TaskList savedTaskList, User expectedUser, List<User> expectedCollaborators){
        assertThat(savedTaskList).isNotNull();
        assertThat(savedTaskList.getId()).isNotNull();
        assertThat(savedTaskList.getOwner()).isEqualTo(expectedUser);
        assertThat(savedTaskList.getTitle()).isEqualTo(taskTitleExpected);
        if(!expectedCollaborators.isEmpty()){
            assertThat(savedTaskList.getCollaborators()).containsAll(expectedCollaborators);
        }
        return savedTaskList;
    }

    @Test
    void createList_asOwner_shouldReturn201CreatedAndList() throws Exception{
        // Arrange
        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Shopping List")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.title").value(dto.getTitle()))
                        .andExpect(jsonPath("$.owner").value("mbonisimpala"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitleAndOwner(dto.getTitle(), ownerUser);
        assertThat(savedList).isPresent();
        assertThat(savedList.get().getOwner().getUsername()).isEqualTo("mbonisimpala");
    }

    @Test
    void createList_asUnauthenticatedUser_shouldReturn401Unauthorized() throws Exception{
        // Arrange
        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Shopping List")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitleAndOwner(dto.getTitle(), ownerUser);
        assertThat(savedList).isEmpty();
    }

    @Test
    void createList_withDuplicateTitle_shouldReturn409Conflict() throws Exception{
        // Arrange
        String title = "Shopping List";
        assertValidTaskListCreation(
                createTaskListAndSave(title, List.of()),
                ownerUser,
                List.of());
        TaskListCreateDTO dto = new TaskListCreateDTO(title);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a list with this title!"));

        // Post-Action Verification
        assertThat(taskListRepository.count()).isEqualTo(1);

    }

    @ParameterizedTest
    @CsvSource({"'', 'Title cannot be empty.'", "'   ', 'Title cannot be blank.'"})
    void createList_withBlankTitle_shouldReturn400BadRequest(String invalidTitle, String expectedErrorMessage) throws Exception{
        // Arrange
        TaskListCreateDTO dto = new TaskListCreateDTO(invalidTitle);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedErrorMessage)));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitleAndOwner(dto.getTitle(), ownerUser);
        assertThat(savedList).isEmpty();
    }

    @Test
    void getAllLists_asAuthenticatedUser_shouldReturn200OkAndLists() throws Exception{
        // Arrange
        assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        TaskList taskList02 = createTaskListAndSave("Clothing Wishlist", List.of());
        assertThat(taskList02).isNotNull();
        assertThat(taskList02.getId()).isNotNull();
        assertThat(taskList02.getOwner()).isEqualTo(ownerUser);
        assertThat(taskList02.getTitle()).isEqualTo("Clothing Wishlist");
        assertThat(taskList02.getCollaborators()).isEmpty();

        // Assert & Act
        mockMvc.perform(MockMvcRequestBuilders.get("/list/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Shopping List"))
                .andExpect(jsonPath("$[0].owner").value(ownerUser.getUsername()))
                .andExpect(jsonPath("$[1].title").value("Clothing Wishlist"))
                .andExpect(jsonPath("$[1].owner").value(ownerUser.getUsername()));
    }

    @Test
    void getAllLists_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange - No test data needed, as the request should be blocked before it hits the controller

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void getListById_asOwner_shouldReturn200AndList() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Shopping List"))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"));
    }

    @Test
    void getListById_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void getListById_asCollaboratorUser_shouldReturn200AndList() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "VeryStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Shopping List"))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"))
                .andExpect(jsonPath("$.collaborators.length()").value(1))
                .andExpect(jsonPath("$.collaborators[0]").value("johnsmith"));

    }

    @Test
    void getListById_withNonExistentListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidId = 999L;

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + invalidId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

    }

    @Test
    void getListById_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())) // skip authentication
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void deleteListById_asOwner_shouldReturn204NoContent() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId)
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findById(taskListId);
        assertThat(savedList).isEmpty();
    }

    @Test
    void deleteListById_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findById(taskListId);
        assertThat(savedList.isPresent()).isTrue();
    }

    @Test
    void deleteListById_withNonexistentListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidId = 999L;

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + invalidId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void deleteListById_whenUserIsCollaborator_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "VeryStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the list owner can delete this list!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findById(taskListId);
        assertThat(savedList.isPresent()).isTrue();
    }

    @Test
    void deleteListById_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + savedTaskList.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void addCollaboratorsByUsername_asOwner_shouldReturn200() throws Exception{
        // Arrange
        User collaborator01 = createUserAndSave("John", "Smith", "VeryStrongPassword1234");
        User collaborator02 = createUserAndSave("Nicole", "Ncube", "ReallyStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add(collaborator01.getUsername());
        collaboratorsList.add(collaborator02.getUsername());
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$",
                        hasItems("nicolencube", "johnsmith")));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).hasSize(2);
    }

    @Test
    void addCollaboratorsByUsername_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add("johnsmith");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void addCollaboratorsByUsername_withNonexistentCollaboratorUsername_shouldReturn404NotFound() throws Exception{
        // Arrange
        String invalidUsername = "karensanders"; // non-existent

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add(invalidUsername);
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Collaborator username 'karensanders' not found!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void addCollaboratorsByUsername_withNonexistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidTaskListId = 999L;

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add("johnsmith");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + invalidTaskListId + "/collaborator/add")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(invalidTaskListId);
        assertThat(retrievedTaskList.isEmpty()).isTrue();
    }

    @Test
    void addCollaboratorsByUsername_withEmptyCollaboratorsList_shouldReturn400BadRequest() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(new HashSet<>());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Please provide at least one collaborator."));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void addCollaboratorsByUsername_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        User collaborator01 = createUserAndSave("John", "Smith", "VeryStrongPassword1234");
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add(collaborator01.getUsername());
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void removeCollaboratorByUsername_asOwner_shouldReturn204NoContent() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");
        TaskList savedList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));

        long taskListId = savedList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void removeCollaboratorByUsername_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");

        TaskList savedList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));
        long taskListId = savedList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));


        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).hasSize(1);
    }

    @Test
    void removeCollaboratorByUsername_withNonexistentCollaboratorUsername_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("karensanders"); // non-existent username

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Collaborator username not found!"));


        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void removeCollaboratorByUsername_withNonexistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidListId = 999L;
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + invalidListId + "/collaborator/remove")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));


        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(invalidListId);
        assertThat(retrievedTaskList.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"'', 'Collaborator username cannot be empty.'", "'   ', 'Collaborator username cannot be blank.'"})
    void removeCollaboratorByUsername_withEmptyOrBlankUsername_shouldReturn400BadRequest(String invalidUsername, String expectedMessage) throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));
        long taskListId = savedTaskList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(invalidUsername);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedMessage)));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).hasSize(1);
    }

    @Test
    void removeCollaboratorByUsername_asUnauthenticatedUser_shouldReturn200Ok() throws Exception{
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");
        TaskList savedList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser,
                List.of(collaborator));
        long taskListId = savedList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).hasSize(1);
    }

    @Test
    void getAllCollaborators_asOwner_shouldReturn200() throws Exception{
        // Arrange
        User collaborator01 = createUserAndSave("John", "Smith", "StrongPassword1234");
        User collaborator02 = createUserAndSave("Nicole", "Ncube", "ReallyStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator01, collaborator02)),
                ownerUser,
                List.of(collaborator01, collaborator02));
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all")
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$", hasItems("nicolencube", "johnsmith")));
    }

    @Test
    void getAllCollaborators_asUnauthorisedUser_shouldReturn40NotFound() throws Exception{
        // Arrange
        User collaborator01 = createUserAndSave("John", "Smith", "StrongPassword1234");
        User collaborator02 = createUserAndSave("Nicole", "Ncube", "ReallyStrongPassword1234");
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "VeryStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator01, collaborator02)),
                ownerUser,
                List.of(collaborator01, collaborator02));
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void getAllCollaborators_withNonexistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidListId = 999L;

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + invalidListId + "/collaborator/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void getAllCollaborators_withEmptyCollaboratorsList_shouldReturn200() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllCollaborators_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        User collaborator01 = createUserAndSave("John", "Smith", "StrongPassword1234");
        User collaborator02 = createUserAndSave("Nicole", "Ncube", "ReallyStrongPassword1234");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator01, collaborator02)),
                ownerUser,
                List.of(collaborator01, collaborator02));
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void updateTaskListTitle_asOwner_shouldReturn200OkAndUpdatedTaskList() throws Exception{
        // Arrange
        String updatedTitle = "Wishlist";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskListId))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"))
                .andExpect(jsonPath("$.collaborators").isEmpty());

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(updatedTitle);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskListTitle_asCollaborator_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");

        String updatedTitle = "Wishlist";
        String originalTitle = "Shopping List";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(originalTitle, List.of(collaborator)),
                ownerUser,
                List.of(collaborator));
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the list owner can update the list title!"));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(originalTitle);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskListTitle_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "VeryStrongPassword1234");

        String updatedTitle = "Wishlist";
        String originalTitle = "Shopping List";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(originalTitle, List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(originalTitle);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskListTitle_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        String updatedTitle = "Wishlist";
        String originalTitle = "Shopping List";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(updatedTitle);
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(originalTitle, List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(originalTitle);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskListTitle_withDuplicateTitle_shouldReturnConflict409() throws Exception{
        // Arrange
        String uniqueTitle = "Shopping List";
        String duplicateTitle = "Wishlist";

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(uniqueTitle, List.of()),
                ownerUser, List.of());

        // Second list that has the title conflict
        TaskList existingTaskList = createTaskListAndSave(duplicateTitle, List.of());
        assertThat(existingTaskList).isNotNull();
        assertThat(existingTaskList.getId()).isNotNull();
        assertThat(existingTaskList.getOwner()).isEqualTo(ownerUser);
        assertThat(existingTaskList.getTitle()).isEqualTo(duplicateTitle);
        assertThat(existingTaskList.getCollaborators()).isEmpty();

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(duplicateTitle);
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();


        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a list with this title!"));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(uniqueTitle); // verify that the title was NOT updated
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @ParameterizedTest
    @CsvSource({"'', 'Title cannot be empty.'", "'   ', 'Title cannot be blank.'"})
    void updateTaskListTitle_withEmptyOrBlankTitle_shouldReturn400BadRequest(String invalidTitle, String expectedMessage) throws Exception{
        // Arrange
        String originalTitle = "Shopping List";

        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO(invalidTitle);
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(originalTitle, List.of()),
                ownerUser,
                List.of());
        long taskListId = savedTaskList.getId();
        long countBefore = taskListRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  taskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedMessage)));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getTitle()).isEqualTo(originalTitle);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskListTitle_withNonExistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidTaskListId = 999L; // non-existent task list ID
        TaskListTitleUpdateDTO dto = new TaskListTitleUpdateDTO("Wishlist");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" +  invalidTaskListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-action verification
        long countAfter = taskListRepository.count();
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(invalidTaskListId);
        assertThat(retrievedTaskList.isEmpty()).isTrue();
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    void duplicateList_asOwner_withDefaultTitle_returns201Created() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        TaskItem task1 = new TaskItem();
        task1.setDescription("Buy milk");
        task1.setCompleted(true);
        task1.setTaskList(originalList);
        taskItemRepository.save(task1);
        originalList.getTasks().add(task1);

        TaskItem task2 = new TaskItem();
        task2.setDescription("Buy bread");
        task2.setCompleted(false);
        task2.setTaskList(originalList);
        taskItemRepository.save(task2);
        originalList.getTasks().add(task2);

        long originalListId = originalList.getId();
        long countBefore = taskListRepository.count();

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO(); // empty DTO for default title

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"))
                .andExpect(jsonPath("$.collaborators").isEmpty())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(2));

        // Post-Action Verification
        long countAfter = taskListRepository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

     @Test
    void duplicateList_asCollaborator_becomesOwner() throws Exception {
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "VeryStrongPassword1234");

        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of(collaborator)),
                ownerUser, List.of(collaborator));

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("johnsmith"))
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"))
                .andExpect(jsonPath("$.collaborators").isEmpty());
    }

    @Test
    void duplicateList_withCustomTitle_returns201Created() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("My Custom List");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Custom List"))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"));
    }

    @Test
    void duplicateList_tasksDeepCopied() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        TaskItem task1 = new TaskItem();
        task1.setDescription("Original Task 1");
        task1.setCompleted(false);
        task1.setTaskList(originalList);
        taskItemRepository.save(task1);
        originalList.getTasks().add(task1);

        TaskItem task2 = new TaskItem();
        task2.setDescription("Original Task 2");
        task2.setCompleted(false);
        task2.setTaskList(originalList);
        taskItemRepository.save(task2);
        originalList.getTasks().add(task2);

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act - Duplicate the list
        String responseJson = mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract duplicate list ID from response
        Long duplicateListId = objectMapper.readTree(responseJson).get("id").asLong();

        // Assert - Verify tasks are separate instances
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + duplicateListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.tasks[0].id").isNumber())
                .andExpect(jsonPath("$.tasks[0].description").value("Original Task 1"))
                .andExpect(jsonPath("$.tasks[1].description").value("Original Task 2"));
    }

    @Test
    void duplicateList_appearsInGetAllLists() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act - Duplicate the list
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Assert - Check GET /list/all includes both lists
        mockMvc.perform(MockMvcRequestBuilders.get("/list/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].title").value(hasItems("Shopping List", "Shopping List (Copy)")));
    }

    @Test
    void duplicateList_autoGeneratesCopyTitle() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"));
    }

    @Test
    void duplicateList_handlesExistingCopy() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                "Shopping List (Copy)",
                createTaskListAndSave("Shopping List (Copy)", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy 2)"));
    }

    @Test
    void duplicateList_incrementsExistingNumber() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                "Shopping List (Copy 3)",
                createTaskListAndSave("Shopping List (Copy 3)", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy 4)"));
    }

    @Test
    void duplicateList_allTasksResetToIncomplete() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        // Create 2 completed and 2 incomplete tasks
        TaskItem completed1 = new TaskItem();
        completed1.setDescription("Completed Task 1");
        completed1.setCompleted(true);
        completed1.setTaskList(originalList);
        taskItemRepository.save(completed1);
        originalList.getTasks().add(completed1);

        TaskItem completed2 = new TaskItem();
        completed2.setDescription("Completed Task 2");
        completed2.setCompleted(true);
        completed2.setTaskList(originalList);
        taskItemRepository.save(completed2);
        originalList.getTasks().add(completed2);

        TaskItem incomplete1 = new TaskItem();
        incomplete1.setDescription("Incomplete Task 1");
        incomplete1.setCompleted(false);
        incomplete1.setTaskList(originalList);
        taskItemRepository.save(incomplete1);
        originalList.getTasks().add(incomplete1);

        TaskItem incomplete2 = new TaskItem();
        incomplete2.setDescription("Incomplete Task 2");
        incomplete2.setCompleted(false);
        incomplete2.setTaskList(originalList);
        taskItemRepository.save(incomplete2);
        originalList.getTasks().add(incomplete2);

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(4))
                .andExpect(jsonPath("$.tasks[0].completed").value(false))
                .andExpect(jsonPath("$.tasks[1].completed").value(false))
                .andExpect(jsonPath("$.tasks[2].completed").value(false))
                .andExpect(jsonPath("$.tasks[3].completed").value(false));
    }

    @Test
    void duplicateList_emptyListWorks() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                "Empty List",
                createTaskListAndSave("Empty List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Empty List (Copy)"))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks").isEmpty());
    }

     @Test
    void duplicateList_manyTasksCopiedCorrectly() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                "Large List",
                createTaskListAndSave("Large List", List.of()),
                ownerUser, List.of());

        // Create 50 tasks
        for (int i = 1; i <= 50; i++) {
            TaskItem task = new TaskItem();
            task.setDescription("Task " + i);
            task.setCompleted(i % 2 == 0); // Half completed, half not
            task.setTaskList(originalList);
            taskItemRepository.save(task);
            originalList.getTasks().add(task);
        }

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(50));
    }

    @Test
    void duplicateList_noAccess_returns404() throws Exception {
        // Arrange
        User unauthorizedUser = createUserAndSave("Karen", "Sanders", "VeryStrongPassword1234");

        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorizedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void duplicateList_titleConflict_returns409() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        // Create a list with the title that would be auto-generated
        TaskList conflictingList = createTaskListAndSave("Shopping List (Copy)", List.of());
        assertThat(conflictingList).isNotNull();

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a list with this title!"));
    }

    @Test
    void duplicateList_customTitleConflict_returns409() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        TaskList existingList = createTaskListAndSave("Existing Title", List.of());
        assertThat(existingList).isNotNull();

        long originalListId = originalList.getId();

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("Existing Title");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a list with this title!"));
    }

    @Test
    void duplicateList_emptyCustomTitle_usesAutoGenerated() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"));
    }

    @Test
    void duplicateList_whitespaceOnlyTitle_usesAutoGenerated() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();

        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();
        dto.setNewTitle("   ");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"));
    }

    @Test
    void duplicateList_unauthenticated_returns401() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void duplicateList_ownerDuplicates_remainsOwner() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("mbonisimpala"));
    }

    @Test
    void duplicateList_originalUnaffected() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        TaskItem task = new TaskItem();
        task.setDescription("Original Task");
        task.setCompleted(true);
        task.setTaskList(originalList);
        taskItemRepository.save(task);
        originalList.getTasks().add(task);


        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act - Duplicate the list
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Assert - Check original list is unchanged
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + originalListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Shopping List"))
                .andExpect(jsonPath("$.tasks.length()").value(1))
                .andExpect(jsonPath("$.tasks[0].description").value("Original Task"))
                .andExpect(jsonPath("$.tasks[0].completed").value(true));
    }

    @Test
    void duplicateList_canModifyIndependently() throws Exception {
        // Arrange
        TaskList originalList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", List.of()),
                ownerUser, List.of());

        long originalListId = originalList.getId();
        TaskListDuplicateDTO dto = new TaskListDuplicateDTO();

        // Act - Duplicate the list
        String responseJson = mockMvc.perform(MockMvcRequestBuilders.post("/list/" + originalListId + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long duplicateListId = objectMapper.readTree(responseJson).get("id").asLong();

        // Modify original list title
        TaskListTitleUpdateDTO titleUpdate = new TaskListTitleUpdateDTO();
        titleUpdate.setTitle("Modified Original");

        mockMvc.perform(MockMvcRequestBuilders.patch("/list/" + originalListId + "/title/update")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(titleUpdate)))
                .andExpect(status().isOk());

        // Assert - Duplicate should be unaffected
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + duplicateListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Shopping List (Copy)"));
    }

}
