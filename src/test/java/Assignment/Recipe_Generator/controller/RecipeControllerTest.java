package Assignment.Recipe_Generator.controller;

import Assignment.Recipe_Generator.dto.RecipeRequest;
import Assignment.Recipe_Generator.dto.RecipeResponse;
import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.service.RecipeService;
import Assignment.Recipe_Generator.service.RatingService;
import Assignment.Recipe_Generator.service.FavoriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private RatingService ratingService;

    @MockBean
    private FavoriteService favoriteService;

    private Recipe testRecipe;
    private RecipeRequest testRequest;

    @BeforeEach
    void setUp() {
        testRecipe = Recipe.builder()
            .id("test-id")
            .title("Test Recipe")
            .ingredients(Arrays.asList(
                Recipe.Ingredient.builder().name("chicken").quantity("500").unit("g").build()
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
    void testGenerateRecipes_Success() throws Exception {
        // Given
        RecipeResponse testResponse = RecipeResponse.builder()
            .recipes(Arrays.asList(testRecipe))
            .metadata(RecipeResponse.RecipeMetadata.builder()
                .totalRecipes(1)
                .highMatchCount(1)
                .userHasAllCount(0)
                .llmGeneratedCount(0)
                .strategy("DB-first")
                .hasUserHasAllRecipes(false)
                .message("Found 1 high-match recipes")
                .userHasAllRecipeIds(Arrays.asList())
                .build())
            .build();
            
        when(recipeService.generateRecipes(any(RecipeRequest.class), any()))
            .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/recipes/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipes").isArray())
            .andExpect(jsonPath("$.recipes[0].title").value("Test Recipe"))
            .andExpect(jsonPath("$.recipes[0].difficulty").value("EASY"))
            .andExpect(jsonPath("$.metadata.totalRecipes").value(1));
    }

    @Test
    void testGenerateRecipes_InvalidRequest() throws Exception {
        // Given - Empty ingredients list
        RecipeRequest invalidRequest = RecipeRequest.builder()
            .ingredients(Arrays.asList())
            .build();

        // When & Then
        mockMvc.perform(post("/api/recipes/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRecipeById_Found() throws Exception {
        // Given
        when(recipeService.findById("test-id")).thenReturn(Optional.of(testRecipe));

        // When & Then
        mockMvc.perform(get("/api/recipes/test-id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Recipe"))
            .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void testGetRecipeById_NotFound() throws Exception {
        // Given
        when(recipeService.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/recipes/non-existent"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testRateRecipe_Success() throws Exception {
        // Given
        when(ratingService.rateRecipe(anyString(), eq("test-id"), eq(5)))
            .thenReturn(null); // Rating object not needed for this test
        when(ratingService.getAverageRating("test-id")).thenReturn(4.5);
        when(ratingService.getRatingCount("test-id")).thenReturn(10);

        // When & Then
        mockMvc.perform(post("/api/recipes/test-id/rate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\": 5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.averageRating").value(4.5))
            .andExpect(jsonPath("$.ratingCount").value(10));
    }

    @Test
    void testRateRecipe_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/recipes/test-id/rate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\": 5}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testRateRecipe_InvalidRating() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/recipes/test-id/rate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\": 6}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSaveRecipe_Success() throws Exception {
        // Given
        when(favoriteService.addToFavorites(anyString(), eq("test-id")))
            .thenReturn(null); // Favorite object not needed for this test

        // When & Then
        mockMvc.perform(post("/api/recipes/test-id/save")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testSaveRecipe_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/recipes/test-id/save")
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }
}
