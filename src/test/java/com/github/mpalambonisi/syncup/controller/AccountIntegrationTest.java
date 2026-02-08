package com.github.mpalambonisi.syncup.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

@Testcontainers
@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private PasswordEncoder encoder;

    private User testUser;

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("nicolesmith");
        testUser.setFirstName("Nicole");
        testUser.setLastName("Smith");
        testUser.setEmail("nicolesmith@example.com");
        testUser.setPassword(encoder.encode("StrongPassword1234"));
        testUser = userRepository.save(testUser);
    }

    private User createAndSaveUser(String firstName, String lastName, String password) {
        String username = (firstName + lastName).toLowerCase();
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(username + "@example.com");
        user.setPassword(encoder.encode(password));
        return userRepository.save(user);
    }

    @Test
    void getAccountDetails_asAuthenticatedUser_shouldReturn200AndUserDetails() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/account/details")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("nicolesmith"))
                .andExpect(jsonPath("$.firstName").value("Nicole"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("nicolesmith@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getAccountDetails_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/account/details"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));
    }

    @Test
    void deleteAccount_asAuthenticatedUser_shouldReturn204AndDeleteUser() throws Exception {
        // Arrange
        long userCountBefore = userRepository.count();
        Long userId = testUser.getId();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/account/delete")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUser)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Post-Action Verification
        long userCountAfter = userRepository.count();
        Optional<User> deletedUser = userRepository.findById(userId);

        assertThat(deletedUser).isEmpty();
        assertThat(userCountAfter).isEqualTo(userCountBefore - 1);
    }

    @Test
    void deleteAccount_asUnauthenticatedUser_shouldReturn401Unauthorised() throws Exception {
        // Arrange
        long userCountBefore = userRepository.count();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/account/delete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is unauthorised! Authentication Failed!"));

        // Post-Action Verification
        long userCountAfter = userRepository.count();
        assertThat(userCountAfter).isEqualTo(userCountBefore);
    }

    @Test
    void deleteAccount_whenUserOwnsTaskLists_shouldDeleteUserAndOwnedTaskLists() throws Exception {
        // Arrange
        TaskList taskList1 = new TaskList();
        taskList1.setTitle("Shopping List");
        taskList1.setOwner(testUser);
        taskListRepository.save(taskList1);

        TaskList taskList2 = new TaskList();
        taskList2.setTitle("Todo List");
        taskList2.setOwner(testUser);
        taskListRepository.save(taskList2);

        long taskListCountBefore = taskListRepository.count();
        Long userId = testUser.getId();

        // Act
        mockMvc.perform(MockMvcRequestBuilders.delete("/account/delete")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUser)))
                .andExpect(status().isNoContent());

        // Assert
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();

        // Verify that task lists owned by the user are deleted
        long taskListCountAfter = taskListRepository.count();
        assertThat(taskListCountAfter).isLessThan(taskListCountBefore);
        assertThat(taskListRepository.findAllByOwner(testUser)).isEmpty();
    }
}
