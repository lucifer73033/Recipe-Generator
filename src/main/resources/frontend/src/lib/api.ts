import { IngredientRecognition, RecipeResponse, Recipe, User, LoginRequest, RegisterRequest, AuthResponse } from '../types';

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  endpoint: string,
  options: RequestInit = {},
  requireAuth: boolean = false,
  token?: string
): Promise<T> {
  // Don't set default Content-Type for FormData requests
  const headers: Record<string, string> = {};
  
  // Only set Content-Type if not FormData and not already specified
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  
  // Add any custom headers
  if (options.headers) {
    Object.assign(headers, options.headers);
  }

  if (requireAuth && token) {
    // Use Basic Authentication with the base64 encoded token
    headers.Authorization = `Basic ${token}`;
  }

  const response = await fetch(`/api${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new ApiError(
      errorData.message || `HTTP error! status: ${response.status}`,
      response.status,
      errorData
    );
  }

  return response.json();
}

// Public API calls (no auth required)
export const publicApi = {
  recipes: {
    generate: (data: any): Promise<RecipeResponse> => 
      request<RecipeResponse>('/recipes/generate', { method: 'POST', body: JSON.stringify(data) }),
    search: (params: any): Promise<any> => 
      request(`/recipes?${new URLSearchParams(params)}`),
    getById: (id: string): Promise<Recipe> => 
      request<Recipe>(`/recipes/${id}`),
    getRating: (id: string): Promise<any> => 
      request(`/recipes/${id}/rating`),
    getFavorite: (id: string): Promise<any> => 
      request(`/recipes/${id}/favorite`),
  },
  ingredients: {
    recognize: (formData: FormData): Promise<IngredientRecognition> => {
      return request<IngredientRecognition>('/ingredients/recognize', { method: 'POST', body: formData });
    },
    getMasterList: (): Promise<string[]> => 
      request<string[]>('/ingredients/master-list'),
    getSupportedFormats: (): Promise<any> => 
      request('/ingredients/supported-formats'),
  },
  auth: {
    login: (data: LoginRequest): Promise<AuthResponse> => 
      request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
    register: (data: RegisterRequest): Promise<AuthResponse> => 
      request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  },
};

// Protected API calls (auth required)
export const protectedApi = {
  recipes: {
    rate: (id: string, stars: number, token: string) => 
      request(`/recipes/${id}/rate`, { method: 'POST', body: JSON.stringify({ stars }) }, true, token),
    save: (id: string, token: string) => 
      request(`/recipes/${id}/save`, { method: 'POST' }, true, token),
    saveLLM: (recipe: any, token: string): Promise<{success: boolean, message: string, recipeId: string}> => 
      request(`/recipes/save-llm`, { method: 'POST', body: JSON.stringify(recipe) }, true, token),
    unsave: (id: string, token: string) => 
      request(`/recipes/${id}/save`, { method: 'DELETE' }, true, token),
  },
  user: {
    getProfile: (token: string): Promise<User> => 
      request<User>('/me/profile', {}, true, token),
    getSavedRecipes: (token: string): Promise<Recipe[]> => 
      request<Recipe[]>('/me/saved', {}, true, token),
    getRatings: (token: string): Promise<any> => 
      request('/me/ratings', {}, true, token),
    getStats: (token: string): Promise<any> => 
      request('/me/stats', {}, true, token),
  },
  admin: {
    addRecipe: (recipe: any, token: string): Promise<Recipe> => 
      request<Recipe>('/admin/recipes', { method: 'POST', body: JSON.stringify(recipe) }, true, token),
    seedDatabase: (token: string): Promise<any> => 
      request('/admin/seed', { method: 'POST' }, true, token),
    getStats: (token: string): Promise<any> => 
      request('/admin/stats', {}, true, token),
  },
};

// Legacy export for backward compatibility
export const api = {
  ...publicApi,
  ...protectedApi,
};



