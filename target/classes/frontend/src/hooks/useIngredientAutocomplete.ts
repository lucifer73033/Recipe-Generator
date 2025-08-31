import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { publicApi } from '../lib/api'

export function useIngredientAutocomplete(searchTerm: string) {
  // Fetch complete master ingredients list once
  const { data: masterIngredients = [], isLoading } = useQuery({
    queryKey: ['master-ingredients'],
    queryFn: () => publicApi.ingredients.getMasterList(),
    staleTime: 10 * 60 * 1000, // 10 minutes - master list doesn't change often
    gcTime: 30 * 60 * 1000, // 30 minutes (formerly cacheTime)
  })

  // Filter ingredients locally in real-time
  const filteredSuggestions = useMemo(() => {
    if (!searchTerm.trim() || masterIngredients.length === 0) {
      return []
    }
    
    const lowerSearchTerm = searchTerm.toLowerCase().trim()
    
    return masterIngredients
      .filter(ingredient => ingredient.toLowerCase().includes(lowerSearchTerm))
      .sort((a, b) => {
        // Prioritize exact starts with matches
        const aStarts = a.toLowerCase().startsWith(lowerSearchTerm)
        const bStarts = b.toLowerCase().startsWith(lowerSearchTerm)
        if (aStarts && !bStarts) return -1
        if (!aStarts && bStarts) return 1
        return a.toLowerCase().localeCompare(b.toLowerCase())
      })
      .slice(0, 10) // Limit to 10 suggestions for performance
  }, [masterIngredients, searchTerm])

  return {
    suggestions: filteredSuggestions,
    isLoading: isLoading && masterIngredients.length === 0,
    masterIngredients, // Expose full list if needed
  }
}
