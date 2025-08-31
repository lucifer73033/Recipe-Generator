package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.dto.RecipeRequest;
import Assignment.Recipe_Generator.dto.RecipeResponse;
import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.repository.RecipeRepository;
import Assignment.Recipe_Generator.service.IngredientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final OpenRouterService openRouterService;

    private final LogService logService;
    private final IngredientService ingredientService;

    @Value("${recipe.score.min:0.60}")
    private double scoreMin;

    @Value("${recipe.score.avg-min:0.55}")
    private double scoreAvgMin;

    public RecipeResponse generateRecipes(RecipeRequest request, String userId) {
        Map<String, Object> requestMetadata = Map.of(
            "ingredients", request.getIngredients(),
            "dietTags", request.getDietTags() != null ? request.getDietTags() : Set.of(),
            "maxTime", request.getMaxTimeMinutes() != null ? request.getMaxTimeMinutes() : "unlimited",
            "difficulty", request.getDifficulty() != null ? request.getDifficulty().name() : "any",
            "cuisine", request.getCuisine() != null ? request.getCuisine() : "any"
        );

        // Categorize user ingredients (matched vs unmatched to master array)
        Map<String, List<String>> categorizedIngredients = ingredientService.categorizeIngredients(request.getIngredients());
        List<String> matchedIngredients = categorizedIngredients.get("matched");
        List<String> unmatchedIngredients = categorizedIngredients.get("unmatched");

        log.info("Ingredient analysis - Matched: {}, Unmatched: {}", matchedIngredients.size(), unmatchedIngredients.size());

        // Step 1: Find DB recipes with at least one ingredient match AND apply filters
        List<Recipe> allDbRecipes = recipeRepository.findAll();
        List<Recipe> dbRecipesWithMatch = allDbRecipes.stream()
            .filter(recipe -> {
                // Check if at least one ingredient matches
                Set<String> recipeIngredients = recipe.getIngredients().stream()
                    .map(ing -> ing.getName().toLowerCase())
                    .collect(Collectors.toSet());
                
                Set<String> userIngredientsLower = request.getIngredients().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
                
                // Check for intersection (at least one match)
                Set<String> intersection = new HashSet<>(recipeIngredients);
                intersection.retainAll(userIngredientsLower);
                return !intersection.isEmpty();
            })
            .filter(recipe -> applyFilters(recipe, request)) // Apply user filters
            .sorted((r1, r2) -> {
                // Sort by ingredient match percentage (highest first)
                double match1 = ingredientService.calculateIngredientMatchPercentage(request.getIngredients(), r1);
                double match2 = ingredientService.calculateIngredientMatchPercentage(request.getIngredients(), r2);
                return Double.compare(match2, match1);
            })
            .limit(3) // Take top 3 DB recipes
            .collect(Collectors.toList());
        
        log.info("Step 1 - Found {} DB recipes with at least one ingredient match", dbRecipesWithMatch.size());

        // Step 2: Handle dietary preferences and ensure we have enough recipes
        List<Recipe> llmRecipes = new ArrayList<>();
        
        // If user has dietary preferences and we have DB recipes, send them to LLM for modification
        if (!dbRecipesWithMatch.isEmpty() && request.getDietTags() != null &&
            request.getDietTags().size() > 0) {
            log.info("Step 2a - Sending DB recipes to LLM for dietary modification");
            List<Recipe> modifiedDbRecipes = openRouterService.modifyRecipesForDietaryPreferences(
                dbRecipesWithMatch, request, userId);

            // Scale modified DB recipes for requested portion size
            modifiedDbRecipes = modifiedDbRecipes.stream()
                .map(recipe -> scaleRecipeForServings(recipe, request.getServings()))
                .collect(Collectors.toList());

            // Replace the original DB recipes with the modified versions so the old
            // variants do not appear in the final list.
            dbRecipesWithMatch = modifiedDbRecipes;
        }
        
        // If we still need more recipes to reach 3 total, generate additional LLM recipes
        int totalRecipes = dbRecipesWithMatch.size() + llmRecipes.size();
        if (totalRecipes < 3) {
            int needed = 3 - totalRecipes;
            log.info("Step 2b - Generating {} additional LLM recipes to reach 3 total", needed);

            // Collect existing titles to avoid duplicates
            Set<String> excludeTitles = new HashSet<>();
            dbRecipesWithMatch.forEach(r -> excludeTitles.add(r.getTitle()));
            llmRecipes.forEach(r -> excludeTitles.add(r.getTitle()));

            for (int i = 0; i < needed; i++) {
                List<Recipe> generated = openRouterService.generateSingleRecipe(request, userId, new ArrayList<>(excludeTitles));
                if (!generated.isEmpty()) {
                    Recipe recipe = generated.get(0);
                    recipe = scaleRecipeForServings(recipe, request.getServings());
                    llmRecipes.add(recipe);
                    excludeTitles.add(recipe.getTitle());
                } else {
                    log.warn("LLM did not return a recipe in attempt {}", i + 1);
                    break;
                }
            }
        }

        // Step 3: Combine DB and LLM recipes
        List<Recipe> combinedRecipes = new ArrayList<>();
        combinedRecipes.addAll(dbRecipesWithMatch);
        combinedRecipes.addAll(llmRecipes);
        
        log.info("Step 3 - Combined recipes: {} DB + {} LLM = {} total", 
            dbRecipesWithMatch.size(), llmRecipes.size(), combinedRecipes.size());

        // Final ranking by ingredient match percentage
        List<Recipe> finalRecipes = combinedRecipes.stream()
            .sorted((r1, r2) -> {
                double match1 = ingredientService.calculateIngredientMatchPercentage(request.getIngredients(), r1);
                double match2 = ingredientService.calculateIngredientMatchPercentage(request.getIngredients(), r2);
                return Double.compare(match2, match1);
            })
            .limit(3) // Ensure exactly 3 recipes
            .collect(Collectors.toList());
        
        log.info("Step 3a - After ranking and limiting: {} recipes", finalRecipes.size());

        // Deduplicate if needed
        List<Recipe> beforeDedup = new ArrayList<>(finalRecipes);
        finalRecipes = deduplicateRecipes(finalRecipes);
        log.info("Step 3b - After deduplication: {} recipes (was {})", finalRecipes.size(), beforeDedup.size());

        // Scale recipes for requested portion size
        finalRecipes = finalRecipes.stream()
            .map(recipe -> scaleRecipeForServings(recipe, request.getServings()))
            .collect(Collectors.toList());
        
        log.info("Step 3c - After scaling: {} recipes", finalRecipes.size());
        
        // Log recipe details for debugging
        for (int i = 0; i < finalRecipes.size(); i++) {
            Recipe recipe = finalRecipes.get(i);
            log.info("Final recipe {}: '{}' (source: {}, ingredients: {})", 
                i + 1, recipe.getTitle(), recipe.getSource(), 
                recipe.getIngredients().stream().map(Recipe.Ingredient::getName).collect(Collectors.joining(", ")));
        }

        String strategy = llmRecipes.isEmpty() ? "db_only" : "db_llm_combined";
        logService.logRecipeGeneration(userId, strategy, finalRecipes.size(), requestMetadata);

        Map<String, Object> enhancedMetadata = Map.of(
            "dbRecipeCount", dbRecipesWithMatch.size(),
            "llmRecipeCount", llmRecipes.size(),
            "finalCount", finalRecipes.size(),
            "matchedIngredients", matchedIngredients.size(),
            "unmatchedIngredients", unmatchedIngredients.size(),
            "strategy", strategy
        );

        logService.logSystemEvent("enhanced_recipe_generation", enhancedMetadata, "INFO");

        // Create response with metadata
        RecipeResponse.RecipeMetadata metadata = RecipeResponse.RecipeMetadata.builder()
            .totalRecipes(finalRecipes.size())
            .highMatchCount(dbRecipesWithMatch.size())
            .userHasAllCount(0) // Not using this concept anymore
            .llmGeneratedCount(llmRecipes.size())
            .strategy(strategy)
            .hasUserHasAllRecipes(false) // Not using this concept anymore
            .message(null)
            .userHasAllRecipeIds(new ArrayList<>())
            .build();

        return RecipeResponse.builder()
            .recipes(finalRecipes)
            .metadata(metadata)
            .build();
    }

    private List<ScoredRecipe> findMatchingRecipesFromDB(RecipeRequest request) {
        List<Recipe> allRecipes = recipeRepository.findAll();
        Set<String> availableIngredients = new HashSet<>(request.getIngredients());
        
        return allRecipes.stream()
            .map(recipe -> new ScoredRecipe(recipe, calculateMatchScore(recipe, request, availableIngredients)))
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(20)
            .collect(Collectors.toList());
    }

    private double calculateMatchScore(Recipe recipe, RecipeRequest request, Set<String> availableIngredients) {
        // Get recipe ingredients names
        Set<String> recipeIngredients = recipe.getIngredients().stream()
            .map(ing -> ing.getName().toLowerCase())
            .collect(Collectors.toSet());
        
        Set<String> normalizedAvailable = availableIngredients.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        
        // Calculate Jaccard similarity
        Set<String> intersection = new HashSet<>(recipeIngredients);
        intersection.retainAll(normalizedAvailable);
        
        Set<String> union = new HashSet<>(recipeIngredients);
        union.addAll(normalizedAvailable);
        
        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        
        // Calculate bonuses/penalties
        double timeBonus = calculateTimeBonus(recipe, request.getMaxTimeMinutes());
        double difficultyBonus = calculateDifficultyBonus(recipe, request.getDifficulty());
        
        // Final score calculation (dietary preferences now handled by LLM)
        double score = 0.7 * jaccard + 0.15 * timeBonus + 0.15 * difficultyBonus;
        
        return Math.max(0.0, Math.min(1.0, score)); // Clamp between 0 and 1
    }

    private double calculateTimeBonus(Recipe recipe, Integer maxTimeMinutes) {
        if (maxTimeMinutes == null || recipe.getTimeMinutes() == null) {
            return 0.05; // Small neutral bonus
        }
        
        if (recipe.getTimeMinutes() <= maxTimeMinutes) {
            // Bonus for being under time limit, higher bonus for much faster recipes
            double ratio = (double) recipe.getTimeMinutes() / maxTimeMinutes;
            return 0.15 * (1.0 - ratio); // 0 to 0.15 bonus
        } else {
            // Penalty for exceeding time limit
            return -0.05;
        }
    }

    private double calculateDifficultyBonus(Recipe recipe, Recipe.Difficulty requestedDifficulty) {
        if (requestedDifficulty == null || recipe.getDifficulty() == null) {
            return 0.05; // Small neutral bonus
        }
        
        if (recipe.getDifficulty() == requestedDifficulty) {
            return 0.15; // Perfect match bonus
        } else if (recipe.getDifficulty().ordinal() < requestedDifficulty.ordinal()) {
            return 0.10; // Bonus for easier than requested
        } else {
            return 0.0; // No bonus for harder than requested
        }
    }

    private boolean evaluateQualityGate(List<ScoredRecipe> recipes) {
        if (recipes.size() < 3) {
            return false;
        }
        
        // Check if we have at least 3 recipes with score >= scoreMin
        long highQualityRecipes = recipes.stream()
            .filter(sr -> sr.getScore() >= scoreMin)
            .count();
        
        if (highQualityRecipes < 3) {
            return false;
        }
        
        // Check if average score of top 5 is >= scoreAvgMin
        double avgScore = recipes.stream()
            .limit(5)
            .mapToDouble(ScoredRecipe::getScore)
            .average()
            .orElse(0.0);
        
        return avgScore >= scoreAvgMin;
    }

    private List<Recipe> deduplicateRecipes(List<Recipe> recipes) {
        Map<String, Recipe> uniqueRecipes = new LinkedHashMap<>();
        
        for (Recipe recipe : recipes) {
            String key = generateRecipeKey(recipe);
            if (!uniqueRecipes.containsKey(key)) {
                uniqueRecipes.put(key, recipe);
            }
        }
        
        return new ArrayList<>(uniqueRecipes.values());
    }

    private String generateRecipeKey(Recipe recipe) {
        // Create a key based on title and main ingredients for deduplication
        String title = recipe.getTitle() != null ? recipe.getTitle().toLowerCase().trim() : "";
        String ingredients = recipe.getIngredients().stream()
            .map(ing -> ing.getName().toLowerCase())
            .sorted()
            .collect(Collectors.joining(","));
        
        return title + "|" + ingredients;
    }

    public Page<Recipe> searchRecipes(String query, Set<String> dietTags, String cuisine, 
                                    Recipe.Difficulty difficulty, Integer maxTime, Pageable pageable) {
        if (query != null && !query.isEmpty()) {
            return recipeRepository.findByTextSearch(query, pageable);
        } else {
            // Use filter-based search
            List<Recipe> filtered = recipeRepository.findByFilters(dietTags, cuisine, difficulty, maxTime);
            // Convert to Page (simplified implementation)
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<Recipe> pageContent = start < filtered.size() ? filtered.subList(start, end) : Collections.emptyList();
            
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filtered.size());
        }
    }

    public Recipe saveRecipe(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    public Optional<Recipe> findById(String id) {
        return recipeRepository.findById(id);
    }

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    public void deleteById(String id) {
        recipeRepository.deleteById(id);
    }

    /**
     * Apply user filters to a recipe
     * @param recipe Recipe to check
     * @param request User's request with filters
     * @return true if recipe passes all filters
     */
    private boolean applyFilters(Recipe recipe, RecipeRequest request) {
        // We intentionally do NOT filter by diet tags here.
        // Any dietary adaptation will be handled downstream by the LLM so that
        // recipes that do not yet match the requested diet can still be
        // considered and modified. This prevents valid recipes from being
        // excluded prematurely.

        // Cuisine filter
        if (request.getCuisine() != null && !request.getCuisine().isEmpty()) {
            if (recipe.getCuisine() == null || 
                !recipe.getCuisine().toLowerCase().contains(request.getCuisine().toLowerCase())) {
                return false;
            }
        }

        // Difficulty filter
        if (request.getDifficulty() != null) {
            if (recipe.getDifficulty() == null || recipe.getDifficulty() != request.getDifficulty()) {
                return false;
            }
        }

        // Max time filter
        if (request.getMaxTimeMinutes() != null && recipe.getTimeMinutes() != null) {
            if (recipe.getTimeMinutes() > request.getMaxTimeMinutes()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Scale recipe ingredients based on requested portion size
     * @param recipe Original recipe
     * @param requestedServings Number of servings requested
     * @return Recipe with scaled ingredients
     */
    private Recipe scaleRecipeForServings(Recipe recipe, int requestedServings) {
        // Default recipe servings (assuming 4 based on seed data)
        int defaultServings = 1;
        
        if (requestedServings == defaultServings) {
            return recipe; // No scaling needed
        }

        // Calculate scaling factor
        double scalingFactor = (double) requestedServings / defaultServings;

        // Create new recipe with scaled ingredients
        Recipe scaledRecipe = Recipe.builder()
            .id(recipe.getId())
            .title(recipe.getTitle())
            .ingredients(scaleIngredients(recipe.getIngredients(), scalingFactor))
            .steps(recipe.getSteps())
            .timeMinutes(recipe.getTimeMinutes())
            .difficulty(recipe.getDifficulty())
            .cuisine(recipe.getCuisine())
            .dietTags(recipe.getDietTags())
            .source(recipe.getSource())
            .nutrition(scaleNutrition(recipe.getNutrition(), scalingFactor))
            .build();

        return scaledRecipe;
    }

    /**
     * Scale ingredient quantities based on scaling factor
     * @param ingredients Original ingredients
     * @param scalingFactor Factor to scale by
     * @return Scaled ingredients
     */
    private List<Recipe.Ingredient> scaleIngredients(List<Recipe.Ingredient> ingredients, double scalingFactor) {
        if (ingredients == null) {
            return null;
        }

        return ingredients.stream()
            .map(ingredient -> Recipe.Ingredient.builder()
                .name(ingredient.getName())
                .quantity(scaleQuantity(ingredient.getQuantity(), scalingFactor))
                .unit(ingredient.getUnit())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Scale a quantity string based on scaling factor
     * @param quantity Original quantity string
     * @param scalingFactor Factor to scale by
     * @return Scaled quantity string
     */
    private String scaleQuantity(String quantity, double scalingFactor) {
        if (quantity == null || quantity.isEmpty() || quantity.equalsIgnoreCase("to taste")) {
            return quantity; // Don't scale non-numeric quantities
        }

        try {
            // Handle fractions (e.g., "1/2", "3/4")
            if (quantity.contains("/")) {
                String[] parts = quantity.split("/");
                if (parts.length == 2) {
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    double value = numerator / denominator;
                    double scaledValue = value * scalingFactor;
                    return formatScaledQuantity(scaledValue);
                }
            }

            // Handle decimal numbers
            double value = Double.parseDouble(quantity);
            double scaledValue = value * scalingFactor;
            return formatScaledQuantity(scaledValue);

        } catch (NumberFormatException e) {
            // If parsing fails, return original quantity
            return quantity;
        }
    }

    /**
     * Format scaled quantity with appropriate precision
     * @param value Scaled numeric value
     * @return Formatted quantity string
     */
    private String formatScaledQuantity(double value) {
        if (value == Math.round(value)) {
            return String.valueOf((int) value);
        } else if (value * 2 == Math.round(value * 2)) {
            // Handle common fractions like 0.5, 1.5, 2.5
            int whole = (int) value;
            double fraction = value - whole;
            if (fraction == 0.5) {
                return whole == 0 ? "1/2" : whole + " 1/2";
            }
        }
        return String.format("%.1f", value);
    }

    /**
     * Scale nutrition values based on scaling factor
     * @param recipe Original recipe
     * @param scalingFactor Factor to scale by
     * @return Scaled nutrition
     */
    private Recipe.Nutrition scaleNutrition(Recipe.Nutrition nutrition, double scalingFactor) {
        if (nutrition == null) {
            return null;
        }

        return Recipe.Nutrition.builder()
            .kcal((int) Math.round(nutrition.getKcal() * scalingFactor))
            .protein(Math.round(nutrition.getProtein() * scalingFactor * 10.0) / 10.0)
            .carbs(Math.round(nutrition.getCarbs() * scalingFactor * 10.0) / 10.0)
            .fat(Math.round(nutrition.getFat() * scalingFactor * 10.0) / 10.0)
            .build();
    }

    /**
     * Calculate ingredient coverage percentage (how many of the recipe's ingredients the user has)
     * @param userIngredients User's ingredients
     * @param recipe Recipe to check
     * @return Coverage percentage (0.0 to 1.0)
     */
    private double calculateIngredientCoverage(List<String> userIngredients, Recipe recipe) {
        if (userIngredients == null || userIngredients.isEmpty() || 
            recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return 0.0;
        }

        Set<String> userIngredientsSet = userIngredients.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        Set<String> recipeIngredientsSet = recipe.getIngredients().stream()
            .map(ingredient -> ingredient.getName().toLowerCase())
            .collect(Collectors.toSet());

        // Calculate how many recipe ingredients the user has
        Set<String> intersection = new HashSet<>(userIngredientsSet);
        intersection.retainAll(recipeIngredientsSet);

        // Coverage = user's ingredients that match recipe / total recipe ingredients
        return (double) intersection.size() / recipeIngredientsSet.size();
    }

    // Helper class for scored recipes
    private static class ScoredRecipe {
        private final Recipe recipe;
        private final double score;

        public ScoredRecipe(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public double getScore() {
            return score;
        }
    }
}



