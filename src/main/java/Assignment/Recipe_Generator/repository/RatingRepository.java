package Assignment.Recipe_Generator.repository;

import Assignment.Recipe_Generator.model.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {
    
    Optional<Rating> findByUserIdAndRecipeId(String userId, String recipeId);
    
    List<Rating> findByUserId(String userId);
    
    List<Rating> findByRecipeId(String recipeId);
    
    @Query("{ 'recipeId': ?0 }")
    List<Rating> findAllByRecipeId(String recipeId);
    
    // Calculate average rating for a recipe
    @Query(value = "{ 'recipeId': ?0 }", 
           fields = "{ 'stars': 1 }")
    List<Rating> findStarsByRecipeId(String recipeId);
    
    void deleteByUserIdAndRecipeId(String userId, String recipeId);
}



