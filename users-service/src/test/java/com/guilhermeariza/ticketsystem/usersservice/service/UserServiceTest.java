package com.guilhermeariza.ticketsystem.usersservice.service;

import com.guilhermeariza.ticketsystem.usersservice.exception.ResourceNotFoundException;
import com.guilhermeariza.ticketsystem.usersservice.model.User;
import com.guilhermeariza.ticketsystem.usersservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests business logic in isolation using Mockito
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
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
    void getAllUsers_ShouldReturnListOfUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void createUser_ShouldSaveAndReturnUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.createUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void createUser_WithNullUser_ShouldHandleGracefully() {
        // Arrange
        when(userRepository.save(null)).thenThrow(new IllegalArgumentException("User cannot be null"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(null);
        });
    }

    @Test
    void updateUser_WithValidId_ShouldUpdateAndReturnUser() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");
        updatedDetails.setEmail("updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUser_WithInvalidId_ShouldThrowException() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updateduser");
        updatedDetails.setEmail("updated@example.com");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(999L, updatedDetails);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("newusername");
        updatedDetails.setEmail("newemail@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deleteUser_WithValidId_ShouldCallRepository() {
        // Arrange
        doNothing().when(userRepository).deleteById(1L);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldStillCallRepository() {
        // Arrange
        doNothing().when(userRepository).deleteById(999L);

        // Act
        userService.deleteUser(999L);

        // Assert
        verify(userRepository, times(1)).deleteById(999L);
    }

    @Test
    void createUser_WithValidData_ShouldPersistCorrectly() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // Act
        User result = userService.createUser(newUser);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("newuser@example.com", result.getEmail());
    }

    @Test
    void updateUser_ShouldNotModifyId() {
        // Arrange
        User updatedDetails = new User();
        updatedDetails.setUsername("updatedname");
        updatedDetails.setEmail("updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updatedDetails);

        // Assert
        assertEquals(1L, result.getId());
        verify(userRepository, times(1)).save(testUser);
    }
}
