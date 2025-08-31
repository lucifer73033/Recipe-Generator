package Assignment.Recipe_Generator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "ratings")
@CompoundIndex(def = "{'userId': 1, 'recipeId': 1}", unique = true)
public class Rating {
    
    @Id
    private String id;
    
    @NotNull
    private String userId;
    
    @NotNull
    private String recipeId;
    
    @Min(1)
    @Max(5)
    private Integer stars;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
}



