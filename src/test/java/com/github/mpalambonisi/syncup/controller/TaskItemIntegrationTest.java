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
    void createTask_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
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
                .andExpect(jsonPath("$.message").value("List not found!"));

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
    void updateTaskItemStatus_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));

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
                .andExpect(jsonPath("$.message").value("List not found!"));

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
    void getTaskItemById_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));
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
                .andExpect(jsonPath("$.message").value("List not found!"));
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
    void updateTaskItemDescription_asUnauthorisedUser_shouldReturn403Forbidden() throws Exception{
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not authorised to access this list!"));

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
                .andExpect(status().isForbidden())
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
}
