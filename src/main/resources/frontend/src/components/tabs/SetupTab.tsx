import { useState, useEffect } from 'react';

export default function SetupTab() {
  const [defaultServings, setDefaultServings] = useState(4);
  const [defaultDifficulty, setDefaultDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');
  const [defaultMaxTime, setDefaultMaxTime] = useState(60);
  const [defaultCuisine, setDefaultCuisine] = useState('');
  const [defaultDietTags, setDefaultDietTags] = useState<string[]>([]);
  const [message, setMessage] = useState('');

  // Load settings from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('recipe-settings');
    if (saved) {
      try {
        const settings = JSON.parse(saved);
        setDefaultServings(settings.defaultServings || 4);
        setDefaultDifficulty(settings.defaultDifficulty || 'MEDIUM');
        setDefaultMaxTime(settings.defaultMaxTime || 60);
        setDefaultCuisine(settings.defaultCuisine || '');
        setDefaultDietTags(settings.defaultDietTags || []);
      } catch (error) {
        console.error('Failed to load settings:', error);
      }
    }
  }, []);

  // Save settings to localStorage whenever they change
  useEffect(() => {
    const settings = {
      defaultServings,
      defaultDifficulty,
      defaultMaxTime,
      defaultCuisine,
      defaultDietTags
    };
    localStorage.setItem('recipe-settings', JSON.stringify(settings));
  }, [defaultServings, defaultDifficulty, defaultMaxTime, defaultCuisine, defaultDietTags]);

  const savePreferences = () => {
    setMessage('Preferences saved successfully!');
    setTimeout(() => setMessage(''), 3000);
  };

  const toggleDietTag = (tag: string) => {
    setDefaultDietTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-lg font-semibold text-neutral-900 mb-2">
          User Preferences
        </h2>
        <p className="text-sm text-neutral-600">
          Set your default preferences for recipe generation. These will be used when you don't specify other options.
        </p>
      </div>

      <div className="card p-6 space-y-6">
        {/* Default Servings */}
        <div>
          <label className="label">Default Number of People</label>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setDefaultServings(Math.max(1, defaultServings - 1))}
              className="w-8 h-8 rounded-full bg-neutral-200 text-neutral-700 hover:bg-neutral-300 transition-colors"
            >
              -
            </button>
            <span className="w-12 text-center font-medium">{defaultServings}</span>
            <button
              onClick={() => setDefaultServings(defaultServings + 1)}
              className="w-8 h-8 rounded-full bg-neutral-200 text-neutral-700 hover:bg-neutral-300 transition-colors"
            >
              +
            </button>
          </div>
          <div className="mt-2">
            <span className="text-sm text-neutral-600">
              {defaultServings} {defaultServings === 1 ? 'person' : 'people'}
            </span>
          </div>
        </div>

        {/* Default Difficulty */}
        <div>
          <label className="label">Default Difficulty</label>
          <select
            value={defaultDifficulty}
            onChange={(e) => setDefaultDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD')}
            className="input w-full"
          >
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
        </div>

        {/* Default Max Time */}
        <div>
          <label className="label">Default Max Cooking Time: {defaultMaxTime} minutes</label>
          <input
            type="range"
            min="15"
            max="180"
            step="15"
            value={defaultMaxTime}
            onChange={(e) => setDefaultMaxTime(parseInt(e.target.value))}
            className="w-full h-2 bg-neutral-200 rounded-lg appearance-none cursor-pointer slider"
          />
          <div className="flex justify-between text-xs text-neutral-500 mt-1">
            <span>15m</span>
            <span>60m</span>
            <span>120m</span>
            <span>180m</span>
          </div>
        </div>

        {/* Default Cuisine */}
        <div>
          <label className="label">Default Cuisine (optional)</label>
          <input
            type="text"
            value={defaultCuisine}
            onChange={(e) => setDefaultCuisine(e.target.value)}
            className="input w-full"
            placeholder="e.g., Italian, Asian, Mexican"
          />
        </div>

        {/* Default Diet Tags */}
        <div>
          <label className="label">Default Dietary Preferences</label>
          <div className="flex flex-wrap gap-2">
            {['vegetarian', 'vegan', 'gluten-free', 'dairy-free', 'nut-free'].map(tag => (
              <button
                key={tag}
                onClick={() => toggleDietTag(tag)}
                className={`px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                  defaultDietTags.includes(tag)
                    ? 'bg-blue-100 text-blue-700'
                    : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
                }`}
              >
                {tag.replace('-', ' ')}
              </button>
            ))}
          </div>
        </div>

        {/* Save Button */}
        <button
          onClick={savePreferences}
          className="w-full btn btn-primary py-3"
        >
          Save Preferences
        </button>

        {message && (
          <div className="p-3 rounded-md text-sm bg-green-50 text-green-700 border border-green-200">
            {message}
          </div>
        )}
      </div>
    </div>
  );
}



