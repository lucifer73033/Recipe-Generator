package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.dto.AuthResponse;
import Assignment.Recipe_Generator.dto.LoginRequest;
import Assignment.Recipe_Generator.dto.RegisterRequest;
import Assignment.Recipe_Generator.model.User;
import Assignment.Recipe_Generator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            
            if (userOpt.isEmpty()) {
                return AuthResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build();
            }
            
            User user = userOpt.get();
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return AuthResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build();
            }
            
            // Update last login
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
            
            // Generate basic auth token
            String token = generateBasicAuthToken(request.getUsername(), request.getPassword());
            
            return AuthResponse.builder()
                .success(true)
                .message("Login successful")
                .token(token)
                .user(user)
                .build();
                
        } catch (Exception e) {
            log.error("Login failed", e);
            return AuthResponse.builder()
                .success(false)
                .message("Login failed: " + e.getMessage())
                .build();
        }
    }
    
    public AuthResponse register(RegisterRequest request) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return AuthResponse.builder()
                    .success(false)
                    .message("Username already exists")
                    .build();
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.builder()
                    .success(false)
                    .message("Email already exists")
                    .build();
            }
            
            // Create new user
            User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Collections.singletonList(User.Role.USER))
                .build();
            
            User savedUser = userRepository.save(newUser);
            
            // Generate basic auth token
            String token = generateBasicAuthToken(request.getUsername(), request.getPassword());
            
            return AuthResponse.builder()
                .success(true)
                .message("Registration successful")
                .token(token)
                .user(savedUser)
                .build();
                
        } catch (Exception e) {
            log.error("Registration failed", e);
            return AuthResponse.builder()
                .success(false)
                .message("Registration failed: " + e.getMessage())
                .build();
        }
    }
    
    private String generateBasicAuthToken(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
