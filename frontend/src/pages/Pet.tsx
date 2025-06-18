import React, { useState, useEffect } from 'react';
import { petApi, pointsApi, Pet, PointBalance } from '../utils/api';
import Layout from '../components/layout/Layout';
import PetCreation from '../components/PetCreation';
import Shop from '../components/Shop';
import Achievements from '../components/Achievements';
import { EnhancedPetDisplay } from '../components/pet';

const PetPage: React.FC = () => {
  const [pet, setPet] = useState<Pet | null>(null);
  const [pointBalance, setPointBalance] = useState<PointBalance>({
    currentBalance: 0,
    totalEarned: 0,
    totalSpent: 0
  });
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'pet' | 'room' | 'shop' | 'achievements'>('pet');
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
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500 mx-auto"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading your pet...</p>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <h1 className="text-3xl font-bold text-text-light dark:text-text-dark">🐾 My Pet</h1>
            {pet && (
              <div className="flex items-center space-x-4 text-sm">
                <span className="px-3 py-1 bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 rounded-full">
                  {pet.name}
                </span>
                <span className="px-3 py-1 bg-secondary-100 dark:bg-secondary-900 text-secondary-700 dark:text-secondary-300 rounded-full">
                  💰 {pointBalance.currentBalance} points
                </span>
              </div>
            )}
          </div>
        </div>
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
              <div className="border-b border-border-light dark:border-border-dark">
                <nav className="-mb-px flex space-x-8">
                  <button
                    onClick={() => setActiveTab('pet')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'pet'
                        ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    }`}
                  >
                    🐾 My Pet
                  </button>
                  <button
                    onClick={() => setActiveTab('room')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'room'
                        ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    }`}
                  >
                    🏠 Pet Room
                  </button>
                  <button
                    onClick={() => setActiveTab('shop')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'shop'
                        ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    }`}
                  >
                    🛒 Shop
                  </button>
                  <button
                    onClick={() => setActiveTab('achievements')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                      activeTab === 'achievements'
                        ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:border-gray-300 dark:hover:border-gray-600'
                    }`}
                  >
                    🏆 Achievements
                  </button>
                </nav>
              </div>
            </div>

            {/* Tab Content */}
            <div>
              {activeTab === 'pet' && (
                <EnhancedPetDisplay
                  pet={pet as any}
                  pointBalance={pointBalance}
                  onPetUpdate={handlePetUpdate}
                  onPointsUpdate={handlePointsUpdate}
                  viewMode="card"
                />
              )}

              {activeTab === 'room' && (
                <EnhancedPetDisplay
                  pet={pet as any}
                  pointBalance={pointBalance}
                  onPetUpdate={handlePetUpdate}
                  onPointsUpdate={handlePointsUpdate}
                  viewMode="room"
                />
              )}

              {activeTab === 'shop' && (
                <Shop
                  pointBalance={pointBalance}
                  onPointsUpdate={handlePointsUpdate}
                  onPetUpdate={handlePetUpdate}
                />
              )}

              {activeTab === 'achievements' && (
                <Achievements />
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default PetPage; 