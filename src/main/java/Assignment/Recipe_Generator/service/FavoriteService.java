package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.model.Favorite;
import Assignment.Recipe_Generator.model.Recipe;
import Assignment.Recipe_Generator.repository.FavoriteRepository;
import Assignment.Recipe_Generator.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    private final LogService logService;

    public Favorite addToFavorites(String userId, String recipeId) {
        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            throw new IllegalStateException("Recipe already in favorites");
        }
        
        Favorite favorite = Favorite.builder()
            .userId(userId)
            .recipeId(recipeId)
            .build();
        
        Favorite saved = favoriteRepository.save(favorite);
        
        logService.logUserActivity("recipe_favorited", userId, 
            java.util.Map.of("recipeId", recipeId));
        
        return saved;
    }

    public void removeFromFavorites(String userId, String recipeId) {
        favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
        
        logService.logUserActivity("recipe_unfavorited", userId, 
            java.util.Map.of("recipeId", recipeId));
    }

    public boolean isFavorited(String userId, String recipeId) {
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    public List<Recipe> getUserFavoriteRecipes(String userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        
        List<String> recipeIds = favorites.stream()
            .map(Favorite::getRecipeId)
            .collect(Collectors.toList());
        
        return recipeRepository.findAllById(recipeIds);
    }

    public List<Favorite> getUserFavorites(String userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public long getFavoriteCount(String recipeId) {
        return favoriteRepository.countByRecipeId(recipeId);
    }

    public List<Favorite> getRecipeFavorites(String recipeId) {
        return favoriteRepository.findByRecipeId(recipeId);
    }

    public Optional<Favorite> findByUserAndRecipe(String userId, String recipeId) {
        return favoriteRepository.findByUserIdAndRecipeId(userId, recipeId);
    }

    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    public void deleteById(String id) {
        favoriteRepository.deleteById(id);
    }
}



