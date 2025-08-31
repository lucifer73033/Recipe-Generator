import { useState } from 'react';
import { Recipe } from '../types';
import { useAuth } from '../contexts/AuthContext';
import { protectedApi } from '../lib/api';

interface RecipeCardProps {
  recipe: Recipe;
  showFullDetails?: boolean;
  isSaved?: boolean;
  userRating?: number | null;
  onSaveToggle?: () => void;
  onRate?: (stars: number) => void;
  showActions?: boolean;
}

export default function RecipeCard({ 
  recipe, 
  showFullDetails = false, 
  isSaved: initialIsSaved = false, 
  userRating: initialUserRating = null,
  onSaveToggle,
  onRate,
  showActions = true
}: RecipeCardProps) {
  const { isAuthenticated, token } = useAuth();
  const [isExpanded, setIsExpanded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  // Use props if provided, otherwise use local state
  const isSaved = onSaveToggle ? initialIsSaved : false;
  const userRating = onRate ? initialUserRating : null;

  const handleRate = async (stars: number) => {
    if (!isAuthenticated || !token) return;
    
    if (onRate) {
      onRate(stars);
      return;
    }
    
    setIsLoading(true);
    try {
      await protectedApi.recipes.rate(recipe.id!, stars, token);
      // Local state handling for standalone usage
    } catch (error) {
      console.error('Failed to rate recipe:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveToggle = async () => {
    if (!isAuthenticated || !token) return;
    
    if (onSaveToggle) {
      onSaveToggle();
      return;
    }
    
    setIsLoading(true);
    try {
      if (isSaved) {
        await protectedApi.recipes.unsave(recipe.id!, token);
        // Local state handling for standalone usage
      } else {
        await protectedApi.recipes.save(recipe.id!, token);
        // Local state handling for standalone usage
      }
    } catch (error) {
      console.error('Failed to toggle save:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY': return 'text-green-600 bg-green-100';
      case 'MEDIUM': return 'text-yellow-600 bg-yellow-100';
      case 'HARD': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getSourceBadgeColor = (source: string) => {
    switch (source) {
      case 'DB': return 'text-blue-600 bg-blue-100';
      case 'LLM': return 'text-purple-600 bg-purple-100';
      case 'FALLBACK': return 'text-orange-600 bg-orange-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden">
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <h3 
            className="text-xl font-semibold text-gray-900 mb-2 cursor-pointer hover:text-blue-600 transition-colors group"
            onClick={() => setIsExpanded(!isExpanded)}
            title="Click to expand recipe details"
          >
            <span className="flex items-center">
              {recipe.title}
              <span className="ml-2 text-sm text-gray-400 group-hover:text-blue-400 transition-colors">
                {isExpanded ? '▼' : '▶'}
              </span>
            </span>
          </h3>
          <div className="flex space-x-2">
            <span className={`px-2 py-1 text-xs font-medium rounded-full ${getSourceBadgeColor(recipe.source)}`}>
              {recipe.source}
            </span>
            <span className={`px-2 py-1 text-xs font-medium rounded-full ${getDifficultyColor(recipe.difficulty)}`}>
              {recipe.difficulty}
            </span>
          </div>
        </div>

        {(showFullDetails || isExpanded) && (
          <>
            <div className="mb-4">
              <h4 className="font-medium text-gray-900 mb-2">Ingredients:</h4>
              <ul className="list-disc list-inside space-y-1 text-sm text-gray-600">
                {recipe.ingredients.map((ingredient, index) => (
                  <li key={index}>
                    {ingredient.quantity} {ingredient.unit} {ingredient.name}
                  </li>
                ))}
              </ul>
            </div>

            <div className="mb-4">
              <h4 className="font-medium text-gray-900 mb-2">Instructions:</h4>
              <ol className="list-decimal list-inside space-y-2 text-sm text-gray-600">
                {recipe.steps.map((step, index) => (
                  <li key={index}>{step}</li>
                ))}
              </ol>
            </div>
          </>
        )}

        <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
          <span>⏱️ {recipe.timeMinutes} minutes</span>
          {recipe.cuisine && <span>🍽️ {recipe.cuisine}</span>}
          {recipe.dietTags && recipe.dietTags.length > 0 && (
            <span>🥗 {recipe.dietTags.join(', ')}</span>
          )}
        </div>

        {/* Authentication-required actions */}
        {isAuthenticated && showActions && (
          <div className="flex items-center space-x-4 pt-4 border-t border-gray-200">
            {/* Rating - only show if not already rated */}
            {!userRating && (
              <div className="flex items-center space-x-2">
                <span className="text-sm font-medium text-gray-700">Rate:</span>
                <div className="flex space-x-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      onClick={() => handleRate(star)}
                      disabled={isLoading}
                      className="text-lg text-gray-300 hover:text-yellow-400 cursor-pointer"
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Show current rating if already rated */}
            {userRating && (
              <div className="flex items-center space-x-2">
                <span className="text-sm font-medium text-gray-700">Your Rating:</span>
                <div className="flex space-x-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <span
                      key={star}
                      className={`text-lg ${
                        star <= userRating ? 'text-yellow-400' : 'text-gray-300'
                      }`}
                    >
                      ★
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Save button - only show if not already saved */}
            {!isSaved && (
              <button
                onClick={handleSaveToggle}
                disabled={isLoading}
                className="px-4 py-2 text-sm font-medium rounded-md transition-colors bg-blue-100 text-blue-700 hover:bg-blue-200"
              >
                Save
              </button>
            )}

            {/* Show saved status if already saved */}
            {isSaved && (
              <div className="flex items-center space-x-2">
                <span className="text-sm font-medium text-green-700">✓ Saved</span>
              </div>
            )}
          </div>
        )}

        {!isAuthenticated && (
          <div className="pt-4 border-t border-gray-200">
            <p className="text-sm text-gray-500 text-center">
              Sign in to rate and save recipes
            </p>
          </div>
        )}
      </div>
    </div>
  );
}



