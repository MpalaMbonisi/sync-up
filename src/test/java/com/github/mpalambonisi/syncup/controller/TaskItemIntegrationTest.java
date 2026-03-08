package com.github.mpalambonisi.syncup.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskItemCreateDTO;
import com.github.mpalambonisi.syncup.dto.TaskItemStatusDTO;
import com.github.mpalambonisi.syncup.dto.request.TaskItemDescriptionDTO;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;

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

    private TaskList createTaskListAndSave(User collaborator){
        TaskList taskList = new TaskList();
        taskList.setTitle("Shopping List");
        taskList.setOwner(ownerUser);
        if(collaborator != null) taskList.getCollaborators().add(collaborator);

        return taskListRepository.save(taskList);
    }

    private TaskItem createTaskItemAndSave(TaskList taskList){
        TaskItem taskItem = new TaskItem();
        taskItem.setTaskList(taskList);
        taskItem.setDescription("Baggy Jeans");
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

    private TaskItem assertValidTaskItemCreation(TaskItem savedTaskItem, TaskList expectedTaskList){
        assertThat(savedTaskItem).isNotNull();
        assertThat(savedTaskItem.getDescription()).isEqualTo("Baggy Jeans");
        assertThat(savedTaskItem.getCompleted()).isFalse();
        assertThat(savedTaskItem.getTaskList()).isEqualTo(expectedTaskList);
        return savedTaskItem;
    }

    @Test
    void createTask_asOwner_shouldReturn201CreatedAndTaskItemResponseDTO() throws Exception{
        // Arrange
        long countBefore = taskItemRepository.count();
        TaskItemCreateDTO dto = new TaskItemCreateDTO("Nike shoes");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
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
                createTaskListAndSave(collaborator),
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
    void createTask_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword"));

        TaskItemCreateDTO dto = new TaskItemCreateDTO("Nike shoes");

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

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
                createTaskListAndSave(null),
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
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    void createTask_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemCreateDTO("Nike shoes"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void createTask_withDuplicateDescription_shouldReturn409Conflict() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem existingTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        String duplicateDesc = existingTaskItem.getDescription();
        TaskItemCreateDTO dto = new TaskItemCreateDTO(duplicateDesc);
        long taskListId = savedTaskList.getId();
        long countBefore = taskItemRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/" + taskListId + "/task/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A task with this description already exists in this list!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemStatus_asOwner_shouldReturn200OkAndTaskItemResponseDTO() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Baggy Jeans"))
                .andExpect(jsonPath("$.taskListTitle").value("Shopping List"))
                .andExpect(jsonPath("$.completed").value(true));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getCompleted()).isTrue();
    }

    @Test
    void updateTaskItemStatus_asCollaborator_shouldReturn200OkAndTaskItemResponseDTO() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", encoder.encode("ReallyStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(collaborator),
                ownerUser,
                collaborator);
        TaskItem savedTaskItem = assertValidTaskItemCreation(createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Baggy Jeans"))
                .andExpect(jsonPath("$.taskListTitle").value("Shopping List"))
                .andExpect(jsonPath("$.completed").value(true));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getCompleted()).isTrue();
    }

    @Test
    void updateTaskItemStatus_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getCompleted()).isFalse();
    }

    @Test
    void updateTaskItemStatus_withNonExistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(createTaskItemAndSave(savedTaskList), savedTaskList);

        long invalidTaskListId = 999L; // non-existent task ID
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + invalidTaskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getCompleted()).isFalse();
    }

    @Test
    void updateTaskItemStatus_withNonExistentTaskItemId_shouldReturn404NotFound() throws Exception{
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        long invalidTaskListId = savedTaskList.getId();
        long invalidTaskItemId = 999L; // non-existent task item ID

        // Act & Assert
        String url = "/list/" + invalidTaskListId + "/task/" + invalidTaskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Task item not found!"));
    }

    @Test
    void updateTaskItemStatus_whenTaskDoesNotBelongToList_shouldReturn403Forbidden() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        User differentOwner = createUserAndSave("Nicole", "Ncube", encoder.encode("SuperStrongPassword1234"));

        TaskList differentTaskList = new TaskList();
        differentTaskList.setTitle("Shopping List");
        differentTaskList.setOwner(differentOwner);
        taskListRepository.save(differentTaskList);

        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(differentTaskList), differentTaskList);

        long taskListId = savedTaskList.getId(); // User has access to this list
        long taskItemId = savedTaskItem.getId(); // But this task belongs to different list

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Task does not belong to the specified list!"));

        // Post-Action verification
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getCompleted()).isFalse();
    }

    @Test
    void updateTaskItemStatus_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/status";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemStatusDTO(true))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void getTaskItemById_asOwner_shouldReturn200OkAndTaskItem() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskItemId))
                .andExpect(jsonPath("$.description").value(savedTaskItem.getDescription()))
                .andExpect(jsonPath("$.completed").value(savedTaskItem.getCompleted()))
                .andExpect(jsonPath("$.taskListTitle").value(savedTaskList.getTitle()));
    }

    @Test
    void getTaskItemById_asCollaborator_shouldReturn200OkAndTaskItem() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", encoder.encode("ReallyStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(collaborator),
                ownerUser,
                collaborator);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskItemId))
                .andExpect(jsonPath("$.description").value(savedTaskItem.getDescription()))
                .andExpect(jsonPath("$.completed").value(savedTaskItem.getCompleted()))
                .andExpect(jsonPath("$.taskListTitle").value(savedTaskList.getTitle()));
    }

    @Test
    void getTaskItemById_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void getTaskItemById_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void getTaskItemById_withNonExistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskItemId = savedTaskItem.getId();
        long invalidTaskListId = 999L; // non-existent task list ID

        // Act & Assert
        String url = "/list/" + invalidTaskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void getTaskItemById_withNonExistentTaskItemId_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);

        long taskListId = savedTaskList.getId();
        long invalidTaskItemId = 999L; // non-existent task item Id

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + invalidTaskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Task item not found!"));
    }

    @Test
    void getTaskItemById_whenTaskItemDoesNotBelongToTaskList_shouldReturn403Forbidden() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        User differentOwner = createUserAndSave("Nicole", "Ncube", encoder.encode("SuperStrongPassword1234"));

        TaskList differentTaskList = new TaskList();
        differentTaskList.setTitle("Shopping List");
        differentTaskList.setOwner(differentOwner);
        taskListRepository.save(differentTaskList);

        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(differentTaskList), differentTaskList);

        long taskListId = savedTaskList.getId(); // User has access to this list
        long taskItemId = savedTaskItem.getId(); // But this task belongs to different list

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Task does not belong to the specified list!"));
    }

    @Test
    void updateTaskItemDescription_asOwner_shouldReturn200OkAndUpdatedTaskItem() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                 ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                    .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskItemId))
                .andExpect(jsonPath("$.description").value(updatedDesc))
                .andExpect(jsonPath("$.completed").value(savedTaskItem.getCompleted()))
                .andExpect(jsonPath("$.taskListTitle").value(savedTaskList.getTitle()));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(updatedDesc);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_asCollaborator_shouldReturn200OkAndUpdatedTaskItem() throws Exception{
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", encoder.encode("ReallyStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(collaborator),
                ownerUser, collaborator);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskItemId))
                .andExpect(jsonPath("$.description").value(updatedDesc))
                .andExpect(jsonPath("$.completed").value(savedTaskItem.getCompleted()))
                .andExpect(jsonPath("$.taskListTitle").value(savedTaskList.getTitle()));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(updatedDesc);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_asUnauthorisedUser_shouldReturn404NotFound() throws Exception{
        // Arrange
        User unauthorisedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorisedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(savedTaskItem.getDescription()); // does not update
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception{
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(savedTaskItem.getDescription());
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @ParameterizedTest
    @CsvSource({"'', 'Description cannot be empty.'", "'    ', 'Description cannot be blank.'"})
    void updateTaskItemDescription_withEmptyOrBlankDescription_shouldReturn400BadRequest(String invalidDesc, String expectedMessage) throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(invalidDesc))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems(expectedMessage)));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(savedTaskItem.getDescription());
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_withDuplicateDescription_shouldReturn409Conflict() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem taskItemToUpdate = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        // Create a DIFFERENT task item with the description we will use for the duplicate
        TaskItem existingTaskItemWithDuplicateDescription = new TaskItem();
        existingTaskItemWithDuplicateDescription.setTaskList(savedTaskList);
        existingTaskItemWithDuplicateDescription.setDescription("Nike shoes");
        existingTaskItemWithDuplicateDescription.setCompleted(false);
        TaskItem savedExistingItem = taskItemRepository.save(existingTaskItemWithDuplicateDescription);

        assertThat(savedExistingItem).isNotNull();
        assertThat(savedExistingItem.getDescription()).isEqualTo("Nike shoes");
        assertThat(savedExistingItem.getCompleted()).isFalse();
        assertThat(savedExistingItem.getTaskList()).isEqualTo(savedTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId();
        long taskItemToUpdateId = taskItemToUpdate.getId();
        String duplicateDesc = savedExistingItem.getDescription();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemToUpdateId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(duplicateDesc))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A task with this description already exists in this list!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemToUpdateId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(taskItemToUpdate.getDescription());
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_withNonExistentTaskListId_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long countBefore = taskItemRepository.count();
        long invalidTaskListId = 999L; // non-existent task list Id
        long taskItemId = savedTaskItem.getId();
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + invalidTaskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(savedTaskItem.getDescription());
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void updateTaskItemDescription_withNonExistentTaskItemId_shouldReturn404NotFound() throws Exception{
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser, null);

        long taskListId = savedTaskList.getId();
        long invalidTaskItemId = 999L; // non-existent task item Id
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + invalidTaskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message.length()").value(1))
                .andExpect(jsonPath("$.message").value("Task item not found!"));

        // Post-Action verification
        long taskItemCount = taskItemRepository.count();
        assertThat(taskItemCount).isEqualTo(0);
    }

    @Test
    void updateTaskItemDescription_whenTaskItemDoesNotBelongToTaskList_shouldReturn403Forbidden() throws Exception{
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        User differentOwner = createUserAndSave("Nicole", "Ncube", encoder.encode("SuperStrongPassword1234"));

        TaskList differentTaskList = new TaskList();
        differentTaskList.setTitle("Shopping List");
        differentTaskList.setOwner(differentOwner);
        taskListRepository.save(differentTaskList);

        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(differentTaskList), differentTaskList);

        long countBefore = taskItemRepository.count();
        long taskListId = savedTaskList.getId(); // User has access to this list
        long taskItemId = savedTaskItem.getId(); // But this task belongs to different list
        String updatedDesc = "Nike AirForce 1s";

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId + "/description";
        mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskItemDescriptionDTO(updatedDesc))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Task does not belong to the specified list!"));

        // Post-Action verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> retrievedTaskItem = taskItemRepository.findById(taskItemId);
        assertThat(retrievedTaskItem.isPresent()).isTrue();
        assertThat(retrievedTaskItem.get().getDescription()).isEqualTo(savedTaskItem.getDescription());
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void deleteTask_asOwner_returns204NoContent() throws Exception {
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        long countBefore = taskItemRepository.count();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Post-Action Verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> deletedTask = taskItemRepository.findById(taskItemId);
        Optional<TaskList> taskListAfter = taskListRepository.findById(taskListId);

        assertThat(deletedTask).isEmpty();
        assertThat(taskListAfter).isPresent();
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    @Test
    void deleteTask_asCollaborator_returns204NoContent() throws Exception {
        // Arrange
        User collaborator = createUserAndSave("John", "Smith", encoder.encode("StrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(collaborator),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        long countBefore = taskItemRepository.count();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(collaborator)))
                .andExpect(status().isNoContent());

        // Post-Action Verification
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> deletedTask = taskItemRepository.findById(taskItemId);

        assertThat(deletedTask).isEmpty();
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    @Test
    void deleteTask_lastTaskInList_listStillExists() throws Exception {
        // Arrange - List with only one task
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNoContent());

        // Post-Action Verification
        Optional<TaskItem> deletedTask = taskItemRepository.findById(taskItemId);
        Optional<TaskList> taskListAfter = taskListRepository.findById(taskListId);

        assertThat(deletedTask).isEmpty();
        assertThat(taskListAfter).isPresent();
        assertThat(taskListAfter.get().getTasks()).isEmpty();
    }

    @Test
    void deleteTask_listNotFound_returns404() throws Exception {
        // Arrange
        long invalidListId = 99999L;
        long taskId = 1L;

        // Act & Assert
        String url = "/list/" + invalidListId + "/task/" + taskId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));
    }

    @Test
    void deleteTask_taskNotFound_returns404() throws Exception {
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        long taskListId = savedTaskList.getId();
        long invalidTaskId = 99999L;

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + invalidTaskId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task item not found!"));
    }

    @Test
    void deleteTask_noAccess_returns404() throws Exception {
        // Arrange
        User unauthorizedUser = createUserAndSave("Karen", "Sanders", encoder.encode("VeryStrongPassword1234"));

        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();
        long countBefore = taskItemRepository.count();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(unauthorizedUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("List not found or you don't have access to it!"));

        // Post-Action Verification - task should still exist
        long countAfter = taskItemRepository.count();
        Optional<TaskItem> taskAfter = taskItemRepository.findById(taskItemId);

        assertThat(taskAfter).isPresent();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void deleteTask_taskFromDifferentList_returns403() throws Exception {
        // Arrange
        TaskList listA = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        User differentOwner = createUserAndSave("Nicole", "Ncube", encoder.encode("SuperStrongPassword1234"));
        TaskList listB = new TaskList();
        listB.setTitle("List B");
        listB.setOwner(differentOwner);
        TaskList savedListB = taskListRepository.save(listB);

        TaskItem taskFromListB = new TaskItem();
        taskFromListB.setDescription("Task from B");
        taskFromListB.setCompleted(false);
        taskFromListB.setTaskList(savedListB);
        TaskItem savedTaskFromB = taskItemRepository.save(taskFromListB);

        long listAId = listA.getId();
        long taskFromBId = savedTaskFromB.getId();

        // Act & Assert - Try to delete task from List B using List A's ID
        String url = "/list/" + listAId + "/task/" + taskFromBId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Task does not belong to the specified list!"));

        // Post-Action Verification - task should still exist
        Optional<TaskItem> taskAfter = taskItemRepository.findById(taskFromBId);
        assertThat(taskAfter).isPresent();
    }

    @Test
    void deleteTask_unauthenticated_returns401() throws Exception {
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        // Act & Assert
        String url = "/list/" + taskListId + "/task/" + taskItemId;
        mockMvc.perform(MockMvcRequestBuilders.delete(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void deleteTask_verifyTaskActuallyDeleted() throws Exception {
        // Arrange
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);
        TaskItem savedTaskItem = assertValidTaskItemCreation(
                createTaskItemAndSave(savedTaskList), savedTaskList);

        long taskListId = savedTaskList.getId();
        long taskItemId = savedTaskItem.getId();

        String deleteUrl = "/list/" + taskListId + "/task/" + taskItemId;
        String getUrl = "/list/" + taskListId + "/task/" + taskItemId;

        // Act - Delete the task
        mockMvc.perform(MockMvcRequestBuilders.delete(deleteUrl)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNoContent());

        // Assert - Try to GET the deleted task
        mockMvc.perform(MockMvcRequestBuilders.get(getUrl)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task item not found!"));
    }

    @Test
    void deleteTask_otherTasksUnaffected() throws Exception {
        // Arrange - Create list with 3 tasks
        TaskList savedTaskList = assertValidTaskListCreation(
                createTaskListAndSave(null),
                ownerUser,
                null);

        TaskItem task1 = new TaskItem();
        task1.setDescription("Task 1");
        task1.setCompleted(false);
        task1.setTaskList(savedTaskList);
        TaskItem savedTask1 = taskItemRepository.save(task1);

        TaskItem task2 = new TaskItem();
        task2.setDescription("Task 2 - TO DELETE");
        task2.setCompleted(false);
        task2.setTaskList(savedTaskList);
        TaskItem savedTask2 = taskItemRepository.save(task2);

        TaskItem task3 = new TaskItem();
        task3.setDescription("Task 3");
        task3.setCompleted(false);
        task3.setTaskList(savedTaskList);
        TaskItem savedTask3 = taskItemRepository.save(task3);

        long taskListId = savedTaskList.getId();
        long task2Id = savedTask2.getId();

        // Act - Delete middle task (task2)
        String url = "/list/" + taskListId + "/task/" + task2Id;
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .with(SecurityMockMvcRequestPostProcessors.user(ownerUser)))
                .andExpect(status().isNoContent());

        // Assert - Verify task2 deleted, task1 and task3 still exist
        Optional<TaskItem> task1After = taskItemRepository.findById(savedTask1.getId());
        Optional<TaskItem> task2After = taskItemRepository.findById(task2Id);
        Optional<TaskItem> task3After = taskItemRepository.findById(savedTask3.getId());

        assertThat(task1After).isPresent();
        assertThat(task2After).isEmpty();
        assertThat(task3After).isPresent();
    }



}
