package Assignment.Recipe_Generator.repository;

import Assignment.Recipe_Generator.model.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RecipeRepository extends MongoRepository<Recipe, String> {
    
    // Text search in title and ingredients
    @Query("{ $text: { $search: ?0 } }")
    Page<Recipe> findByTextSearch(String searchText, Pageable pageable);
    
    // Filter by diet tags
    List<Recipe> findByDietTagsIn(Set<String> dietTags);
    
    // Filter by cuisine
    List<Recipe> findByCuisineIgnoreCase(String cuisine);
    
    // Filter by difficulty
    List<Recipe> findByDifficulty(Recipe.Difficulty difficulty);
    
    // Filter by time
    List<Recipe> findByTimeMinutesLessThanEqual(Integer maxTime);
    
    // Complex query combining multiple filters
    @Query("{ " +
           "$and: [" +
           "  { $or: [ { 'dietTags': { $in: ?0 } }, { $expr: { $eq: [{ $size: ?0 }, 0] } } ] }," +
           "  { $or: [ { 'cuisine': { $regex: ?1, $options: 'i' } }, { $expr: { $eq: [?1, ''] } } ] }," +
           "  { $or: [ { 'difficulty': ?2 }, { $expr: { $eq: [?2, null] } } ] }," +
           "  { $or: [ { 'timeMinutes': { $lte: ?3 } }, { $expr: { $eq: [?3, null] } } ] }" +
           "] }")
    List<Recipe> findByFilters(Set<String> dietTags, String cuisine, Recipe.Difficulty difficulty, Integer maxTime);
    
    // Find by source
    List<Recipe> findBySource(Recipe.Source source);
    
    // Find recipes created by user
    List<Recipe> findByCreatedBy(String userId);
    
    // Get all distinct cuisines
    @Query(value = "{}", fields = "{ 'cuisine': 1 }")
    List<Recipe> findAllCuisines();
    
    // Delete by source
    void deleteBySource(Recipe.Source source);
}
