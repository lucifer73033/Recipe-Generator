import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import LoginForm from './LoginForm';

export default function Layout() {
  const { user, isAuthenticated, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('ingredients');

  const handleTabChange = (value: string) => {
    setActiveTab(value);
  };

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-semibold text-gray-900">
                Recipe Generator
              </h1>
            </div>
            
            {/* User section */}
            <div className="flex items-center space-x-4">
              {isAuthenticated ? (
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white font-semibold">
                    {user?.name?.charAt(0)?.toUpperCase()}
                  </div>
                  <span className="text-sm text-gray-700">
                    Logged in as: {user?.name}
                  </span>
                  <button
                    onClick={handleLogout}
                    className="text-sm text-red-600 hover:text-red-800"
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <LoginForm />
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Navigation Tabs */}
      <nav className="bg-white border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-8">
            <button
              onClick={() => handleTabChange('ingredients')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'ingredients'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Ingredients
            </button>
            <button
              onClick={() => handleTabChange('saved')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'saved'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Saved Recipes
            </button>
            <button
              onClick={() => handleTabChange('setup')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'setup'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Setup
            </button>
            {isAuthenticated && user?.roles?.includes('ADMIN') && (
              <button
                onClick={() => handleTabChange('admin')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'admin'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Admin
              </button>
            )}
          </div>
        </div>
      </nav>

      {/* Tab Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {activeTab === 'ingredients' && <IngredientsTab />}
        {activeTab === 'saved' && <SavedTab />}
        {activeTab === 'setup' && <SetupTab />}
        {activeTab === 'admin' && isAuthenticated && user?.roles?.includes('ADMIN') && <AdminTab />}
      </main>
    </div>
  );
}

// Import tab components
import IngredientsTab from './tabs/IngredientsTab';
import SavedTab from './tabs/SavedTab';
import SetupTab from './tabs/SetupTab';
import AdminTab from './tabs/AdminTab';



