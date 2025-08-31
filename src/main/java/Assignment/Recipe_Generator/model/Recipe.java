package Assignment.Recipe_Generator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.TextIndexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "recipes")
public class Recipe {
    
    @Id
    private String id;
    
    @NotBlank
    @TextIndexed
    private String title;
    
    @NotNull
    private List<Ingredient> ingredients;
    
    @NotNull
    private List<String> steps;
    
    @Positive
    private Integer timeMinutes;
    
    @NotNull
    private Difficulty difficulty;
    
    private String cuisine;
    
    @Builder.Default
    private Set<String> dietTags = Set.of();
    
    private Nutrition nutrition;
    
    @NotNull
    private Source source;
    
    private String createdBy; // userId if created by user, null for seeded recipes
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private String imageUrl; // placeholder or actual image URL
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ingredient {
        @NotBlank
        private String name;
        
        private String quantity;
        private String unit;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Nutrition {
        @Min(0)
        private Integer kcal;
        
        @Min(0)
        private Double protein; // grams
        
        @Min(0)
        private Double carbs; // grams
        
        @Min(0)
        private Double fat; // grams
    }
    
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
    public enum Source {
        DB, LLM, FALLBACK
    }
}

