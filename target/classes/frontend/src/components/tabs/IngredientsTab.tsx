import { useState, useEffect } from 'react';
import { Recipe, RecipeRequest } from '../../types';
import { publicApi, protectedApi } from '../../lib/api';
import RecipeCard from '../RecipeCard';
import LoadingSpinner from '../LoadingSpinner';
import AutocompleteInput from '../AutocompleteInput';
import { useAuth } from '../../contexts/AuthContext';

const CUISINES = [
  'Any Cuisine',
  'Italian',
  'Mediterranean', 
  'Asian',
  'Mexican',
  'Thai',
  'Chinese',
  'Indian',
  'Greek',
  'Modern',
  'Moroccan',
  'Korean',
  'French',
  'Japanese',
  'Brazilian',
  'Turkish'
];

export default function IngredientsTab() {
  const { isAuthenticated, token } = useAuth();
  const [ingredients, setIngredients] = useState<string[]>([]);
  const [newIngredient, setNewIngredient] = useState('');
  const [dietaryPreferences, setDietaryPreferences] = useState<string[]>([]);
  const [difficulty, setDifficulty] = useState<string>('ANY');
  const [maxCookingTime, setMaxCookingTime] = useState<number>(60);
  const [cuisine, setCuisine] = useState<string>('Any Cuisine');
  const [numberOfPeople, setNumberOfPeople] = useState<number>(4);
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [isDragOver, setIsDragOver] = useState(false);
  const [userRatings, setUserRatings] = useState<Map<string, number>>(new Map());
  const [savedRecipes, setSavedRecipes] = useState<Set<string>>(new Set());

  // Load user ratings and saved recipes when component mounts
  useEffect(() => {
    if (isAuthenticated && token) {
      loadUserData();
    }
  }, [isAuthenticated, token]);

  const loadUserData = async () => {
    if (!token) return;
    
    try {
      const [ratings, saved] = await Promise.all([
        protectedApi.user.getRatings(token),
        protectedApi.user.getSavedRecipes(token)
      ]);
      
      // Convert ratings to Map for easy lookup
      const ratingsMap = new Map();
      ratings.forEach((rating: any) => {
        ratingsMap.set(rating.recipeId, rating.stars);
      });
      setUserRatings(ratingsMap);
      
      // Convert saved recipes to Set for easy lookup
      const savedSet = new Set(saved.map((recipe: Recipe) => recipe.id));
      setSavedRecipes(savedSet);
    } catch (error) {
      console.error('Error loading user data:', error);
    }
  };

  const handleRateRecipe = async (recipe: Recipe, stars: number) => {
    if (!isAuthenticated || !token) return;
    
    try {
      let recipeId = recipe.id;
      
      // If this is an LLM recipe (no ID), save it to database first
      if (!recipeId) {
        try {
          const response = await protectedApi.recipes.saveLLM(recipe, token);
          if (response.recipeId) {
            recipeId = response.recipeId;
            // Update the recipe with the new ID
            setRecipes(prev => prev.map(r => 
              r === recipe ? { ...r, id: response.recipeId } : r
            ));
            // Add to saved recipes
            if (response.recipeId) {
              setSavedRecipes(prev => new Set(prev.add(response.recipeId)));
            }
          } else {
            throw new Error('Failed to get recipe ID from save response');
          }
        } catch (saveError) {
          console.error('Failed to save LLM recipe before rating:', saveError);
          setError('Failed to save LLM recipe. Please try again.');
          setTimeout(() => setError(''), 3000);
          return;
        }
      }
      
      // Now rate the recipe (it should have an ID now)
      if (recipeId) {
        await protectedApi.recipes.rate(recipeId, stars, token);
        setUserRatings(prev => new Map(prev.set(recipeId, stars)));
        setSuccess(`Recipe rated ${stars} stars!`);
        setTimeout(() => setSuccess(''), 3000);
      } else {
        throw new Error('Recipe ID is still undefined after saving');
      }
    } catch (error) {
      console.error('Failed to rate recipe:', error);
      setError('Failed to rate recipe. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleSaveRecipe = async (recipe: Recipe) => {
    if (!isAuthenticated || !token) return;
    
    try {
      if (recipe.id) {
        // DB recipe - use regular save endpoint
        await protectedApi.recipes.save(recipe.id, token);
        setSavedRecipes(prev => new Set(prev.add(recipe.id!)));
      } else {
        // LLM recipe - use save-llm endpoint
        const response = await protectedApi.recipes.saveLLM(recipe, token);
        if (response.recipeId) {
          // Update the recipe with the new ID from database
          setRecipes(prev => prev.map(r => 
            r === recipe ? { ...r, id: response.recipeId } : r
          ));
          setSavedRecipes(prev => new Set(prev.add(response.recipeId)));
        }
      }
      setSuccess('Recipe saved to favorites!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (error) {
      console.error('Failed to save recipe:', error);
      setError('Failed to save recipe. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleUnsaveRecipe = async (recipeId: string) => {
    if (!isAuthenticated || !token || !recipeId) return;
    
    try {
      await protectedApi.recipes.unsave(recipeId, token);
      setSavedRecipes(prev => {
        const newSet = new Set(prev);
        newSet.delete(recipeId);
        return newSet;
      });
      setSuccess('Recipe removed from favorites!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (error) {
      console.error('Failed to unsave recipe:', error);
      setError('Failed to remove recipe. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  const addIngredient = () => {
    if (newIngredient.trim() && !ingredients.includes(newIngredient.trim())) {
      setIngredients([...ingredients, newIngredient.trim()]);
      setNewIngredient('');
      setError('');
      setSuccess('');
    }
  };

  const removeIngredient = (index: number) => {
    setIngredients(ingredients.filter((_, i) => i !== index));
  };



  const toggleDietaryPreference = (preference: string) => {
    setDietaryPreferences(prev => 
      prev.includes(preference) 
        ? prev.filter(p => p !== preference)
        : [...prev, preference]
    );
  };

  const handleGenerate = async () => {
    if (ingredients.length === 0) {
      setError('Please add at least one ingredient');
      return;
    }

    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      const request: RecipeRequest = {
          ingredients,
          dietTags: dietaryPreferences.length > 0 ? dietaryPreferences : undefined,
          difficulty: difficulty !== 'ANY' ? difficulty as 'EASY' | 'MEDIUM' | 'HARD' : undefined,
          maxTimeMinutes: maxCookingTime > 0 ? maxCookingTime : undefined,
          servings: numberOfPeople,
          cuisine: cuisine !== 'Any Cuisine' ? cuisine : undefined
        };

      const response = await publicApi.recipes.generate(request);
      setRecipes(response.recipes || []);
    } catch (error) {
      setError('Failed to generate recipes. Please try again.');
      console.error('Error generating recipes:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileUpload = async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    setIsLoading(true);
    setError('');

    try {
      const formData = new FormData();
      formData.append('image', files[0]);

      console.log('Uploading file:', files[0].name, 'Size:', files[0].size, 'Type:', files[0].type);
      
      const response = await publicApi.ingredients.recognize(formData);
      
      if (response.ingredients && response.ingredients.length > 0) {
        const newIngredients = response.ingredients
          .map(ing => ing.name)
          .filter(name => !ingredients.includes(name));
        
        setIngredients([...ingredients, ...newIngredients]);
        setSuccess(`Successfully recognized ${newIngredients.length} ingredients from image!`);
        console.log('Successfully recognized ingredients:', newIngredients);
        
        // Clear success message after 5 seconds
        setTimeout(() => setSuccess(''), 5000);
      } else {
        setError('No ingredients were recognized from the image. Please try a different image.');
      }
    } catch (error) {
      console.error('Error recognizing ingredients:', error);
      if (error instanceof Error) {
        setError(`Failed to recognize ingredients: ${error.message}`);
      } else {
        setError('Failed to recognize ingredients from image. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleFileUpload(files);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-lg font-semibold text-neutral-900 mb-2">
          Generate Recipes from Ingredients
        </h2>
        <p className="text-sm text-neutral-600">
          Enter the ingredients you have available and we'll suggest recipes you can make.
        </p>
      </div>

      {/* File Upload - Moved to top and made bigger */}
      <div className="card p-6 space-y-4">
        <h3 className="font-medium text-neutral-900">Upload Ingredient Image</h3>
        <p className="text-sm text-neutral-600 mb-4">
          Drag and drop an image of your ingredients or click to browse
        </p>
        <div 
          className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
            isDragOver 
              ? 'border-blue-400 bg-blue-50' 
              : 'border-neutral-300 hover:border-neutral-400'
          }`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <input
            type="file"
            accept="image/*"
            onChange={(e) => handleFileUpload(e.target.files)}
            className="hidden"
            id="image-upload"
            disabled={isLoading}
          />
          <label htmlFor="image-upload" className={`block ${isLoading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}`}>
            {isLoading ? (
              <>
                <div className="text-4xl mb-4">⏳</div>
                <div className="text-lg font-medium text-neutral-700 mb-2">
                  Processing image...
                </div>
                <div className="text-sm text-neutral-500">
                  Please wait while we analyze your ingredients
                </div>
              </>
            ) : (
              <>
                <div className="text-4xl mb-4">📸</div>
                <div className="text-lg font-medium text-neutral-700 mb-2">
                  Drop your ingredient image here
                </div>
                <div className="text-sm text-neutral-500">
                  or click to browse files
                </div>
              </>
            )}
          </label>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column - Input */}
        <div className="space-y-4">
          {/* Available Ingredients */}
          <div className="card p-6 space-y-4">
            <h3 className="font-medium text-neutral-900">Available Ingredients</h3>
            
            <div className="flex gap-2">
              <AutocompleteInput
                value={newIngredient}
                onChange={setNewIngredient}
                onAdd={addIngredient}
                placeholder="Enter an ingredient (e.g., chicken)"
                className="flex-1"
              />
            </div>

            {ingredients.length === 0 ? (
              <p className="text-sm text-neutral-500">No ingredients added yet</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {ingredients.map((ingredient, index) => (
                  <div
                    key={index}
                    className="flex items-center gap-2 px-3 py-1 bg-neutral-100 text-neutral-700 rounded-full text-sm"
                  >
                    <span>{ingredient}</span>
                    <button
                      onClick={() => removeIngredient(index)}
                      className="text-neutral-500 hover:text-neutral-700 text-lg leading-none"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Available Ingredients in Database */}
          <div className="card p-6 space-y-4">
            <h3 className="font-medium text-neutral-900">Available Ingredients in Database</h3>
            <p className="text-sm text-neutral-600">
              {ingredients.length} ingredients available
            </p>
          </div>

          {/* Dietary Preferences */}
          <div className="card p-6 space-y-4">
            <h3 className="font-medium text-neutral-900">Dietary Preferences</h3>
            <div className="grid grid-cols-2 gap-2">
              {['vegetarian', 'vegan', 'gluten-free', 'dairy-free', 'nut-free'].map(preference => (
                <label key={preference} className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={dietaryPreferences.includes(preference)}
                    onChange={() => toggleDietaryPreference(preference)}
                    className="rounded border-neutral-300 text-neutral-900 focus:ring-neutral-500"
                  />
                  <span className="text-sm text-neutral-700 capitalize">
                    {preference.replace('-', ' ')}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Recipe Filters */}
          <div className="card p-6 space-y-4">
            <h3 className="font-medium text-neutral-900">Recipe Filters</h3>
            
            <div>
              <label className="label">Difficulty</label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                className="input w-full"
              >
                <option value="ANY">Any difficulty</option>
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>

            <div>
              <label className="label">Max Cooking Time: {maxCookingTime} minutes</label>
              <input
                type="range"
                min="15"
                max="180"
                step="15"
                value={maxCookingTime}
                onChange={(e) => setMaxCookingTime(parseInt(e.target.value))}
                className="w-full h-2 bg-neutral-200 rounded-lg appearance-none cursor-pointer slider"
              />
              <div className="flex justify-between text-xs text-neutral-500 mt-1">
                <span>15m</span>
                <span>60m</span>
                <span>120m</span>
                <span>180m</span>
              </div>
            </div>

            <div>
              <label className="label">Cuisine</label>
              <select
                value={cuisine}
                onChange={(e) => setCuisine(e.target.value)}
                className="input w-full"
              >
                {CUISINES.map(cuisineOption => (
                  <option key={cuisineOption} value={cuisineOption}>
                    {cuisineOption}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="label">Number of People</label>
              <input
                type="number"
                min="1"
                max="12"
                value={numberOfPeople}
                onChange={(e) => setNumberOfPeople(parseInt(e.target.value))}
                className="input w-full"
              />
            </div>
          </div>

          {/* Generate Button */}
          <button
            onClick={handleGenerate}
            disabled={isLoading || ingredients.length === 0}
            className="w-full btn btn-primary py-3 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <>
                <LoadingSpinner size="sm" />
                Generating Recipes...
              </>
            ) : (
              'Generate Recipes'
            )}
          </button>


        </div>

        {/* Right Column - Results */}
        <div className="space-y-4">
          {error && (
            <div className="card p-4 border-red-200 bg-red-50">
              <div className="flex items-center gap-2 text-red-800">
                <span>❌</span>
                <span>{error}</span>
              </div>
            </div>
          )}

          {success && (
            <div className="card p-4 border-green-200 bg-green-50">
              <div className="flex items-center gap-2 text-green-800">
                <span>✅</span>
                <span>{success}</span>
              </div>
            </div>
          )}

          {isLoading && (
            <div className="card p-8 text-center">
              <LoadingSpinner />
              <p className="mt-4 text-neutral-600">Generating recipes...</p>
            </div>
          )}

          {recipes.length > 0 && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-neutral-900">
                  Generated Recipes ({recipes.length})
                </h3>
              </div>
              
              <div className="space-y-4">
                {recipes.map((recipe, index) => (
                  <RecipeCard 
                    key={recipe.id || index} 
                    recipe={recipe} 
                    showFullDetails={true}
                    isSaved={recipe.id ? savedRecipes.has(recipe.id) : false}
                    userRating={recipe.id ? userRatings.get(recipe.id) || null : null}
                    onRate={(stars) => handleRateRecipe(recipe, stars)}
                    onSaveToggle={() => {
                      if (recipe.id && savedRecipes.has(recipe.id)) {
                        handleUnsaveRecipe(recipe.id);
                      } else {
                        handleSaveRecipe(recipe);
                      }
                    }}
                  />
                ))}
              </div>
            </div>
          )}

          {!isLoading && recipes.length === 0 && ingredients.length > 0 && (
            <div className="card p-8 text-center">
              <div className="text-4xl text-neutral-400 mb-4">🍳</div>
              <h3 className="font-medium text-neutral-900 mb-2">Ready to Generate</h3>
              <p className="text-sm text-neutral-600">
                Click "Generate Recipes" to create recipes from your ingredients.
              </p>
            </div>
          )}

          {!isLoading && ingredients.length === 0 && (
            <div className="card p-8 text-center">
              <div className="text-4xl text-neutral-400 mb-4">🥕</div>
              <h3 className="font-medium text-neutral-900 mb-2">Add Some Ingredients</h3>
              <p className="text-sm text-neutral-600">
                Start by adding the ingredients you have available.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}



