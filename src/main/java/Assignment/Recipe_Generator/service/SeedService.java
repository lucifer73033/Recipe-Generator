package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.repository.RecipeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedService {

    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;
    private final LogService logService;

    public Map<String, Integer> seedDatabase() {
        try {
            int recipesLoaded = loadSeedRecipes();
            
            logService.logSystemEvent("database_seeded", 
                Map.of("recipesLoaded", recipesLoaded), 
                "INFO");
            
            return Map.of("recipes", recipesLoaded);
            
        } catch (Exception e) {
            log.error("Error seeding database", e);
            
            logService.logSystemEvent("database_seed_error", 
                Map.of("error", e.getMessage()), 
                "ERROR");
            
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    private int loadSeedRecipes() throws IOException {
        ClassPathResource resource = new ClassPathResource("seed/recipes.json");
        
        if (!resource.exists()) {
            log.warn("Seed recipes file not found at seed/recipes.json");
            return 0;
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<Recipe>> seedData = objectMapper.readValue(
                inputStream, 
                new TypeReference<Map<String, List<Recipe>>>() {}
            );
            
            List<Recipe> recipes = seedData.get("recipes");
            if (recipes == null || recipes.isEmpty()) {
                log.warn("No recipes found in seed data");
                return 0;
            }
            
            // Set source for all seed recipes
            recipes.forEach(recipe -> {
                recipe.setSource(Recipe.Source.DB);
                recipe.setCreatedBy(null); // System-created
            });
            
            // Clear existing seed recipes before loading new ones
            recipeRepository.deleteBySource(Recipe.Source.DB);
            
            List<Recipe> savedRecipes = recipeRepository.saveAll(recipes);
            
            log.info("Loaded {} seed recipes", savedRecipes.size());
            return savedRecipes.size();
        }
    }
}



