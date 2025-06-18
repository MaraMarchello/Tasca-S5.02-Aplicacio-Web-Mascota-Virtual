import React, { useState } from 'react';
import { petApi, pointsApi, Pet, PointBalance } from '../utils/api';

interface PetDisplayProps {
  pet: Pet;
  pointBalance: PointBalance;
  onPetUpdate: (pet: Pet) => void;
  onPointsUpdate: (balance: PointBalance) => void;
}

const PetDisplay: React.FC<PetDisplayProps> = ({ 
  pet, 
  pointBalance, 
  onPetUpdate, 
  onPointsUpdate 
}) => {
  const [isFeeding, setIsFeeding] = useState(false);
  const [feedingMessage, setFeedingMessage] = useState('');

  const handleFeedPet = async () => {
    if (isFeeding) return;
    
    setIsFeeding(true);
    setFeedingMessage('');

    try {
      const response = await petApi.feedPet();
      if (response.success && response.data) {
        onPetUpdate(response.data);
        setFeedingMessage('Your pet is happy! 🎉');
        
        // Refresh points balance
        const balanceResponse = await pointsApi.getBalance();
        if (balanceResponse.success && balanceResponse.data) {
          onPointsUpdate(balanceResponse.data);
        }
      }
    } catch (error) {
      setFeedingMessage(error instanceof Error ? error.message : 'Failed to feed pet');
    } finally {
      setIsFeeding(false);
    }
  };

  const getHappinessColor = (happiness: number) => {
    if (happiness >= 80) return 'text-green-500';
    if (happiness >= 60) return 'text-yellow-500';
    if (happiness >= 40) return 'text-orange-500';
    return 'text-red-500';
  };

  const getHappinessEmoji = (happiness: number) => {
    if (happiness >= 80) return '😄';
    if (happiness >= 60) return '😊';
    if (happiness >= 40) return '😐';
    if (happiness >= 20) return '😞';
    return '😢';
  };

  const getPetTypeEmoji = (type: string) => {
    switch (type.toUpperCase()) {
      case 'DUKE_JAVA':
        return '☕';
      case 'COFFEE_BEAN':
        return '🫘';
      default:
        return '🐾';
    }
  };

  const formatLastFed = (lastFed: string) => {
    const lastFedDate = new Date(lastFed);
    const now = new Date();
    const diffInHours = Math.floor((now.getTime() - lastFedDate.getTime()) / (1000 * 60 * 60));
    
    if (diffInHours < 1) return 'Less than an hour ago';
    if (diffInHours === 1) return '1 hour ago';
    if (diffInHours < 24) return `${diffInHours} hours ago`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays === 1) return '1 day ago';
    return `${diffInDays} days ago`;
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="text-center mb-6">
        <div className="text-6xl mb-2">
          {getPetTypeEmoji(pet.type)}
        </div>
        <h2 className="text-2xl font-bold text-gray-800 mb-1">{pet.name}</h2>
        <p className="text-gray-600 capitalize">{pet.type.replace('_', ' ')}</p>
      </div>

      <div className="space-y-4">
        {/* Happiness Bar */}
        <div>
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-gray-700">Happiness</span>
            <span className={`text-lg ${getHappinessColor(pet.happiness)}`}>
              {getHappinessEmoji(pet.happiness)} {pet.happiness}%
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3">
            <div
              className={`h-3 rounded-full transition-all duration-500 ${
                pet.happiness >= 80 
                  ? 'bg-green-500' 
                  : pet.happiness >= 60 
                  ? 'bg-yellow-500' 
                  : pet.happiness >= 40 
                  ? 'bg-orange-500' 
                  : 'bg-red-500'
              }`}
              style={{ width: `${pet.happiness}%` }}
            ></div>
          </div>
        </div>

        {/* Pet Stats */}
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-gray-600">Total Points Earned:</span>
            <div className="font-semibold text-blue-600">{pet.totalPointsEarned}</div>
          </div>
          <div>
            <span className="text-gray-600">Last Fed:</span>
            <div className="font-semibold">{formatLastFed(pet.lastFed)}</div>
          </div>
        </div>

        {/* Feed Button */}
        <div className="pt-4">
          <button
            onClick={handleFeedPet}
            disabled={isFeeding}
            className={`w-full py-3 px-4 rounded-lg font-medium transition-colors ${
              isFeeding
                ? 'bg-gray-300 cursor-not-allowed'
                : 'bg-blue-500 hover:bg-blue-600 text-white'
            }`}
          >
            {isFeeding ? 'Feeding...' : 'Feed Pet (Free)'}
          </button>
          
          {feedingMessage && (
            <p className={`text-center text-sm mt-2 ${
              feedingMessage.includes('Failed') ? 'text-red-500' : 'text-green-500'
            }`}>
              {feedingMessage}
            </p>
          )}
        </div>

        {/* Current Points Display */}
        <div className="bg-gray-50 rounded-lg p-3 text-center">
          <div className="text-sm text-gray-600">Current Points</div>
          <div className="text-xl font-bold text-green-600">{pointBalance.currentBalance}</div>
        </div>
      </div>
    </div>
  );
};

export default PetDisplay; 