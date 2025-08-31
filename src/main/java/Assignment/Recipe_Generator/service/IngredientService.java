package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientService {

    private final RecipeRepository recipeRepository;

    /**
     * Get the complete list of master ingredients dynamically from all stored recipes in the database
     */
    public Set<String> getMasterIngredientsList() {
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();
            
            Set<String> masterIngredients = allRecipes.stream()
                .flatMap(recipe -> recipe.getIngredients().stream())
                .map(ingredient -> ingredient.getName().toLowerCase().trim())
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
            
            log.debug("Extracted {} unique ingredients from {} recipes in database", 
                     masterIngredients.size(), allRecipes.size());
            
            return masterIngredients;
            
        } catch (Exception e) {
            log.error("Error extracting master ingredients from database", e);
            // Return empty set as fallback
            return new HashSet<>();
        }
    }

    /**
     * Calculate the percentage match between user ingredients and a recipe's ingredients
     * @param userIngredients List of ingredients provided by user
     * @param recipe Recipe to compare against
     * @return Match percentage (0.0 to 1.0)
     */
    public double calculateIngredientMatchPercentage(List<String> userIngredients, Recipe recipe) {
        if (userIngredients == null || userIngredients.isEmpty() || 
            recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return 0.0;
        }

        // Convert to lowercase for case-insensitive comparison
        Set<String> userIngredientsSet = userIngredients.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        Set<String> recipeIngredientsSet = recipe.getIngredients().stream()
            .map(ingredient -> ingredient.getName().toLowerCase())
            .collect(Collectors.toSet());

        // Calculate intersection (common ingredients)
        Set<String> intersection = new HashSet<>(userIngredientsSet);
        intersection.retainAll(recipeIngredientsSet);

        // Calculate union (all unique ingredients)
        Set<String> union = new HashSet<>(userIngredientsSet);
        union.addAll(recipeIngredientsSet);

        // Jaccard similarity: intersection / union
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Filter recipes by ingredient match threshold
     * @param recipes List of recipes to filter
     * @param userIngredients User's ingredients
     * @param threshold Minimum match percentage (e.g., 0.5 for 50%)
     * @return Filtered list of recipes sorted by match percentage (highest first)
     */
    public List<Recipe> filterRecipesByMatchThreshold(List<Recipe> recipes, List<String> userIngredients, double threshold) {
        if (recipes == null || recipes.isEmpty() || userIngredients == null || userIngredients.isEmpty()) {
            return new ArrayList<>();
        }

        return recipes.stream()
            .filter(recipe -> {
                double matchPercentage = calculateIngredientMatchPercentage(userIngredients, recipe);
                return matchPercentage >= threshold;
            })
            .sorted((r1, r2) -> {
                double match1 = calculateIngredientMatchPercentage(userIngredients, r1);
                double match2 = calculateIngredientMatchPercentage(userIngredients, r2);
                return Double.compare(match2, match1); // Descending order
            })
            .collect(Collectors.toList());
    }

    /**
     * Check if an ingredient exists in the master ingredients list
     * @param ingredient Ingredient name to check
     * @return True if ingredient exists in master list
     */
    public boolean isInMasterList(String ingredient) {
        Set<String> masterIngredients = getMasterIngredientsList();
        return masterIngredients.contains(ingredient.toLowerCase().trim());
    }

    /**
     * Get the standardized name from master list if ingredient matches
     * @param ingredient Ingredient name to standardize
     * @return Standardized name from master list, or original name if not found
     */
    public String getStandardizedIngredientName(String ingredient) {
        Set<String> masterIngredients = getMasterIngredientsList();
        String lowerIngredient = ingredient.toLowerCase().trim();
        return masterIngredients.stream()
            .filter(masterIngredient -> masterIngredient.equals(lowerIngredient))
            .findFirst()
            .orElse(ingredient); // Return original if not found
    }

    /**
     * Separate ingredients into those that match master list vs those that don't
     * @param ingredients List of ingredient names
     * @return Map with "matched" and "unmatched" keys containing respective lists
     */
    public Map<String, List<String>> categorizeIngredients(List<String> ingredients) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("matched", new ArrayList<>());
        result.put("unmatched", new ArrayList<>());

        if (ingredients != null) {
            for (String ingredient : ingredients) {
                if (isInMasterList(ingredient)) {
                    result.get("matched").add(getStandardizedIngredientName(ingredient));
                } else {
                    result.get("unmatched").add(ingredient);
                }
            }
        }

        return result;
    }

    /**
     * Find recipes where user has ALL required ingredients but the match percentage is still low
     * This happens when user has few ingredients but recipe needs many more
     * @param recipes List of recipes to check
     * @param userIngredients User's ingredients
     * @return List of recipes where user has all ingredients but needs more for completion
     */
    public List<Recipe> findRecipesWhereUserHasAllIngredients(List<Recipe> recipes, List<String> userIngredients) {
        if (recipes == null || recipes.isEmpty() || userIngredients == null || userIngredients.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert user ingredients to lowercase for comparison
        Set<String> userIngredientsSet = userIngredients.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return recipes.stream()
            .filter(recipe -> {
                if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
                    return false;
                }

                // Get recipe ingredients
                Set<String> recipeIngredientsSet = recipe.getIngredients().stream()
                    .map(ingredient -> ingredient.getName().toLowerCase())
                    .collect(Collectors.toSet());

                // Check if user has ALL ingredients needed for this recipe
                return userIngredientsSet.containsAll(recipeIngredientsSet);
            })
            .collect(Collectors.toList());
    }



    /**
     * Enhanced recipe filtering that separates high-match, low-match, and "user-has-all" recipes
     * @param recipes List of recipes to filter
     * @param userIngredients User's ingredients  
     * @param threshold Match threshold (e.g., 0.5 for 50%)
     * @return Map with "highMatch", "userHasAll", and "other" categories
     */
    public Map<String, List<Recipe>> categorizeRecipesByMatch(List<Recipe> recipes, List<String> userIngredients, double threshold) {
        Map<String, List<Recipe>> result = new HashMap<>();
        result.put("highMatch", new ArrayList<>());
        result.put("userHasAll", new ArrayList<>());
        result.put("other", new ArrayList<>());

        if (recipes == null || recipes.isEmpty() || userIngredients == null || userIngredients.isEmpty()) {
            return result;
        }

        for (Recipe recipe : recipes) {
            double matchPercentage = calculateIngredientMatchPercentage(userIngredients, recipe);
            
            if (matchPercentage >= threshold) {
                result.get("highMatch").add(recipe);
            } else {
                // Check if user has all required ingredients for this recipe
                if (userHasAllIngredientsForRecipe(recipe, userIngredients)) {
                    result.get("userHasAll").add(recipe);
                } else {
                    result.get("other").add(recipe);
                }
            }
        }

        // Sort each category by match percentage
        for (List<Recipe> categoryRecipes : result.values()) {
            categoryRecipes.sort((r1, r2) -> {
                double match1 = calculateIngredientMatchPercentage(userIngredients, r1);
                double match2 = calculateIngredientMatchPercentage(userIngredients, r2);
                return Double.compare(match2, match1);
            });
        }

        return result;
    }

    /**
     * Check if user has all required ingredients for a specific recipe
     * @param recipe Recipe to check
     * @param userIngredients User's ingredients
     * @return true if user has all ingredients needed for this recipe
     */
    private boolean userHasAllIngredientsForRecipe(Recipe recipe, List<String> userIngredients) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty() || 
            userIngredients == null || userIngredients.isEmpty()) {
            return false;
        }

        // Convert user ingredients to lowercase for comparison
        Set<String> userIngredientsSet = userIngredients.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        // Get recipe ingredients
        Set<String> recipeIngredientsSet = recipe.getIngredients().stream()
            .map(ingredient -> ingredient.getName().toLowerCase())
            .collect(Collectors.toSet());

        // Check if user has ALL ingredients needed for this recipe
        return userIngredientsSet.containsAll(recipeIngredientsSet);
    }
}
