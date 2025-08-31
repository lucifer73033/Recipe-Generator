import { useState, useRef, useEffect } from 'react'
import { useIngredientAutocomplete } from '../hooks/useIngredientAutocomplete'

interface AutocompleteInputProps {
  value: string
  onChange: (value: string) => void
  onAdd: () => void
  placeholder?: string
  className?: string
  disabled?: boolean
}

export default function AutocompleteInput({
  value,
  onChange,
  onAdd,
  placeholder = "Enter an ingredient",
  className = "",
  disabled = false
}: AutocompleteInputProps) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(-1)
  const inputRef = useRef<HTMLInputElement>(null)
  const suggestionRefs = useRef<(HTMLButtonElement | null)[]>([])
  
  const { suggestions, isLoading } = useIngredientAutocomplete(value)

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value
    onChange(newValue)
    setShowSuggestions(newValue.length > 0)
    setSelectedIndex(-1)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!showSuggestions) {
      if (e.key === 'Enter') {
        e.preventDefault()
        onAdd()
      }
      return
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex(prev => 
          prev < suggestions.length - 1 ? prev + 1 : prev
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex(prev => prev > 0 ? prev - 1 : -1)
        break
      case 'Enter':
        e.preventDefault()
        if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
          selectSuggestion(suggestions[selectedIndex])
        } else {
          onAdd()
        }
        break
      case 'Escape':
        setShowSuggestions(false)
        setSelectedIndex(-1)
        break
      case 'Tab':
        if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
          e.preventDefault()
          selectSuggestion(suggestions[selectedIndex])
        }
        break
    }
  }

  const selectSuggestion = (suggestion: string) => {
    onChange(suggestion)
    setShowSuggestions(false)
    setSelectedIndex(-1)
    // Focus back to input for better UX
    inputRef.current?.focus()
  }

  const handleInputFocus = () => {
    if (value.length > 0 && suggestions.length > 0) {
      setShowSuggestions(true)
    }
  }

  const handleInputBlur = () => {
    // Delay hiding suggestions to allow clicking on them
    setTimeout(() => {
      setShowSuggestions(false)
      setSelectedIndex(-1)
    }, 200)
  }

  // Update refs array when suggestions change
  useEffect(() => {
    suggestionRefs.current = suggestionRefs.current.slice(0, suggestions.length)
  }, [suggestions])

  // Scroll selected item into view
  useEffect(() => {
    if (selectedIndex >= 0 && suggestionRefs.current[selectedIndex]) {
      suggestionRefs.current[selectedIndex]?.scrollIntoView({
        block: 'nearest',
        behavior: 'smooth'
      })
    }
  }, [selectedIndex])

  const shouldShowSuggestions = showSuggestions && suggestions.length > 0 && value.length > 0

  return (
    <div className="relative">
      <input
        ref={inputRef}
        type="text"
        value={value}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        onFocus={handleInputFocus}
        onBlur={handleInputBlur}
        placeholder={placeholder}
        className={`input w-full ${className}`}
        disabled={disabled}
        autoComplete="off"
        role="combobox"
        aria-expanded={shouldShowSuggestions}
        aria-haspopup="listbox"
        aria-autocomplete="list"
      />
      
      {shouldShowSuggestions && (
        <div 
          className="absolute z-50 w-full mt-1 bg-white border border-neutral-200 rounded-md shadow-lg max-h-60 overflow-auto"
          role="listbox"
        >
          {isLoading && (
            <div className="px-3 py-2 text-sm text-neutral-500 flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-neutral-300 border-t-neutral-600 rounded-full animate-spin"></div>
              Searching ingredients...
            </div>
          )}
          
          {suggestions.map((suggestion, index) => (
            <button
              key={suggestion}
              ref={(el) => (suggestionRefs.current[index] = el)}
              className={`w-full px-3 py-2 text-left text-sm hover:bg-neutral-50 focus:bg-neutral-50 focus:outline-none ${
                index === selectedIndex ? 'bg-neutral-100' : ''
              }`}
              onClick={() => selectSuggestion(suggestion)}
              role="option"
              aria-selected={index === selectedIndex}
            >
              <span className="font-medium">{suggestion}</span>
            </button>
          ))}
          
          {!isLoading && suggestions.length === 0 && value.length > 0 && (
            <div className="px-3 py-2 text-sm text-neutral-500">
              No matching ingredients found. Press Enter to add "{value}" anyway.
            </div>
          )}
        </div>
      )}
    </div>
  )
}
