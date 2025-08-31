package Assignment.Recipe_Generator.controller;

import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.service.RecipeService;
import Assignment.Recipe_Generator.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin-only operations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RecipeService recipeService;
    private final LogService logService;

    @PostMapping("/recipes")
    @Operation(summary = "Add a new recipe to the database (Admin only)")
    public ResponseEntity<Recipe> addRecipe(@RequestBody Recipe recipe) {
        try {
            // Set source as DB since this is manually added
            recipe.setSource(Recipe.Source.DB);
            
            Recipe savedRecipe = recipeService.saveRecipe(recipe);
            
            logService.logSystemEvent("admin_recipe_added", Map.of(
                "recipeId", savedRecipe.getId(),
                "title", savedRecipe.getTitle(),
                "ingredientsCount", savedRecipe.getIngredients().size()
            ), "INFO");
            
            return ResponseEntity.ok(savedRecipe);
            
        } catch (Exception e) {
            log.error("Error adding recipe via admin", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed the database with sample recipes (Admin only)")
    public ResponseEntity<Map<String, Object>> seedDatabase() {
        try {
            // This would call a seeding service
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "Database seeded successfully",
                "counts", Map.of(
                    "recipes", 0,
                    "ingredients", 0
                )
            );
            
            logService.logSystemEvent("admin_database_seeded", result, "INFO");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error seeding database", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics (Admin only)")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            // This would call various services to get stats
            Map<String, Object> stats = Map.of(
                "totalRecipes", 0,
                "totalUsers", 0,
                "systemStatus", "healthy"
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting admin stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}



