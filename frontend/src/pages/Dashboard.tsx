import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi, authApi, getAuthToken } from '../utils/api';

interface User {
  id: number;
  name: string;
  email: string;
  authorities: string[];
}

const Dashboard: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserData = async () => {
      const token = getAuthToken();
      if (!token) {
        navigate('/login');
        return;
      }

      try {
        const userData = await userApi.getCurrentUser();
        setUser(userData);
      } catch (error) {
        console.error('Error fetching user data:', error);
        navigate('/login');
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, [navigate]);

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Error during logout:', error);
    } finally {
      navigate('/login');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!user) {
    return null; // Will redirect to login
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navigation Bar */}
      <nav className="bg-gradient-to-r from-blue-500 to-purple-600 shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-white text-xl font-bold">CodeMate</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-white text-sm">
                Welcome, {user.name}!
              </span>
              {localStorage.getItem('userRole') === 'ADMIN' && (
                <button
                  onClick={() => navigate('/admin')}
                  className="bg-white bg-opacity-20 hover:bg-opacity-30 text-white px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
                >
                  Admin Panel
                </button>
              )}
              <button
                onClick={handleLogout}
                className="bg-white bg-opacity-20 hover:bg-opacity-30 text-white px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <div className="bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-8 mb-8 text-white text-center">
          <h1 className="text-4xl font-bold mb-4">Welcome to CodeMate!</h1>
          <p className="text-xl mb-2">Your AI-powered Java learning companion</p>
          <p className="text-blue-100">You have successfully logged in using JWT authentication.</p>
        </div>

        {/* Feature Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 text-center hover:shadow-xl transform hover:-translate-y-1 transition-all duration-200">
            <div className="text-purple-600 text-4xl mb-4">
              🐾
            </div>
            <h3 className="text-xl font-semibold mb-2">Virtual Pet</h3>
            <p className="text-gray-600 mb-4">Care for your coding companion, earn points, and unlock achievements!</p>
            <button 
              onClick={() => navigate('/pet')}
              className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
            >
              Meet Your Pet
            </button>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 text-center hover:shadow-xl transform hover:-translate-y-1 transition-all duration-200">
            <div className="text-blue-600 text-4xl mb-4">
              <i className="fas fa-code"></i>
            </div>
            <h3 className="text-xl font-semibold mb-2">AI Code Helper</h3>
            <p className="text-gray-600 mb-4">Get intelligent code suggestions and explanations for your Java projects.</p>
            <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors">
              Get Started
            </button>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 text-center hover:shadow-xl transform hover:-translate-y-1 transition-all duration-200">
            <div className="text-green-600 text-4xl mb-4">
              <i className="fab fa-git-alt"></i>
            </div>
            <h3 className="text-xl font-semibold mb-2">Git Coach</h3>
            <p className="text-gray-600 mb-4">Learn Git commands and best practices with personalized guidance.</p>
            <button className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg transition-colors">
              Learn Git
            </button>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 text-center hover:shadow-xl transform hover:-translate-y-1 transition-all duration-200">
            <div className="text-yellow-600 text-4xl mb-4">
              <i className="fas fa-bug"></i>
            </div>
            <h3 className="text-xl font-semibold mb-2">Stack Trace Explainer</h3>
            <p className="text-gray-600 mb-4">Understand and fix errors with detailed stack trace analysis.</p>
            <button className="bg-yellow-600 hover:bg-yellow-700 text-white px-4 py-2 rounded-lg transition-colors">
              Debug Now
            </button>
          </div>
        </div>

        {/* Security Information */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h3 className="text-xl font-semibold mb-4">Security Information</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <p><strong>Authentication Type:</strong> JWT-based</p>
              <p><strong>Username:</strong> {user.name}</p>
              <p><strong>Status:</strong> <span className="text-green-600">Authenticated</span></p>
            </div>
            <div className="space-y-2">
              <p><strong>User ID:</strong> {user.id}</p>
              <p><strong>Email:</strong> {user.email}</p>
              <p><strong>Roles:</strong> {user.authorities.join(', ')}</p>
              <p><strong>Login Time:</strong> {new Date().toLocaleString()}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 