package Assignment.Recipe_Generator.controller;

import Assignment.Recipe_Generator.dto.RecipeRequest;
import Assignment.Recipe_Generator.dto.RecipeResponse;
import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.service.FavoriteService;
import Assignment.Recipe_Generator.service.RatingService;
import Assignment.Recipe_Generator.service.RecipeService;
import Assignment.Recipe_Generator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recipes", description = "Recipe generation and management API")
public class RecipeController {

    private final RecipeService recipeService;
    private final RatingService ratingService;
    private final FavoriteService favoriteService;
    private final UserService userService;

    private String getCurrentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            var userOpt = userService.findByUsername(username);
            return userOpt.map(user -> user.getId()).orElse(null);
        }
        return null;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate recipes based on ingredients and preferences")
    public ResponseEntity<RecipeResponse> generateRecipes(
            @Valid @RequestBody RecipeRequest request) {
        
        String userId = getCurrentUserId();
        RecipeResponse response = recipeService.generateRecipes(request, userId);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search and filter recipes")
    public ResponseEntity<Page<Recipe>> searchRecipes(
            @Parameter(description = "Search query for recipe titles and ingredients")
            @RequestParam(required = false) String query,
            
            @Parameter(description = "Diet tags to filter by")
            @RequestParam(required = false) Set<String> diet,
            
            @Parameter(description = "Maximum cooking time in minutes")
            @RequestParam(required = false) Integer timeMax,
            
            @Parameter(description = "Recipe difficulty level")
            @RequestParam(required = false) Recipe.Difficulty difficulty,
            
            @Parameter(description = "Cuisine type")
            @RequestParam(required = false) String cuisine,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Recipe> recipes = recipeService.searchRecipes(query, diet, cuisine, difficulty, timeMax, pageable);
        
        return ResponseEntity.ok(recipes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recipe by ID")
    public ResponseEntity<Recipe> getRecipe(@PathVariable String id) {
        return recipeService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "Rate a recipe (requires authentication)")
    public ResponseEntity<Map<String, Object>> rateRecipe(
            @PathVariable String id,
            @RequestBody Map<String, Integer> request) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Integer stars = request.get("stars");
        if (stars == null || stars < 1 || stars > 5) {
            return ResponseEntity.badRequest().build();
        }
        
        ratingService.rateRecipe(userId, id, stars);
        
        // Return updated rating stats
        double avgRating = ratingService.getAverageRating(id);
        int ratingCount = ratingService.getRatingCount(id);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "averageRating", avgRating,
            "ratingCount", ratingCount,
            "userRating", stars
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/save")
    @Operation(summary = "Save recipe to favorites (requires authentication)")
    public ResponseEntity<Map<String, Object>> saveRecipe(
            @PathVariable String id) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            favoriteService.addToFavorites(userId, id);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Recipe saved to favorites"
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/save-llm")
    @Operation(summary = "Save LLM-generated recipe to favorites (requires authentication)")
    public ResponseEntity<Map<String, Object>> saveLLMRecipe(
            @RequestBody Recipe recipe) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            // First save the LLM recipe to the database
            Recipe savedRecipe = recipeService.saveRecipe(recipe);
            
            // Then add it to user's favorites
            favoriteService.addToFavorites(userId, savedRecipe.getId());
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "LLM recipe saved to favorites",
                "recipeId", savedRecipe.getId()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to save LLM recipe: " + e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}/save")
    @Operation(summary = "Remove recipe from favorites (requires authentication)")
    public ResponseEntity<Map<String, Object>> unsaveRecipe(
            @PathVariable String id) {
        
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        favoriteService.removeFromFavorites(userId, id);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "Recipe removed from favorites"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/rating")
    @Operation(summary = "Get recipe rating information")
    public ResponseEntity<Map<String, Object>> getRecipeRating(
            @PathVariable String id) {
        
        double avgRating = ratingService.getAverageRating(id);
        int ratingCount = ratingService.getRatingCount(id);
        
        String userId = getCurrentUserId();
        Integer userRating = null;
        if (userId != null) {
            userRating = ratingService.getUserRating(userId, id)
                .map(rating -> rating.getStars())
                .orElse(null);
        }
        
        Map<String, Object> response = Map.of(
            "averageRating", avgRating,
            "ratingCount", ratingCount,
            "userRating", userRating
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/favorite")
    @Operation(summary = "Check if recipe is favorited by user")
    public ResponseEntity<Map<String, Object>> getRecipeFavorite(
            @PathVariable String id) {
        
        long favoriteCount = favoriteService.getFavoriteCount(id);
        String userId = getCurrentUserId();
        boolean isFavorited = userId != null && favoriteService.isFavorited(userId, id);
        
        Map<String, Object> response = Map.of(
            "favoriteCount", favoriteCount,
            "isFavorited", isFavorited
        );
        
        return ResponseEntity.ok(response);
    }
}



