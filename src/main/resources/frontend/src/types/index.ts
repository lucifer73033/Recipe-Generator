export interface Recipe {
  id?: string
  title: string
  ingredients: Ingredient[]
  steps: string[]
  timeMinutes: number
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  cuisine?: string
  dietTags: string[]
  nutrition?: Nutrition
  source: 'DB' | 'LLM' | 'FALLBACK'
  createdBy?: string
  createdAt?: string
  imageUrl?: string
}

export interface Ingredient {
  name: string
  quantity?: string
  unit?: string
}

export interface Nutrition {
  kcal: number
  protein: number
  carbs: number
  fat: number
}

export interface RecipeRequest {
  ingredients: string[]
  dietTags?: string[]
  maxTimeMinutes?: number
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
  cuisine?: string
  servings?: number
}

export interface RecipeResponse {
  recipes: Recipe[]
  metadata: RecipeMetadata
}

export interface RecipeMetadata {
  totalRecipes: number
  highMatchCount: number
  userHasAllCount: number
  llmGeneratedCount: number
  strategy: string
  hasUserHasAllRecipes: boolean
  message?: string
  userHasAllRecipeIds: string[]
}

export interface IngredientRecognition {
  ingredients: RecognizedIngredient[]
}

export interface RecognizedIngredient {
  name: string
  confidence: number
}

export interface User {
  id: string
  username: string
  name: string
  email: string
  roles: string[]
  createdAt: string
  lastLoginAt?: string
}

export interface Rating {
  recipeId: string
  stars: number
  createdAt: string
  updatedAt?: string
}

export interface Log {
  id: string
  timestamp: string
  event: string
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'
  userId?: string
  metadata?: Record<string, any>
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  error?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  name: string
  password: string
}

export interface AuthResponse {
  success: boolean
  message: string
  token: string
  user: User
}



