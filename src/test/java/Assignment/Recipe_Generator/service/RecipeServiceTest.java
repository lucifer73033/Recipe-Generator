package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.dto.RecipeRequest;
import Assignment.Recipe_Generator.dto.RecipeResponse;
import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private OpenRouterService openRouterService;



    @Mock
    private LogService logService;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe testRecipe;
    private RecipeRequest testRequest;

    @BeforeEach
    void setUp() {
        testRecipe = Recipe.builder()
            .id("test-id")
            .title("Test Recipe")
            .ingredients(Arrays.asList(
                Recipe.Ingredient.builder().name("chicken").quantity("500").unit("g").build(),
                Recipe.Ingredient.builder().name("tomato").quantity("2").unit("medium").build()
            ))
            .steps(Arrays.asList("Step 1", "Step 2"))
            .timeMinutes(30)
            .difficulty(Recipe.Difficulty.EASY)
            .cuisine("Italian")
            .dietTags(Set.of())
            .source(Recipe.Source.DB)
            .build();

        testRequest = RecipeRequest.builder()
            .ingredients(Arrays.asList("chicken", "tomato"))
            .servings(4)
            .build();
    }

    @Test
    void testCalculateMatchScore_PerfectMatch() {
        // Given
        when(recipeRepository.findAll()).thenReturn(Arrays.asList(testRecipe));

        // When
        RecipeResponse results = recipeService.generateRecipes(testRequest, "test-user");

        // Then
        assertNotNull(results);
        assertNotNull(results.getRecipes());
        assertFalse(results.getRecipes().isEmpty());
        verify(logService).logRecipeGeneration(eq("test-user"), any(), anyInt(), any());
    }

    @Test
    void testCalculateMatchScore_PartialMatch() {
        // Given
        Recipe partialMatchRecipe = Recipe.builder()
            .id("partial-id")
            .title("Partial Match Recipe")
            .ingredients(Arrays.asList(
                Recipe.Ingredient.builder().name("chicken").quantity("500").unit("g").build(),
                Recipe.Ingredient.builder().name("onion").quantity("1").unit("medium").build(),
                Recipe.Ingredient.builder().name("garlic").quantity("3").unit("cloves").build()
            ))
            .steps(Arrays.asList("Step 1"))
            .timeMinutes(25)
            .difficulty(Recipe.Difficulty.MEDIUM)
            .cuisine("Asian")
            .dietTags(Set.of())
            .source(Recipe.Source.DB)
            .build();

        when(recipeRepository.findAll()).thenReturn(Arrays.asList(partialMatchRecipe));

        // When
        RecipeResponse results = recipeService.generateRecipes(testRequest, "test-user");

        // Then
        assertNotNull(results);
        assertNotNull(results.getRecipes());
        // Should still return results even with partial match
        verify(logService).logRecipeGeneration(eq("test-user"), any(), anyInt(), any());
    }

    @Test
    void testGenerateRecipes_FallbackToLLM() {
        // Given - No good matches in DB
        Recipe poorMatchRecipe = Recipe.builder()
            .id("poor-id")
            .title("Poor Match Recipe")
            .ingredients(Arrays.asList(
                Recipe.Ingredient.builder().name("beef").quantity("500").unit("g").build(),
                Recipe.Ingredient.builder().name("potato").quantity("3").unit("medium").build()
            ))
            .steps(Arrays.asList("Step 1"))
            .timeMinutes(60)
            .difficulty(Recipe.Difficulty.HARD)
            .source(Recipe.Source.DB)
            .build();

        Recipe llmRecipe = Recipe.builder()
            .title("LLM Generated Recipe")
            .ingredients(Arrays.asList(
                Recipe.Ingredient.builder().name("chicken").quantity("400").unit("g").build(),
                Recipe.Ingredient.builder().name("tomato").quantity("2").unit("large").build()
            ))
            .steps(Arrays.asList("LLM Step 1", "LLM Step 2"))
            .timeMinutes(25)
            .difficulty(Recipe.Difficulty.EASY)
            .source(Recipe.Source.LLM)
            .build();

        when(recipeRepository.findAll()).thenReturn(Arrays.asList(poorMatchRecipe));
        when(openRouterService.generateRecipes(any(), any())).thenReturn(Arrays.asList(llmRecipe));

        // When
        RecipeResponse results = recipeService.generateRecipes(testRequest, "test-user");

        // Then
        assertNotNull(results);
        assertNotNull(results.getRecipes());
        verify(openRouterService).generateRecipes(any(), eq("test-user"));
        verify(logService).logRecipeGeneration(eq("test-user"), contains("llm"), anyInt(), any());
    }

    @Test
    void testFindById_RecipeExists() {
        // Given
        when(recipeRepository.findById("test-id")).thenReturn(Optional.of(testRecipe));

        // When
        Optional<Recipe> result = recipeService.findById("test-id");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Recipe", result.get().getTitle());
    }

    @Test
    void testFindById_RecipeNotExists() {
        // Given
        when(recipeRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<Recipe> result = recipeService.findById("non-existent");

        // Then
        assertFalse(result.isPresent());
    }
}



