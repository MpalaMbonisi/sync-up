package com.github.mpalambonisi.syncup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void createList_asUnauthorisedUser_shouldReturn401Unauthorized() throws Exception{
        // Arrange

        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Grocery Shopping List")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication Failed! Invalid credentials!"));

        // Post-Action Verification
        Optional<TaskList> savedList = taskListRepository.findByTitle(dto.getTitle());
        assertThat(savedList).isEmpty();
    }

    @Test
    void createList_withDuplicateTitle_shouldReturn409Conflict() throws Exception{
        // Arrange
        String title = "Grocery Shopping List";

        // Create Task-list and save it to the database
        TaskList existingtaskList = new TaskList();
        existingtaskList.setTitle(title);
        existingtaskList.setOwner(ownerUser);
        taskListRepository.save(existingtaskList);

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

        TaskList taskList01 = new TaskList();
        taskList01.setOwner(ownerUser);
        taskList01.setTitle("Grocery Shopping List");

        TaskList taskList02 = new TaskList();
        taskList02.setOwner(ownerUser);
        taskList02.setTitle("Clothing Wishlist");

        List<TaskList> savedList = List.of(taskList01, taskList02);
        taskListRepository.saveAll(savedList);

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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getListById_asOwner_shouldReturn200AndList() throws Exception{
        // Arrange

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        TaskList savedTaskList = taskListRepository.save(taskList);

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
        User unauthorizedUser = new User();
        unauthorizedUser.setUsername("karensanders");
        unauthorizedUser.setFirstName("Karen");
        unauthorizedUser.setLastName("Sanders");
        unauthorizedUser.setEmail("karensanders@gmail.com");
        unauthorizedUser.setPassword(encoder.encode("ReallyStrongPassword1234"));
        userRepository.save(unauthorizedUser);

        TaskList taskList = new TaskList();
        taskList.setTitle("Grocery Shopping List");
        taskList.setOwner(ownerUser);

        TaskList savedTaskList = taskListRepository.save(taskList);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/list/" + savedTaskList.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorizedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));
    }

    @Test
    void getListById_asCollaboratorUser_shouldReturn200AndList() throws Exception{
        // Arrange
        User collaborator = new User();
        collaborator.setUsername("johnsmith");
        collaborator.setFirstName("John");
        collaborator.setLastName("Smith");
        collaborator.setEmail("johnsmith@yahoo.com");
        collaborator.setPassword(encoder.encode("VeryStrongPassword"));

        userRepository.save(collaborator);

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

}
