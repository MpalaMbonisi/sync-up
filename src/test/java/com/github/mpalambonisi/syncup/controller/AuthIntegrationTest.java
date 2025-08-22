package com.github.mpalambonisi.syncup.controller;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Testcontainers
@Transactional // To ensure each test run in a transaction and is rolled back
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

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
    void register_WithValidDto_shouldReturn201Created() throws Exception{
        // Arrange
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .email("mpalambonisi@gmail.com")
                .username("mbonisimpala")
                .password("StrongPassword123")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully!"));

        // Post-Action Verification
        Optional<User> userOptional = userRepository.findByUsername("mbonisimpala");

        // Assertions
        assertThat(userOptional).isPresent();
        User createdUser = userOptional.get();
        assertThat(createdUser.getFirstName()).isEqualTo("Mbonisi");
        assertThat(createdUser.getLastName()).isEqualTo("Mpala");
        assertThat(createdUser.getEmail()).isEqualTo("mpalambonisi@gmail.com");
        assertThat(createdUser.getPassword()).isNotEqualTo("StrongPassword123");
    }

    @Test
    void register_withInvalidEmail_shouldReturn400BadRequest() throws Exception{
        // Arrange
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .email("invalidEmail") // invalid email format
                .username("mbonisimpala")
                .password("StrongPassword1234")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message", hasItems("Please provide a valid email address.")));

        // Post-Action Verification
        Optional<User> userOptional = userRepository.findByUsername("mbonisimpala");
        assertThat(userOptional.isEmpty()).isTrue();

    }

    @Test
    void register_withExistingUsername_shouldReturn409Conflict() throws Exception{
        // Arrange
        User existingUser = new User("mbonisimpala", "Mbonisi", "Mpala",
                "mbonisim12@gmail.com", "hashedPassword");
        userRepository.save(existingUser);
        long userCountBefore = userRepository.count();

        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .firstName("Mbonisi")
                .lastName("Mpala")
                .email("mbonisim12@gmail.com")
                .username("mbonisimpala")
                .password("StrongPassword1234")
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already in use."));

        // Post-Action Verification
        long userCountAfter = userRepository.count();
        assertThat(userCountBefore).isEqualTo(userCountAfter);

    }

}
