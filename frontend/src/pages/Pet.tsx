import React, { useState, useEffect } from 'react';
import { petApi, pointsApi, Pet, PointBalance } from '../utils/api';
import PetDisplay from '../components/PetDisplay';
import PetCreation from '../components/PetCreation';
import Shop from '../components/Shop';
import Achievements from '../components/Achievements';

const PetPage: React.FC = () => {
  const [pet, setPet] = useState<Pet | null>(null);
  const [pointBalance, setPointBalance] = useState<PointBalance>({
    currentBalance: 0,
    totalEarned: 0,
    totalSpent: 0
  });
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'pet' | 'shop' | 'achievements'>('pet');
  const [dailyLoginChecked, setDailyLoginChecked] = useState(false);

  useEffect(() => {
    loadPetData();
    checkDailyLogin();
  }, []);

  const loadPetData = async () => {
    setLoading(true);
    try {
      const [petResponse, balanceResponse] = await Promise.all([
        petApi.getUserPet(),
        pointsApi.getBalance()
      ]);

      if (petResponse.success && petResponse.data) {
        setPet(petResponse.data);
      }

      if (balanceResponse.success && balanceResponse.data) {
        setPointBalance(balanceResponse.data);
      }
    } catch (error) {
      console.error('Failed to load pet data:', error);
    } finally {
      setLoading(false);
    }
  };

  const checkDailyLogin = async () => {
    if (dailyLoginChecked) return;
    
    try {
      const response = await pointsApi.checkDailyLogin();
      if (response.success && response.data) {
        // User got daily login bonus
        setPointBalance(prev => ({
          ...prev,
          currentBalance: prev.currentBalance + response.data!.amount,
          totalEarned: prev.totalEarned + response.data!.amount
        }));
        
        // Show a notification or toast here if needed
        console.log('Daily login bonus received!', response.data.amount, 'points');
      }
    } catch (error) {
      console.error('Failed to check daily login:', error);
    } finally {
      setDailyLoginChecked(true);
    }
  };

  const handlePetCreated = (newPet: Pet) => {
    setPet(newPet);
    loadPetData(); // Refresh all data
  };

  const handlePetUpdate = (updatedPet: Pet) => {
    setPet(updatedPet);
  };

  const handlePointsUpdate = (updatedBalance: PointBalance) => {
    setPointBalance(updatedBalance);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading your pet...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-4">
              <h1 className="text-2xl font-bold text-gray-900">CodeMate Pet System</h1>
              {pet && (
                <div className="flex items-center space-x-2 text-sm text-gray-600">
                  <span>🐾 {pet.name}</span>
                  <span>💰 {pointBalance.currentBalance} points</span>
                </div>
              )}
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => window.location.href = '/dashboard'}
                className="text-gray-600 hover:text-gray-800 transition-colors"
              >
                ← Back to Dashboard
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {!pet ? (
          /* Pet Creation */
          <div className="max-w-md mx-auto">
            <PetCreation onPetCreated={handlePetCreated} />
          </div>
        ) : (
          /* Main Pet Interface */
          <div>
            {/* Navigation Tabs */}
            <div className="mb-8">
              <div className="border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                  <button
                    onClick={() => setActiveTab('pet')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'pet'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                  >
                    🐾 My Pet
                  </button>
                  <button
                    onClick={() => setActiveTab('shop')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'shop'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                  >
                    🛒 Shop
                  </button>
                  <button
                    onClick={() => setActiveTab('achievements')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'achievements'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                  >
                    🏆 Achievements
                  </button>
                </nav>
              </div>
            </div>

            {/* Tab Content */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {activeTab === 'pet' && (
                <>
                  {/* Pet Display - Main Column */}
                  <div className="lg:col-span-2">
                    <PetDisplay
                      pet={pet}
                      pointBalance={pointBalance}
                      onPetUpdate={handlePetUpdate}
                      onPointsUpdate={handlePointsUpdate}
                    />
                  </div>
                  
                  {/* Quick Stats Sidebar */}
                  <div className="space-y-6">
                    {/* Points Summary */}
                    <div className="bg-white rounded-lg shadow-md p-6">
                      <h3 className="text-lg font-semibold text-gray-800 mb-4">Points Summary</h3>
                      <div className="space-y-3">
                        <div className="flex justify-between">
                          <span className="text-gray-600">Current Balance:</span>
                          <span className="font-semibold text-green-600">{pointBalance.currentBalance}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-600">Total Earned:</span>
                          <span className="font-semibold text-blue-600">{pointBalance.totalEarned}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-600">Total Spent:</span>
                          <span className="font-semibold text-red-600">{pointBalance.totalSpent}</span>
                        </div>
                      </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="bg-white rounded-lg shadow-md p-6">
                      <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
                      <div className="space-y-3">
                        <button
                          onClick={() => setActiveTab('shop')}
                          className="w-full py-2 px-4 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
                        >
                          🛒 Visit Shop
                        </button>
                        <button
                          onClick={() => setActiveTab('achievements')}
                          className="w-full py-2 px-4 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition-colors"
                        >
                          🏆 View Achievements
                        </button>
                      </div>
                    </div>

                    {/* Tips */}
                    <div className="bg-blue-50 rounded-lg p-4">
                      <h4 className="font-semibold text-blue-800 mb-2">💡 Tips</h4>
                      <ul className="text-sm text-blue-700 space-y-1">
                        <li>• Feed your pet daily to keep them happy!</li>
                        <li>• Earn points by using CodeMate features</li>
                        <li>• Buy items from the shop to boost happiness</li>
                        <li>• Complete achievements for extra rewards</li>
                      </ul>
                    </div>
                  </div>
                </>
              )}

              {activeTab === 'shop' && (
                <div className="lg:col-span-3">
                  <Shop
                    pointBalance={pointBalance}
                    onPointsUpdate={handlePointsUpdate}
                    onPetUpdate={handlePetUpdate}
                  />
                </div>
              )}

              {activeTab === 'achievements' && (
                <div className="lg:col-span-3">
                  <Achievements />
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PetPage; 