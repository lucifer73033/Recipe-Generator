package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.dto.*;
import Assignment.Recipe_Generator.model.Recipe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenRouterService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final LogService logService;
    private final IngredientService ingredientService;

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.base-url}")
    private String baseUrl;

    @Value("${openrouter.model}")
    private String model;

    private WebClient getWebClient() {
        return webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("HTTP-Referer", "https://github.com/your-repo/recipe-generator")
            .defaultHeader("X-Title", "Smart Recipe Generator")
            .build();
    }

    public IngredientRecognition recognizeIngredients(byte[] imageBytes, String userId) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            // Get master ingredients list for the prompt
            Set<String> masterIngredients = ingredientService.getMasterIngredientsList();
            String masterIngredientsList = String.join(", ", masterIngredients);

            String promptText = String.format(
                "Here is a list of known ingredients: [%s]. " +
                "For each ingredient you recognize in this image: " +
                "- If it matches an ingredient in the provided list, return exactly that ingredient name from the list " +
                "- If it doesn't match any ingredient in the list, return the ingredient name as you recognize it " +
                "Return JSON: [{\"name\": \"ingredient\", \"confidence\": 0.95}]. " +
                "No brands. No cookware. Be specific but concise.",
                masterIngredientsList
            );
            
            log.info("=== INGREDIENT RECOGNITION LLM REQUEST ===");
            log.info("Prompt: {}", promptText);

            List<OpenRouterRequest.Content> content = List.of(
                OpenRouterRequest.Content.builder()
                    .type("text")
                    .text(promptText)
                    .build(),
                OpenRouterRequest.Content.builder()
                    .type("image_url")
                    .imageUrl(OpenRouterRequest.ImageUrl.builder()
                        .url(imageUrl)
                        .detail("low")
                        .build())
                    .build()
            );

            OpenRouterRequest request = OpenRouterRequest.builder()
                .model(model)
                .messages(List.of(
                    OpenRouterRequest.Message.builder()
                        .role("user")
                        .content(content)
                        .build()
                ))
                .maxTokens(500)
                .temperature(0.3)
                .responseFormat(OpenRouterRequest.ResponseFormat.builder()
                    .type("json_object")
                    .build())
                .build();
                


            Map<String, Object> metadata = Map.of(
                "operation", "image_recognition",
                "imageSize", imageBytes.length,
                "model", model
            );

            logService.logLLMCall("recognizeIngredients", userId, metadata);

            OpenRouterResponse response = callOpenRouter(request);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content_response = response.getChoices().get(0).getMessage().getContent();
                
                log.info("=== INGREDIENT RECOGNITION LLM RESPONSE ===");
                log.info("Response: {}", content_response);
                
                IngredientRecognition result = parseIngredientRecognition(content_response);
                return result;
            }

            return getEmptyIngredientRecognition();

        } catch (Exception e) {
            log.error("Error recognizing ingredients from image", e);
            logService.logSystemEvent("ingredient_recognition_error", 
                Map.of("error", e.getMessage(), "userId", userId), 
                "ERROR");
            return getEmptyIngredientRecognition();
        }
    }

    public List<Recipe> generateSingleRecipe(RecipeRequest request, String userId, List<String> excludeTitles) {
        try {
            String prompt = buildRecipeGenerationPrompt(request, excludeTitles);
            
            log.info("=== RECIPE GENERATION LLM REQUEST ===");
            log.info("Prompt: {}", prompt);

            OpenRouterRequest llmRequest = OpenRouterRequest.builder()
                .model(model)
                .messages(List.of(
                    OpenRouterRequest.Message.builder()
                        .role("system")
                        .content("You are a culinary assistant creating practical, detailed, safe recipes. Always return valid JSON.")
                        .build(),
                    OpenRouterRequest.Message.builder()
                        .role("user")
                        .content(prompt)
                        .build()
                ))
                .maxTokens(4096)
                .temperature(0.7)
                .responseFormat(OpenRouterRequest.ResponseFormat.builder()
                    .type("json_object")
                    .build())
                .build();

            Map<String, Object> metadata = Map.of(
                "operation", "recipe_generation_single",
                "ingredients", request.getIngredients(),
                "dietTags", request.getDietTags() != null ? request.getDietTags() : Set.of(),
                "servings", request.getServings(),
                "model", model
            );

            logService.logLLMCall("generateRecipes", userId, metadata);

            OpenRouterResponse response = callOpenRouter(llmRequest);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                
                log.info("=== RECIPE GENERATION LLM RESPONSE ===");
                log.info("Response: {}", content);
                
                List<Recipe> recipes = parseRecipeResponse(content);
                return recipes;
            }

            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error generating recipes", e);
            logService.logSystemEvent("recipe_generation_error", 
                Map.of("error", e.getMessage(), "userId", userId), 
                "ERROR");
            return Collections.emptyList();
        }
    }

    // Legacy method kept for backward-compatibility with existing tests. It simply
    // delegates to the new single-recipe generator and returns a list of one
    // recipe. Tests that expected a list will still compile.
    @Deprecated
    public List<Recipe> generateRecipes(RecipeRequest request, String userId) {
        return generateSingleRecipe(request, userId, java.util.Collections.emptyList());
    }

    /**
     * Modify existing DB recipes to accommodate dietary preferences
     */
    public List<Recipe> modifyRecipesForDietaryPreferences(List<Recipe> dbRecipes, RecipeRequest request, String userId) {
        try {
            String prompt = buildDietaryModificationPrompt(dbRecipes, request);
            
            log.info("=== DIETARY MODIFICATION LLM REQUEST ===");
            log.info("Prompt: {}", prompt);

            OpenRouterRequest llmRequest = OpenRouterRequest.builder()
                .model(model)
                .messages(List.of(
                    OpenRouterRequest.Message.builder()
                        .role("system")
                        .content("You are a culinary expert who modifies existing recipes to accommodate dietary restrictions while maintaining the original flavor and structure. Always return valid JSON.")
                        .build(),
                    OpenRouterRequest.Message.builder()
                        .role("user")
                        .content(prompt)
                        .build()
                ))
                .maxTokens(2000)
                .temperature(0.6)
                .responseFormat(OpenRouterRequest.ResponseFormat.builder()
                    .type("json_object")
                    .build())
                .build();

            Map<String, Object> metadata = Map.of(
                "operation", "dietary_modification",
                "recipesCount", dbRecipes.size(),
                "dietTags", request.getDietTags() != null ? request.getDietTags() : Set.of(),
                "model", model
            );

            logService.logLLMCall("modifyRecipesForDietaryPreferences", userId, metadata);

            OpenRouterResponse response = callOpenRouter(llmRequest);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                
                log.info("=== DIETARY MODIFICATION LLM RESPONSE ===");
                log.info("Response: {}", content);
                
                List<Recipe> modifiedRecipes = parseRecipeResponse(content);
                
                // Mark these as LLM-generated but based on DB recipes
                modifiedRecipes.forEach(recipe -> {
                    recipe.setSource(Recipe.Source.LLM);
                    // You could add a note that this was modified from a DB recipe
                });
                
                return modifiedRecipes;
            }

            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error modifying recipes for dietary preferences", e);
            return Collections.emptyList();
        }
    }

    private OpenRouterResponse callOpenRouter(OpenRouterRequest request) {
        try {
            OpenRouterResponse response = getWebClient()
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();
                
            return response;
            
        } catch (Exception e) {
            log.error("OpenRouter API call failed: {}", e.getMessage());
            throw e;
        }
    }

    private String buildRecipeGenerationPrompt(RecipeRequest request, List<String> excludeTitles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate 1 practical recipe using these ingredients: ")
              .append(String.join(", ", request.getIngredients()));

        if (request.getDietTags() != null && !request.getDietTags().isEmpty()) {
            prompt.append(". Dietary requirements: ").append(String.join(", ", request.getDietTags()));
            prompt.append(". Ensure all recipes strictly follow these dietary restrictions.");
        }

        if (request.getCuisine() != null && !request.getCuisine().isEmpty()) {
            prompt.append(". Preferred cuisine: ").append(request.getCuisine());
        }

        if (request.getMaxTimeMinutes() != null) {
            prompt.append(". Maximum cooking time: ").append(request.getMaxTimeMinutes()).append(" minutes");
        }

        if (request.getDifficulty() != null) {
            prompt.append(". Difficulty: ").append(request.getDifficulty().name().toLowerCase());
        }

        prompt.append(". Number of people: ").append(request.getServings());

        if (excludeTitles != null && !excludeTitles.isEmpty()) {
            prompt.append(". Do NOT create a recipe with these titles: ")
                  .append(String.join(", ", excludeTitles));
        }

        prompt.append("\n\nIMPORTANT: Return ONLY valid JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"Recipe Title\",\n");
        prompt.append("  \"timeMinutes\": 30,\n");
        prompt.append("  \"difficulty\": \"EASY\",\n");
        prompt.append("  \"cuisine\": \"Cuisine Name\",\n");
        prompt.append("  \"ingredients\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"Ingredient Name\",\n");
        prompt.append("      \"quantity\": \"2\",\n");
        prompt.append("      \"unit\": \"cups\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"steps\": [\n");
        prompt.append("    \"Step 1 description\",\n");
        prompt.append("    \"Step 2 description\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"nutrition\": {\n");
        prompt.append("    \"kcal\": 400,\n");
        prompt.append("    \"protein\": 20.0,\n");
        prompt.append("    \"carbs\": 45.0,\n");
        prompt.append("    \"fat\": 15.0\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("\nDo not include any text before or after the JSON. Only return the JSON object.");

        return prompt.toString();
    }

    private String buildDietaryModificationPrompt(List<Recipe> dbRecipes, RecipeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a culinary expert who modifies existing recipes to accommodate dietary restrictions while maintaining the original flavor and structure. Given the following DB recipes:\n");
        
        for (int i = 0; i < dbRecipes.size(); i++) {
            Recipe recipe = dbRecipes.get(i);
            prompt.append("Recipe ").append(i+1).append(": ").append(recipe.getTitle()).append(" (").append(recipe.getDifficulty().name().toLowerCase()).append(")\n");
            prompt.append("Ingredients: ").append(recipe.getIngredients().stream().map(Recipe.Ingredient::getName).collect(Collectors.joining(", "))).append("\n");
            prompt.append("Steps: ").append(recipe.getSteps().stream().collect(Collectors.joining("; "))).append("\n");
            // Do not include nutrition values to reduce prompt size
            prompt.append("--- End Recipe ").append(i+1).append(" ---\n");
        }

        prompt.append("Number of people to serve: ").append(request.getServings()).append(".\n");
        prompt.append("Your task is to modify these recipes to accommodate the following dietary preferences: ").append(String.join(", ", request.getDietTags())).append(".\n");
        prompt.append("For each recipe, if an ingredient is not suitable for the dietary preference, replace it with a suitable alternative. If a step is not suitable for the dietary preference, modify it. Always return valid JSON.\n");
        
        prompt.append("\nIMPORTANT: Return ONLY valid JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"recipes\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"Modified Recipe Title 1\",\n");
        prompt.append("      \"timeMinutes\": 30,\n");
        prompt.append("      \"difficulty\": \"EASY\",\n");
        prompt.append("      \"cuisine\": \"Cuisine Name\",\n");
        prompt.append("      \"ingredients\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"name\": \"Ingredient Name\",\n");
        prompt.append("          \"quantity\": \"2\",\n");
        prompt.append("          \"unit\": \"cups\"\n");
        prompt.append("        }\n");
        prompt.append("      ],\n");
        prompt.append("      \"steps\": [\n");
        prompt.append("        \"Modified step 1 description\",\n");
        prompt.append("        \"Modified step 2 description\"\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"Modified Recipe Title 2\",\n");
        prompt.append("      \"timeMinutes\": 45,\n");
        prompt.append("      \"difficulty\": \"MEDIUM\",\n");
        prompt.append("      \"cuisine\": \"Cuisine Name\",\n");
        prompt.append("      \"ingredients\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"name\": \"Ingredient Name\",\n");
        prompt.append("          \"quantity\": \"1\",\n");
        prompt.append("          \"unit\": \"cup\"\n");
        prompt.append("        }\n");
        prompt.append("      ],\n");
        prompt.append("      \"steps\": [\n");
        prompt.append("        \"Modified step 1 description\",\n");
        prompt.append("        \"Modified step 2 description\"\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("\nDo not include any text before or after the JSON. Only return the JSON object.");

        return prompt.toString();
    }

    private IngredientRecognition parseIngredientRecognition(String jsonResponse) {
        try {
            // First try to parse as the expected structure
            try {
                IngredientRecognition result = objectMapper.readValue(jsonResponse, IngredientRecognition.class);
                return result;
            } catch (JsonProcessingException e) {
                // Try to parse as direct array
                try {
                    List<Map<String, Object>> ingredientsArray = objectMapper.readValue(jsonResponse, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    
                    // Convert to IngredientRecognition format
                    List<IngredientRecognition.RecognizedIngredient> recognizedIngredients = new ArrayList<>();
                    for (Map<String, Object> ingredientMap : ingredientsArray) {
                        String name = (String) ingredientMap.get("name");
                        Object confidenceObj = ingredientMap.get("confidence");
                        Double confidence = confidenceObj instanceof Number ? ((Number) confidenceObj).doubleValue() : 0.95;
                        
                        if (name != null && !name.trim().isEmpty()) {
                            recognizedIngredients.add(IngredientRecognition.RecognizedIngredient.builder()
                                .name(name.trim())
                                .confidence(confidence)
                                .build());
                        }
                    }
                    
                    IngredientRecognition result = IngredientRecognition.builder()
                        .ingredients(recognizedIngredients)
                        .build();
                    
                    return result;
                    
                } catch (Exception arrayParseError) {
                    throw e; // Re-throw original error
                }
            }
        } catch (Exception e) {
            log.error("Error parsing ingredient recognition response: {}", e.getMessage());
            return getEmptyIngredientRecognition();
        }
    }

    private List<Recipe> parseRecipeResponse(String jsonResponse) {
        try {
            // Clean the response to remove any malformed characters
            String cleanedResponse = cleanJsonResponse(jsonResponse);
            
            // Handle both single recipe objects and arrays of recipes
            if (cleanedResponse.trim().startsWith("[")) {
                // Root-level JSON array
                List<Map<String, Object>> recipesData = objectMapper.readValue(cleanedResponse,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                
                List<Recipe> recipes = new ArrayList<>();
                for (Map<String, Object> recipeData : recipesData) {
                    Recipe recipe = parseRecipeFromMap(recipeData);
                    if (recipe != null) {
                        recipes.add(recipe);
                    }
                }
                return recipes;
            } else {
                Map<String, Object> response = objectMapper.readValue(cleanedResponse, Map.class);
                
                // Check if it's wrapped in a "recipes" array
                if (response.containsKey("recipes")) {
                    List<Map<String, Object>> recipesData = (List<Map<String, Object>>) response.get("recipes");
                    List<Recipe> recipes = new ArrayList<>();
                    for (Map<String, Object> recipeData : recipesData) {
                        Recipe recipe = parseRecipeFromMap(recipeData);
                        if (recipe != null) {
                            recipes.add(recipe);
                        }
                    }
                    return recipes;
                } else {
                    // Single recipe object
                    Recipe recipe = parseRecipeFromMap(response);
                    if (recipe != null) {
                        return Collections.singletonList(recipe);
                    }
                }
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error parsing recipe response: {}", e.getMessage());
            log.error("Raw response: {}", jsonResponse);
            return Collections.emptyList();
        }
    }

    /**
     * Clean JSON response to remove malformed characters and fix common issues
     */
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null) {
            return "";
        }
        
        // Remove any non-printable characters and common Unicode issues
        String cleaned = jsonResponse
            .replaceAll("[\\x00-\\x1F\\x7F]", "") // Remove control characters
            .replaceAll("[\\uFFFD]", "") // Remove replacement characters
            .replaceAll("[\\u3000-\\u303F]", "") // Remove CJK symbols and punctuation
            .replaceAll("[\\uFF00-\\uFFEF]", ""); // Remove halfwidth and fullwidth forms
        
        // Try to find the start and end of valid JSON
        int startBrace = cleaned.indexOf('{');
        int endBrace = cleaned.lastIndexOf('}');
        
        if (startBrace >= 0 && endBrace > startBrace) {
            cleaned = cleaned.substring(startBrace, endBrace + 1);
        }
        

        
        return cleaned;
    }

    private Recipe parseRecipeFromMap(Map<String, Object> recipeData) {

        
        try {
            List<Recipe.Ingredient> ingredients = new ArrayList<>();
            List<Map<String, Object>> ingredientsData = (List<Map<String, Object>>) recipeData.get("ingredients");
            
            if (ingredientsData != null) {
                for (int i = 0; i < ingredientsData.size(); i++) {
                    Map<String, Object> ing = ingredientsData.get(i);
                    
                    Recipe.Ingredient ingredient = Recipe.Ingredient.builder()
                        .name((String) ing.get("name"))
                        .quantity((String) ing.get("quantity"))
                        .unit((String) ing.get("unit"))
                        .build();
                    ingredients.add(ingredient);
                }
            }

            Map<String, Object> nutritionData = (Map<String, Object>) recipeData.get("nutrition");
            Recipe.Nutrition nutrition = null;
            if (nutritionData != null) {
                nutrition = Recipe.Nutrition.builder()
                    .kcal(((Number) nutritionData.get("kcal")).intValue())
                    .protein(((Number) nutritionData.get("protein")).doubleValue())
                    .carbs(((Number) nutritionData.get("carbs")).doubleValue())
                    .fat(((Number) nutritionData.get("fat")).doubleValue())
                    .build();
            }

            String title = (String) recipeData.get("title");
            List<String> steps = (List<String>) recipeData.get("steps");
            Number timeMinutes = (Number) recipeData.get("timeMinutes");
            String difficultyStr = (String) recipeData.get("difficulty");
            String cuisine = (String) recipeData.get("cuisine");

            Recipe recipe = Recipe.builder()
                .title(title)
                .ingredients(ingredients)
                .steps(steps)
                .timeMinutes(timeMinutes != null ? timeMinutes.intValue() : 0)
                .difficulty(difficultyStr != null ? Recipe.Difficulty.valueOf(difficultyStr.toUpperCase()) : Recipe.Difficulty.EASY)
                .cuisine(cuisine)
                .nutrition(nutrition)
                .source(Recipe.Source.LLM)
                .build();
                
            return recipe;

        } catch (Exception e) {
            log.error("Error parsing individual recipe from map: {}", e.getMessage());
            return null;
        }
    }

    private IngredientRecognition getEmptyIngredientRecognition() {
        return IngredientRecognition.builder()
            .ingredients(Collections.emptyList())
            .build();
    }
}



