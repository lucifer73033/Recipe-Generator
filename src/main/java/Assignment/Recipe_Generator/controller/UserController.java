package Assignment.Recipe_Generator.controller;

import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.service.FavoriteService;
import Assignment.Recipe_Generator.service.RatingService;
import Assignment.Recipe_Generator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User profile and preferences API")
public class UserController {

    private final FavoriteService favoriteService;
    private final RatingService ratingService;
    private final UserService userService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new RuntimeException("User not authenticated");
    }



    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        try {
            String username = getCurrentUsername();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                Map<String, Object> profile = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "roles", user.getRoles(),
                    "createdAt", user.getCreatedAt(),
                    "lastLoginAt", user.getLastLoginAt()
                );
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get user profile"));
        }
    }

    @GetMapping("/saved")
    @Operation(summary = "Get user's saved recipes")
    public ResponseEntity<List<Recipe>> getSavedRecipes() {
        try {
            String username = getCurrentUsername();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                List<Recipe> savedRecipes = favoriteService.getUserFavoriteRecipes(user.getId());
                return ResponseEntity.ok(savedRecipes);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting saved recipes", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get user's recipe ratings")
    public ResponseEntity<List<Map<String, Object>>> getUserRatings() {
        try {
            String username = getCurrentUsername();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                var ratings = ratingService.getUserRatings(user.getId())
                    .stream()
                    .map(rating -> Map.<String, Object>of(
                        "recipeId", rating.getRecipeId(),
                        "stars", rating.getStars(),
                        "createdAt", rating.getCreatedAt(),
                        "updatedAt", rating.getUpdatedAt() != null ? rating.getUpdatedAt() : rating.getCreatedAt()
                    ))
                    .toList();
                return ResponseEntity.ok(ratings);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting user ratings", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            String username = getCurrentUsername();
            var userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                int savedRecipesCount = favoriteService.getUserFavoriteRecipes(user.getId()).size();
                int ratingsCount = ratingService.getUserRatings(user.getId()).size();
                
                Map<String, Object> stats = Map.of(
                    "savedRecipesCount", savedRecipesCount,
                    "ratingsCount", ratingsCount,
                    "memberSince", user.getCreatedAt()
                );
                return ResponseEntity.ok(stats);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting user stats", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get user stats"));
        }
    }
}



