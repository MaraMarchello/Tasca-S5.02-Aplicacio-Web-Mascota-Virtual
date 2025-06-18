import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { clsx } from 'clsx';
import { useTheme } from '../../contexts/ThemeContext';
import { useToast } from '../../contexts/ToastContext';
import Button from '../ui/Button';
import { authApi } from '../../utils/api';

interface NavBarProps {
  user?: {
    id: number;
    name: string;
    email: string;
    authorities: string[];
  } | null;
  onSidebarToggle?: () => void;
}

const NavBar: React.FC<NavBarProps> = ({ user, onSidebarToggle }) => {
  const { theme, toggleTheme } = useTheme();
  const { showSuccess } = useToast();
  const navigate = useNavigate();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  const handleLogout = async () => {
    try {
      await authApi.logout();
      showSuccess('Logged out successfully');
      navigate('/login');
    } catch (error) {
      console.error('Error during logout:', error);
      navigate('/login');
    }
  };

  const isAdmin = user?.authorities?.includes('ADMIN');

  return (
    <nav className="fixed top-0 left-0 right-0 z-40 bg-surface-light/80 dark:bg-surface-dark/80 backdrop-blur-md border-b border-border-light dark:border-border-dark">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Left side - Hamburger menu and Logo */}
          <div className="flex items-center space-x-4">
            {/* Hamburger Menu for Sidebar */}
            {onSidebarToggle && (
              <button
                onClick={onSidebarToggle}
                className={clsx(
                  'p-2 rounded-xl transition-colors lg:hidden',
                  'text-gray-600 dark:text-gray-300',
                  'hover:text-primary-600 dark:hover:text-primary-400',
                  'hover:bg-primary-50 dark:hover:bg-primary-950',
                  'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500'
                )}
                title="Toggle sidebar"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
            )}
            
            {/* Logo - hidden on mobile when sidebar is available */}
            <Link 
              to="/dashboard" 
              className={clsx(
                'flex items-center space-x-3 hover:opacity-80 transition-opacity',
                onSidebarToggle && 'hidden lg:flex'
              )}
            >
              <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-xl flex items-center justify-center">
                <span className="text-white font-bold text-lg">C</span>
              </div>
              <span className="text-xl font-bold text-text-light dark:text-text-dark">
                CodeMate
              </span>
            </Link>
          </div>



          {/* Right Side Actions */}
          <div className="flex items-center space-x-3">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className={clsx(
                'p-2 rounded-xl transition-colors',
                'text-gray-600 dark:text-gray-300',
                'hover:text-primary-600 dark:hover:text-primary-400',
                'hover:bg-primary-50 dark:hover:bg-primary-950',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500'
              )}
              title={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
            >
              {theme === 'light' ? (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              )}
            </button>

            {/* User Menu */}
            {user ? (
              <div className="relative">
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className={clsx(
                    'flex items-center space-x-2 p-2 rounded-xl transition-colors',
                    'text-gray-600 dark:text-gray-300',
                    'hover:bg-primary-50 dark:hover:bg-primary-950',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500'
                  )}
                >
                  <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-lg flex items-center justify-center">
                    <span className="text-white font-medium text-sm">
                      {user.name.charAt(0).toUpperCase()}
                    </span>
                  </div>
                  <span className="hidden sm:block text-sm font-medium">
                    {user.name}
                  </span>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {/* Dropdown Menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-surface-light dark:bg-surface-dark rounded-xl shadow-strong border border-border-light dark:border-border-dark py-1 animate-slide-in">
                    <div className="px-4 py-2 border-b border-border-light dark:border-border-dark">
                      <p className="text-sm font-medium text-text-light dark:text-text-dark">
                        {user.name}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {user.email}
                      </p>
                    </div>
                    <Link
                      to="/dashboard"
                      className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-primary-50 dark:hover:bg-primary-950 transition-colors"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      Dashboard
                    </Link>
                    <Link
                      to="/pet"
                      className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-primary-50 dark:hover:bg-primary-950 transition-colors"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      My Pet
                    </Link>
                    {isAdmin && (
                      <Link
                        to="/admin"
                        className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-primary-50 dark:hover:bg-primary-950 transition-colors"
                        onClick={() => setIsUserMenuOpen(false)}
                      >
                        Admin Panel
                      </Link>
                    )}
                    <div className="border-t border-border-light dark:border-border-dark mt-1 pt-1">
                      <button
                        onClick={() => {
                          setIsUserMenuOpen(false);
                          handleLogout();
                        }}
                        className="block w-full text-left px-4 py-2 text-sm text-error-600 dark:text-error-400 hover:bg-error-50 dark:hover:bg-error-950 transition-colors"
                      >
                        Sign Out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/login')}
                >
                  Sign In
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => navigate('/signup')}
                >
                  Sign Up
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>


    </nav>
  );
};

export default NavBar; 