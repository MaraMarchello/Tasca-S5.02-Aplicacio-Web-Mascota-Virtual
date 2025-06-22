import React from 'react';
import { Pet, PointBalance } from '../../utils/api';

interface PetHeaderProps {
  pet: Pet | null;
  pointBalance: PointBalance;
  onAwardTestPoints: () => void;
}

const PetHeader: React.FC<PetHeaderProps> = ({ pet, pointBalance, onAwardTestPoints }) => {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center space-x-4">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">🐾 My Pet</h1>
        {pet && (
          <div className="flex items-center space-x-4 text-sm">
            <span className="px-3 py-1 bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 rounded-full">
              {pet.name}
            </span>
            <span className="px-4 py-2 bg-gradient-to-r from-yellow-100 to-yellow-200 dark:from-yellow-900 dark:to-yellow-800 text-yellow-800 dark:text-yellow-200 rounded-full font-semibold border border-yellow-300 dark:border-yellow-700">
              💰 {(pointBalance?.currentBalance || 0).toLocaleString()} points
            </span>
            {(pointBalance?.currentBalance === 0) && (
              <button
                onClick={onAwardTestPoints}
                className="px-3 py-1 bg-orange-500 text-white rounded-lg text-sm hover:bg-orange-600 transition-colors"
                title="Award 100 test points (admin function)"
              >
                🎁 Get Test Points
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default PetHeader; 