package Assignment.Recipe_Generator.dto;

import Assignment.Recipe_Generator.model.Recipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeRequest {
    
    @NotNull
    private List<String> ingredients;
    
    private Set<String> dietTags;
    
    @Min(1)
    @Max(300) // 5 hours max
    private Integer maxTimeMinutes;
    
    private Recipe.Difficulty difficulty;
    
    private String cuisine;
    
    @Positive
    private Integer servings = 4;
}



