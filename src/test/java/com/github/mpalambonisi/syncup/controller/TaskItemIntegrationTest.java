package com.github.mpalambonisi.syncup.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.response.TaskListResponseDTO;
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
@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TaskItemIntegrationTest {
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
        user.setEmail(username + "@outlook.com");
        user.setPassword(encoder.encode(password));
        return userRepository.save(user);
    }

    private TaskList createTaskListAndSave(String title, User collaborator){
        TaskList taskList = new TaskList();
        taskList.setTitle(title);
        taskList.setOwner(ownerUser);
        if(collaborator != null) taskList.getCollaborators().add(collaborator);

        return taskListRepository.save(taskList);
    }

    private TaskItem createTaskItemAndSave(String description, TaskList taskList){
        TaskItem taskItem = new TaskItem();
        taskItem.setTaskList(taskList);
        taskItem.setDescription(description);
        taskItem.setCompleted(false);
        return taskItemRepository.save(taskItem);
    }

    private TaskList assertValidTaskListCreation(TaskList savedTaskList, User expectedUser, User expectedCollaborator){
        assertThat(savedTaskList).isNotNull();
        assertThat(savedTaskList.getId()).isNotNull();
        assertThat(savedTaskList.getOwner()).isEqualTo(expectedUser);
        assertThat(savedTaskList.getTitle()).isEqualTo("Shopping List");
        if(expectedCollaborator != null) assertThat(savedTaskList.getCollaborators()).contains(expectedCollaborator);
        return savedTaskList;
    }

    private TaskItem assertValidTaskItemCreation(TaskItem savedTaskItem, TaskList taskList){
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getDescription()).isEqualTo("Baggy Jeans");
        assertThat(savedTaskItem.isCompleted()).isFalse();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(taskList);
        return savedTaskItem;
    }

    @Test
    void createTask_asOwner_shouldReturn201CreatedAndTaskItemResponseDTO() throws Exception{
        // Arrange
        long countBefore = taskItemRepository.count();
        TaskItemCreateDTO dto = new TaskItemCreateDTO("Nike shoes");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Nike shoes"))
                .andExpect(jsonPath("$.completed").value("false"))
                .andExpect(jsonPath("$.taskListTitle").value("Shopping List"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void createTask_asCollaboratorUser_shouldReturn201CreatedAndTaskItemResponseDTO() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", encoder.encode("StrongPassword1234"));

        long countBefore = taskItemRepository.count();
        TaskItemCreateDTO dto = new TaskItemCreateDTO("Nike shoes");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", collaborator),
                ownerUser,
                collaborator);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Nike shoes"))
                .andExpect(jsonPath("$.completed").value("false"))
                .andExpect(jsonPath("$.taskListTitle").value("Shopping List"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void createTask_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword"));

        TaskItemCreateDTO dto = new TaskItemCreateDTO("Nike shoes");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(0);
    }

    @ParameterizedTest
    @WithMockUser(username = "mbonisimpala")
    @CsvSource({"'', 'Task description cannot be empty.'", "'   ', 'Task description cannot be blank.'"})
    void createTask_withEmptyOrBlankDescription_shouldReturn400BadRequest(String invalidDescription, String expectedMessage) throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaskItemCreateDTO(invalidDescription))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedMessage)));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    void createTask_withNonExistentListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        long invalidTaskListId = 999L;

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + invalidTaskListId + "/task/create")
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaskItemCreateDTO("Nike shoes"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    void createTask_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemCreateDTO("Nike shoes"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication Failed! Invalid credentials!"));
    }

    @Test
    void updateTask_asOwner_shouldReturn200OkAndTaskItemResponseDTO() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave("Shopping List", null),
                ownerUser,
                null);
        TaskItem savedTaskItem = createTaskItemAndSave("Baggy jeans", savedTaskList);

        long taskListId = savedTaskList.getId(), taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/update";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nike shoes"))
                .andExpect(jsonPath("$.taskListTitle").value("Shopping List"))
                .andExpect(jsonPath("$.completed").value(true));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().isCompleted()).isTrue();
    }
}
