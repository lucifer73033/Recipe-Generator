package Assignment.Recipe_Generator.dto;

import Assignment.Recipe_Generator.model.Recipe;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeResponse {
    
    private List<Recipe> recipes;
    private RecipeMetadata metadata;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecipeMetadata {
        private int totalRecipes;
        private int highMatchCount;
        private int userHasAllCount;
        private int llmGeneratedCount;
        private String strategy;
        private boolean hasUserHasAllRecipes;
        private String message;
        
        // List of recipe IDs that are "user-has-all" recipes
        private List<String> userHasAllRecipeIds;
    }
}
