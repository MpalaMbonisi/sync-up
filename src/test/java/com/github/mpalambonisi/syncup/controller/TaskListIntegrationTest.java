package com.github.mpalambonisi.syncup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.dto.request.AddCollaboratorsRequestDTO;
import com.github.mpalambonisi.syncup.dto.request.RemoveCollaboratorRequestDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
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
import org.springframework.security.test.context.support.WithMockUser;
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

    private TaskList createTaskListAndSave(String title){
        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setOwner(ownerUser);

        return taskListRepository.save(taskList);
    }

    @Test
    void createList_asOwner_shouldReturn201CreatedAndList() throws Exception{
        // Arrange
        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Grocery Shopping List")
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
        Optional<TaskList> savedList = taskListRepository.findByTitle(dto.getTitle());
        assertThat(savedList).isPresent();
        assertThat(savedList.get().getOwner().getUsername()).isEqualTo("mbonisimpala");
    }

    @Test
    void createList_asUnauthenticatedUser_shouldReturn401Unauthorized() throws Exception{
        // Arrange
        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Grocery Shopping List")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitle(dto.getTitle());
        assertThat(savedList).isEmpty();
    }

    @Test
    void createList_withDuplicateTitle_shouldReturn409Conflict() throws Exception{
        // Arrange
        String title = "Grocery Shopping List";
        createTaskListAndSave(title);
        TaskListCreateDTO dto = new TaskListCreateDTO(title);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Title is already being used!"));

        // Post-Action Verification
        assertThat(taskListRepository.count()).isEqualTo(1);

    }

    @ParameterizedTest
    @WithMockUser(username = "mbonisimpala")
    @CsvSource({"'', 'Title cannot be empty.'", "'   ', 'Title cannot be blank.'"})
    void createList_withBlankTitle_shouldReturn400BadRequest(String invalidTitle, String expectedErrorMessage) throws Exception{
        // Arrange
        TaskListCreateDTO dto = new TaskListCreateDTO(invalidTitle);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedErrorMessage)));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitle(dto.getTitle());
        assertThat(savedList).isEmpty();
    }

    @Test
    void getAllLists_asAuthenticatedUser_shouldReturn200OkAndLists() throws Exception{
        // Arrange
        createTaskListAndSave("Grocery Shopping List");
        createTaskListAndSave("Clothing Wishlist");

        // Assert & Act
        mockMvc.perform(MockMvcRequestBuilders.get("/list/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].title").value("Grocery Shopping List"))
                .andExpect(jsonPath("$[1].owner").value(ownerUser.getUsername()))
                .andExpect(jsonPath("$[0].title").value("Clothing Wishlist"))
                .andExpect(jsonPath("$[0].owner").value(ownerUser.getUsername()));
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
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Grocery Shopping List"))
                .andExpect(jsonPath("$.owner").value("mbonisimpala"));
    }

    @Test
    void getListById_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));
    }

    @Test
    void getListById_asCollaboratorUser_shouldReturn200AndList() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "VeryStrongPassword1234");

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        TaskList savedTaskList = taskListRepository.save(taskList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Grocery Shopping List"))
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
                .andExpect(jsonPath("$.message").value("List not found!"));

    }

    @Test
    void getListById_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())) // skip authentication
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void deleteListById_asOwner_shouldReturn204NoContent() throws Exception{
        // Arrange
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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
    void deleteListById_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");

        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access to delete this list!"));

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
                .andExpect(jsonPath("$.message").value("List not found!"));
    }

    @Test
    void deleteListById_whenUserIsCollaborator_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "VeryStrongPassword1234");

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        TaskList savedTaskList = taskListRepository.save(taskList);
        long taskListId = savedTaskList.getId();
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access to delete this list!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findById(taskListId);
        assertThat(savedList.isPresent()).isTrue();
    }

    @Test
    void deleteListById_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");

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

        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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
    void addCollaboratorsByUsername_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");

        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
        long taskListId = savedTaskList.getId();

        Set<String> collaboratorsList = new HashSet<>();
        collaboratorsList.add("johnsmith");
        AddCollaboratorsRequestDTO dto = new AddCollaboratorsRequestDTO(collaboratorsList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/collaborator/add")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to add collaborators!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).isEmpty();
    }

    @Test
    void addCollaboratorsByUsername_withNonexistentCollaboratorUsername_shouldReturn404NotFound() throws Exception{
        // Arrange
        String invalidUsername = "karensanders"; // non-existent

        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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
                .andExpect(jsonPath("$.message").value("Collaborator username not found!"));

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
                .andExpect(jsonPath("$.message").value("List not found!"));

        // Post-Action Verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(invalidTaskListId);
        assertThat(retrievedTaskList.isEmpty()).isTrue();
    }

    @Test
    void addCollaboratorsByUsername_withEmptyCollaboratorsList_shouldReturn400BadRequest() throws Exception{
        // Arrange
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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
        TaskList taskList = new TaskList();
        taskList.setOwner(ownerUser);
        taskList.setTitle("Grocery Shopping List");
        taskList.getCollaborators().add(collaborator);

        TaskList savedList = taskListRepository.save(taskList);
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
    void removeCollaboratorByUsername_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "ReallyStrongPassword1234");
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");
        TaskList taskList = new TaskList();
        taskList.setOwner(ownerUser);
        taskList.setTitle("Grocery Shopping List");
        taskList.getCollaborators().add(collaborator);

        TaskList savedList = taskListRepository.save(taskList);
        long taskListId = savedList.getId();
        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO("johnsmith");

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to remove collaborators!"));


        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(taskListId);
        assertThat(retrievedTaskList.isPresent()).isTrue();
        assertThat(retrievedTaskList.get().getCollaborators()).hasSize(1);
    }

    @Test
    void removeCollaboratorByUsername_withNonexistentCollaboratorUsername_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedList = createTaskListAndSave("Grocery Shopping List");
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
                .andExpect(jsonPath("$.message").value("List not found!"));


        // Post-Action verification
        Optional<TaskList> retrievedTaskList = taskListRepository.findById(invalidListId);
        assertThat(retrievedTaskList.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @WithMockUser("mbonisimpala")
    @CsvSource({"'', 'Collaborator username cannot be empty.'", "'   ', 'Collaborator username cannot be blank.'"})
    void removeCollaboratorByUsername_withEmptyOrBlankUsername_shouldReturn400BadRequest(String invalidUsername, String expectedMessage) throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", "StrongPassword1234");
        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator);

        TaskList savedTaskList = taskListRepository.save(taskList);
        long taskListId = savedTaskList.getId();

        RemoveCollaboratorRequestDTO dto = new RemoveCollaboratorRequestDTO(invalidUsername);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/list/" + taskListId + "/collaborator/remove")
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
        TaskList taskList = new TaskList();
        taskList.setOwner(ownerUser);
        taskList.setTitle("Grocery Shopping List");
        taskList.getCollaborators().add(collaborator);

        TaskList savedList = taskListRepository.save(taskList);
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

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator01);
        taskList.getCollaborators().add(collaborator02);

        TaskList savedTaskList = taskListRepository.save(taskList);
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all")
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$", hasItems("nicolencube", "johnsmith")));
    }

    @Test
    void getAllCollaborators_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User collaborator01 = createUserAndSave("John", "Smith", "StrongPassword1234");
        User collaborator02 = createUserAndSave("Nicole", "Ncube", "ReallyStrongPassword1234");
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", "VeryStrongPassword1234");

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator01);
        taskList.getCollaborators().add(collaborator02);

        TaskList savedTaskList = taskListRepository.save(taskList);
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to retrieve all collaborators!"));
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
                .andExpect(jsonPath("$.message").value("List not found!"));
    }

    @Test
    void getAllCollaborators_withEmptyCollaboratorsList_shouldReturn200() throws Exception{
        // Arrange
        TaskList savedTaskList = createTaskListAndSave("Grocery Shopping List");
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

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);
        taskList.getCollaborators().add(collaborator01);
        taskList.getCollaborators().add(collaborator02);

        TaskList savedTaskList = taskListRepository.save(taskList);
        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + taskListId + "/collaborator/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

}
