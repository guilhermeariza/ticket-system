package com.guilhermeariza.ticketsystem.authservice.service;

import com.guilhermeariza.ticketsystem.authservice.model.User;
import com.guilhermeariza.ticketsystem.authservice.repository.UserRepository;
import com.guilhermeariza.ticketsystem.authservice.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
    }

    @Test
    void register_ShouldEncodePasswordAndSaveUser() {
        // Arrange
        String encodedPassword = "encodedPassword123";
        when(passwordEncoder.encode(testUser.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(testUser);
    }

    @Test
    void register_ShouldReturnSavedUser() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser);

        // Assert
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String expectedToken = "jwt.token.here";

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn(expectedToken);

        // Act
        String token = authService.login(username, password);

        // Assert
        assertNotNull(token);
        assertEquals(expectedToken, token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(userDetails);
    }

    @Test
    void login_ShouldCallAuthenticationManager() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // Act
        authService.login(username, password);

        // Assert
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_ShouldSetEncodedPassword() {
        // Arrange
        String encodedPassword = "super_secure_hash";
        when(passwordEncoder.encode(testUser.getPassword())).thenReturn(encodedPassword);

        User savedUser = new User();
        savedUser.setUsername(testUser.getUsername());
        savedUser.setPassword(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(testUser);

        // Assert
        verify(passwordEncoder).encode("password123");
    }
}
