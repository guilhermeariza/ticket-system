package com.guilhermeariza.ticketsystem.usersservice.integration;

import com.guilhermeariza.ticketsystem.usersservice.model.User;
import com.guilhermeariza.ticketsystem.usersservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User functionality
 * Tests the full stack from controller to repository
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("integrationuser");
        testUser.setEmail("integration@example.com");
    }

    @Test
    void testFullUserLifecycle_CreateReadUpdateDelete() throws Exception {
        // 1. Create user
        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(createResponse, User.class);
        Long userId = createdUser.getId();

        // 2. Read user
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("integrationuser"));

        // 3. Update user
        User updateRequest = new User();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        // 4. Delete user
        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        // 5. Verify deletion
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers_WithMultipleUsers() throws Exception {
        // Create multiple users
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        userRepository.save(user2);

        User user3 = new User();
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
        userRepository.save(user3);

        // Get all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("user1", "user2", "user3")));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateUser_PersistsToDatabase() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk());

        // Verify it was persisted
        long count = userRepository.count();
        assert count == 1;
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        User updateRequest = new User();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUser_UpdatesOnlySpecifiedFields() throws Exception {
        // Create initial user
        User savedUser = userRepository.save(testUser);

        // Update only username
        User updateRequest = new User();
        updateRequest.setUsername("newusername");
        updateRequest.setEmail("newemail@example.com");

        mockMvc.perform(put("/api/users/" + savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"));
    }

    @Test
    void testDeleteUser_RemovesFromDatabase() throws Exception {
        // Create user
        User savedUser = userRepository.save(testUser);
        long initialCount = userRepository.count();
        assert initialCount == 1;

        // Delete user
        mockMvc.perform(delete("/api/users/" + savedUser.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        long finalCount = userRepository.count();
        assert finalCount == 0;
    }

    @Test
    void testCreateMultipleUsers_AllPersist() throws Exception {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk());

        long count = userRepository.count();
        assert count == 2;
    }

    @Test
    void testGetAllUsers_WhenEmpty_ReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpdateUser_PreservesId() throws Exception {
        // Create user
        User savedUser = userRepository.save(testUser);
        Long originalId = savedUser.getId();

        // Update user
        User updateRequest = new User();
        updateRequest.setUsername("updatedname");
        updateRequest.setEmail("updated@example.com");

        mockMvc.perform(put("/api/users/" + originalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalId));
    }
}
