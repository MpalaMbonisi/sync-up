package com.github.mpalambonisi.syncup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpalambonisi.syncup.dto.TaskListCreateDTO;
import com.github.mpalambonisi.syncup.model.TaskList;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    // Configure Test containers
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Test
    void createList_asOwner_shouldReturn201CreatedAndList() throws Exception{
        // Arrange
        // 1. Create a user with a real encoded password and save them to the database
        User ownerUser = new User("mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", encoder.encode("StrongPassword1234"));
        userRepository.save(ownerUser);

        // 2. Manually set the authentication context
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(ownerUser, null, ownerUser.getAuthorities());

        // Place this real Authentication object into the security context
        SecurityContextHolder.getContext().setAuthentication(authToken);

        TaskListCreateDTO dto = TaskListCreateDTO.builder()
                .title("Grocery Shopping List")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/list/create")
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
        // Skip authorisation

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
}
