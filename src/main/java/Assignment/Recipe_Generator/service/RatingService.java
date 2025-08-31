package Assignment.Recipe_Generator.service;

import Assignment.Recipe_Generator.model.Rating;
import Assignment.Recipe_Generator.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final LogService logService;

    public Rating rateRecipe(String userId, String recipeId, Integer stars) {
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndRecipeId(userId, recipeId);
        
        Rating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            Integer oldStars = rating.getStars();
            rating.setStars(stars);
            rating.setUpdatedAt(LocalDateTime.now());
            
            logService.logUserActivity("rating_updated", userId, 
                java.util.Map.of("recipeId", recipeId, "oldStars", oldStars, "newStars", stars));
        } else {
            // Create new rating
            rating = Rating.builder()
                .userId(userId)
                .recipeId(recipeId)
                .stars(stars)
                .build();
            
            logService.logUserActivity("rating_created", userId, 
                java.util.Map.of("recipeId", recipeId, "stars", stars));
        }
        
        return ratingRepository.save(rating);
    }

    public Optional<Rating> getUserRating(String userId, String recipeId) {
        return ratingRepository.findByUserIdAndRecipeId(userId, recipeId);
    }

    public List<Rating> getUserRatings(String userId) {
        return ratingRepository.findByUserId(userId);
    }

    public List<Rating> getRecipeRatings(String recipeId) {
        return ratingRepository.findByRecipeId(recipeId);
    }

    public double getAverageRating(String recipeId) {
        List<Rating> ratings = ratingRepository.findStarsByRecipeId(recipeId);
        
        if (ratings.isEmpty()) {
            return 0.0;
        }
        
        return ratings.stream()
            .mapToInt(Rating::getStars)
            .average()
            .orElse(0.0);
    }

    public int getRatingCount(String recipeId) {
        return ratingRepository.findByRecipeId(recipeId).size();
    }

    public void deleteRating(String userId, String recipeId) {
        ratingRepository.deleteByUserIdAndRecipeId(userId, recipeId);
        
        logService.logUserActivity("rating_deleted", userId, 
            java.util.Map.of("recipeId", recipeId));
    }

    public Optional<Rating> findById(String id) {
        return ratingRepository.findById(id);
    }

    public List<Rating> findAll() {
        return ratingRepository.findAll();
    }
}



