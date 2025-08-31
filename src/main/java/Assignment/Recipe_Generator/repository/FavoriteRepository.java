package Assignment.Recipe_Generator.repository;

import Assignment.Recipe_Generator.model.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String> {
    
    Optional<Favorite> findByUserIdAndRecipeId(String userId, String recipeId);
    
    List<Favorite> findByUserId(String userId);
    
    List<Favorite> findByRecipeId(String recipeId);
    
    boolean existsByUserIdAndRecipeId(String userId, String recipeId);
    
    void deleteByUserIdAndRecipeId(String userId, String recipeId);
    
    long countByRecipeId(String recipeId);
}



