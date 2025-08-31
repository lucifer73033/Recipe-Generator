package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.model.User;
import Assignment.Recipe_Generator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void updateLastLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public boolean isAdmin(String userId) {
        return userRepository.findById(userId)
            .map(user -> user.getRoles().contains(User.Role.ADMIN))
            .orElse(false);
    }

    public void promoteToAdmin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getRoles().add(User.Role.ADMIN);
            userRepository.save(user);
        });
    }

    public void revokeAdmin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getRoles().remove(User.Role.ADMIN);
            userRepository.save(user);
        });
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}



