import { useState, useEffect } from 'react';
import { Recipe } from '../../types';
import { protectedApi } from '../../lib/api';
import { useAuth } from '../../contexts/AuthContext';
import RecipeCard from '../RecipeCard';
import LoadingSpinner from '../LoadingSpinner';

export default function SavedTab() {
  const { isAuthenticated, token } = useAuth();
  const [savedRecipes, setSavedRecipes] = useState<Recipe[]>([]);
  const [userStats, setUserStats] = useState<any>(null);
  const [userRatings, setUserRatings] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  useEffect(() => {
    if (isAuthenticated && token) {
      loadUserData();
    }
  }, [isAuthenticated, token]);

  const loadUserData = async () => {
    if (!token) return;
    
    setIsLoading(true);
    setError('');
    setSuccess('');
    
    try {
      const [recipes, stats, ratings] = await Promise.all([
        protectedApi.user.getSavedRecipes(token),
        protectedApi.user.getStats(token),
        protectedApi.user.getRatings(token)
      ]);
      
      setSavedRecipes(recipes || []);
      setUserStats(stats || {});
      setUserRatings(ratings || []);
    } catch (error) {
      setError('Failed to load user data');
      console.error('Error loading user data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteRecipe = async (recipeId: string) => {
    if (!token) return;
    
    try {
      await protectedApi.recipes.unsave(recipeId, token);
      setSavedRecipes(prev => prev.filter(recipe => recipe.id !== recipeId));
      setSuccess('Recipe removed from favorites!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (error) {
      console.error('Failed to remove recipe:', error);
      setError('Failed to remove recipe. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Saved Recipes</h2>
        <p className="text-gray-600 mb-6">Sign in to view your saved recipes and ratings.</p>
        <button
          onClick={() => window.location.reload()}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          Sign In
        </button>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <LoadingSpinner size="lg" />
        <p className="text-gray-600 mt-4">Loading your saved recipes...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <div className="text-red-600 mb-4">{error}</div>
        <button
          onClick={loadUserData}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          Try Again
        </button>
      </div>
    );
  }



  const filteredRecipes = savedRecipes?.filter(recipe =>
    recipe.title.toLowerCase().includes('') // Add search functionality here if needed
  ) || [];

  return (
    <div className="space-y-6">
      {/* Success Message */}
      {success && (
        <div className="bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-lg">
          <div className="flex items-center justify-between">
            <span>{success}</span>
            <button
              onClick={() => setSuccess('')}
              className="text-green-600 hover:text-green-800"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      {/* User Stats */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Your Recipe Stats</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center p-4 bg-blue-50 rounded-lg">
            <div className="text-3xl font-bold text-blue-600">
              {userStats?.savedRecipesCount || 0}
            </div>
            <div className="text-sm text-blue-700">Saved Recipes</div>
          </div>
          
          <div className="text-center p-4 bg-green-50 rounded-lg">
            <div className="text-3xl font-bold text-green-600">
              {userStats?.ratingsCount || 0}
            </div>
            <div className="text-sm text-green-700">Ratings Given</div>
          </div>
          
          <div className="text-center p-4 bg-purple-50 rounded-lg">
            <div className="text-sm text-purple-700">Member Since</div>
            <div className="text-lg font-semibold text-purple-600">
              {userStats?.memberSince ? 
                new Date(userStats.memberSince).toLocaleDateString() : 
                'N/A'
              }
            </div>
          </div>
        </div>

        {/* Recipe Summary */}
        {savedRecipes && savedRecipes.length > 0 && (
          <div className="mt-6 p-4 bg-gray-50 rounded-lg">
            <div className="text-sm text-gray-600 mb-2">
              Showing {filteredRecipes.length} of {savedRecipes.length} saved recipes
            </div>
            <div className="text-sm text-gray-600">
              {savedRecipes.length > 0 && (
                <>
                  {`${savedRecipes.length} saved ${savedRecipes.length === 1 ? 'recipe' : 'recipes'}`}
                  <br />
                  Total cooking time: {savedRecipes.reduce((total, recipe) => total + recipe.timeMinutes, 0)} minutes
                </>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Saved Recipes */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-xl font-semibold text-gray-900 mb-4">Saved Recipes</h3>
        
        {savedRecipes && savedRecipes.length > 0 ? (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {filteredRecipes.map((recipe, index) => {
              // Find if this recipe is already rated
              const rating = userRatings.find(r => r.recipeId === recipe.id);
              return (
                <div key={recipe.id || index} className="relative">
                  <RecipeCard 
                    recipe={recipe} 
                    showFullDetails={false}
                    isSaved={true}
                    userRating={rating?.stars || null}
                    showActions={false} // Hide actions since these are already saved
                  />
                  <button
                    onClick={() => handleDeleteRecipe(recipe.id || '')}
                    className="absolute top-2 right-2 p-2 bg-red-100 hover:bg-red-200 text-red-600 rounded-full transition-colors"
                    title="Remove from favorites"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-gray-500 mb-4">You haven't saved any recipes yet.</p>
            <p className="text-sm text-gray-400">Start exploring recipes and save your favorites!</p>
          </div>
        )}
      </div>

      {/* Recent Ratings */}
      {userRatings && userRatings.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Recent Ratings</h3>
          
          <div className="space-y-3">
            {userRatings.slice(0, 5).map((rating, index) => {
              // Find the recipe title for this rating
              const recipe = savedRecipes.find(r => r.id === rating.recipeId);
              return (
                <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <div className="flex space-x-1">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <span
                          key={star}
                          className={`text-lg ${
                            star <= rating.stars ? 'text-yellow-400' : 'text-gray-300'
                          }`}
                        >
                          ★
                        </span>
                      ))}
                    </div>
                    <span className="text-sm text-gray-600">
                      {recipe?.title || 'Unknown Recipe'}
                    </span>
                  </div>
                  <div className="text-xs text-gray-500">
                    {rating.createdAt ? new Date(rating.createdAt).toLocaleDateString() : 'N/A'}
                  </div>
                </div>
              );
            })}
            
            {userRatings.length > 5 && (
              <div className="text-center text-sm text-gray-500 pt-2">
                And {userRatings.length - 5} more ratings...
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}



