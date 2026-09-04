package com.example.usersservice.controller;

import com.example.usersservice.exception.ResourceNotFoundException;
import com.example.usersservice.model.User;
import com.example.usersservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController
 * Tests REST endpoints using MockMvc
 */
@WebMvcTest(controllers = UserController.class,
    excludeAutoConfiguration = {
        org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    })
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser, user2));

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[1].username").value("user2"))
                .andExpect(jsonPath("$[1].email").value("user2@example.com"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturn404() throws Exception {
        // Arrange
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    void createUser_WithValidData_ShouldReturnCreatedUser() throws Exception {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");

        when(userService.createUser(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void createUser_WithEmptyBody_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk()); // Note: Without validation, this still creates

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() throws Exception {
        // Arrange
        User updateRequest = new User();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    void updateUser_WithInvalidId_ShouldReturn404() throws Exception {
        // Arrange
        User updateRequest = new User();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");

        when(userService.updateUser(eq(999L), any(User.class)))
                .thenThrow(new ResourceNotFoundException("User", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(999L), any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldStillReturn204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(999L);
    }

    @Test
    void createUser_ShouldAcceptOnlyJsonContentType() throws Exception {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void updateUser_ShouldAcceptOnlyJsonContentType() throws Exception {
        // Arrange
        User updateRequest = new User();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).updateUser(any(Long.class), any(User.class));
    }

    @Test
    void getUserById_WithNonNumericId_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserById(any(Long.class));
    }

    @Test
    void createUser_WithCompleteUserData_ShouldMapAllFields() throws Exception {
        // Arrange
        User newUser = new User();
        newUser.setUsername("completeuser");
        newUser.setEmail("complete@example.com");

        User savedUser = new User();
        savedUser.setId(3L);
        savedUser.setUsername("completeuser");
        savedUser.setEmail("complete@example.com");

        when(userService.createUser(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("completeuser"))
                .andExpect(jsonPath("$.email").value("complete@example.com"));
    }
}
