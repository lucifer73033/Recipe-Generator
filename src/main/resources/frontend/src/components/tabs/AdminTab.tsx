import { useState } from 'react';
import { Recipe } from '../../types';
import { protectedApi } from '../../lib/api';
import { useAuth } from '../../contexts/AuthContext';

export default function AdminTab() {
  const { token } = useAuth();
  const [recipe, setRecipe] = useState<Partial<Recipe>>({
    title: '',
    ingredients: [],
    steps: [],
    timeMinutes: 30,
    difficulty: 'MEDIUM',
    cuisine: '',
    dietTags: [],
    source: 'DB'
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  const addIngredient = () => {
    setRecipe(prev => ({
      ...prev,
      ingredients: [...(prev.ingredients || []), { name: '', quantity: '', unit: '' }]
    }));
  };

  const removeIngredient = (index: number) => {
    setRecipe(prev => ({
      ...prev,
      ingredients: prev.ingredients?.filter((_, i) => i !== index) || []
    }));
  };

  const updateIngredient = (index: number, field: 'name' | 'quantity' | 'unit', value: string) => {
    setRecipe(prev => ({
      ...prev,
      ingredients: prev.ingredients?.map((ing, i) => 
        i === index ? { ...ing, [field]: value } : ing
      ) || []
    }));
  };

  const addStep = () => {
    setRecipe(prev => ({
      ...prev,
      steps: [...(prev.steps || []), '']
    }));
  };

  const removeStep = (index: number) => {
    setRecipe(prev => ({
      ...prev,
      steps: prev.steps?.filter((_, i) => i !== index) || []
    }));
  };

  const updateStep = (index: number, value: string) => {
    setRecipe(prev => ({
      ...prev,
      steps: prev.steps?.map((step, i) => i === index ? value : step) || []
    }));
  };

  const toggleDietTag = (tag: string) => {
    setRecipe(prev => ({
      ...prev,
      dietTags: prev.dietTags?.includes(tag) 
        ? prev.dietTags.filter(t => t !== tag)
        : [...(prev.dietTags || []), tag]
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setMessage('You must be logged in to add recipes');
      return;
    }

    setIsSubmitting(true);
    setMessage('');

    try {
      await protectedApi.admin.addRecipe(recipe, token);
      setMessage('Recipe added successfully!');
      setRecipe({
        title: '',
        ingredients: [],
        steps: [],
        timeMinutes: 30,
        difficulty: 'MEDIUM',
        cuisine: '',
        dietTags: [],
        source: 'DB'
      });
    } catch (error) {
      setMessage('Failed to add recipe. Please try again.');
      console.error('Error adding recipe:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Add New Recipe</h2>
        
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Info */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="label">Recipe Title</label>
              <input
                type="text"
                value={recipe.title}
                onChange={(e) => setRecipe(prev => ({ ...prev, title: e.target.value }))}
                className="input w-full"
                required
              />
            </div>
            
            <div>
              <label className="label">Cooking Time (minutes)</label>
              <input
                type="number"
                value={recipe.timeMinutes}
                onChange={(e) => setRecipe(prev => ({ ...prev, timeMinutes: parseInt(e.target.value) || 0 }))}
                className="input w-full"
                min="1"
                required
              />
            </div>
            
            <div>
              <label className="label">Difficulty</label>
              <select
                value={recipe.difficulty}
                onChange={(e) => setRecipe(prev => ({ ...prev, difficulty: e.target.value as 'EASY' | 'MEDIUM' | 'HARD' }))}
                className="input w-full"
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
            
            <div>
              <label className="label">Cuisine</label>
              <input
                type="text"
                value={recipe.cuisine}
                onChange={(e) => setRecipe(prev => ({ ...prev, cuisine: e.target.value }))}
                className="input w-full"
                placeholder="e.g., Italian, Asian"
              />
            </div>
          </div>

          {/* Diet Tags */}
          <div>
            <label className="label">Dietary Tags</label>
            <div className="flex flex-wrap gap-2">
              {['vegetarian', 'vegan', 'gluten-free', 'dairy-free', 'low-carb', 'keto'].map(tag => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => toggleDietTag(tag)}
                  className={`px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                    recipe.dietTags?.includes(tag)
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {tag.replace('-', ' ')}
                </button>
              ))}
            </div>
          </div>

          {/* Ingredients */}
          <div>
            <label className="label">Ingredients</label>
            <div className="space-y-2">
              {recipe.ingredients?.map((ingredient, index) => (
                <div key={index} className="flex gap-2">
                  <input
                    type="text"
                    value={ingredient.quantity}
                    onChange={(e) => updateIngredient(index, 'quantity', e.target.value)}
                    className="input flex-1"
                    placeholder="Quantity"
                  />
                  <input
                    type="text"
                    value={ingredient.unit}
                    onChange={(e) => updateIngredient(index, 'unit', e.target.value)}
                    className="input flex-1"
                    placeholder="Unit"
                  />
                  <input
                    type="text"
                    value={ingredient.name}
                    onChange={(e) => updateIngredient(index, 'name', e.target.value)}
                    className="input flex-1"
                    placeholder="Ingredient name"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => removeIngredient(index)}
                    className="px-3 py-2 text-red-600 hover:text-red-800"
                  >
                    ×
                  </button>
                </div>
              ))}
              <button
                type="button"
                onClick={addIngredient}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                + Add Ingredient
              </button>
            </div>
          </div>

          {/* Steps */}
          <div>
            <label className="label">Instructions</label>
            <div className="space-y-2">
              {recipe.steps?.map((step, index) => (
                <div key={index} className="flex gap-2">
                  <span className="text-sm text-gray-500 w-6">{index + 1}.</span>
                  <input
                    type="text"
                    value={step}
                    onChange={(e) => updateStep(index, e.target.value)}
                    className="input flex-1"
                    placeholder={`Step ${index + 1}`}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => removeStep(index)}
                    className="px-3 py-2 text-red-600 hover:text-red-800"
                  >
                    ×
                  </button>
                </div>
              ))}
              <button
                type="button"
                onClick={addStep}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                + Add Step
              </button>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Adding Recipe...' : 'Add Recipe'}
          </button>

          {message && (
            <div className={`p-3 rounded-md text-sm ${
              message.includes('successfully') 
                ? 'bg-green-50 text-green-700 border border-green-200'
                : 'bg-red-50 text-red-700 border border-red-200'
            }`}>
              {message}
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
