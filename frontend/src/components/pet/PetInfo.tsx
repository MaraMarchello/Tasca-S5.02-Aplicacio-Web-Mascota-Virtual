import React, { useState } from 'react';
import { Pet, PointBalance, PetItem } from '../../utils/api';
import { petApi } from '../../utils/api';
import PetCharacter from './PetCharacter';
import { getPetImage, getPetDisplayName } from '../../utils/petUtils';

interface PetInfoProps {
  pet: Pet;
  pointBalance: PointBalance;
  petItems: PetItem[];
  onPetUpdate: (pet: Pet) => void;
  onFeedPet: () => void;
  onAwardTestPoints: () => void;
  onLoadPetData: () => void;
  onCheckDailyLogin: () => void;
  onDeletePet?: () => Promise<void>;
}

const PetInfo: React.FC<PetInfoProps> = ({
  pet,
  pointBalance,
  petItems,
  onPetUpdate,
  onFeedPet,
  onAwardTestPoints,
  onLoadPetData,
  onCheckDailyLogin,
  onDeletePet
}) => {
  const [isEditingName, setIsEditingName] = useState(false);
  const [newPetName, setNewPetName] = useState('');
  const [isUpdatingName, setIsUpdatingName] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleStartEditName = () => {
    setNewPetName(pet?.name || '');
    setIsEditingName(true);
  };

  const handleCancelEditName = () => {
    setIsEditingName(false);
    setNewPetName('');
  };

  const handleSavePetName = async () => {
    if (!newPetName.trim() || !pet) return;
    
    setIsUpdatingName(true);
    try {
      const response = await petApi.updatePetName({ name: newPetName.trim() });
      if (response.success && response.data) {
        onPetUpdate(response.data);
        setIsEditingName(false);
        setNewPetName('');
        console.log('✅ Pet name updated successfully');
      }
    } catch (error) {
      console.error('❌ Failed to update pet name:', error);
    } finally {
      setIsUpdatingName(false);
    }
  };

  const handleDeletePet = async () => {
    if (!onDeletePet) return;
    
    setIsDeleting(true);
    try {
      await onDeletePet();
      setShowDeleteConfirm(false);
      // Note: Pet state will be reset by the hook
    } catch (error) {
      console.error('❌ Failed to delete pet:', error);
      alert('Failed to delete pet. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm">
        <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Your Pet</h3>
        <div className="text-center space-y-4">
          <div className="flex justify-center">
            <PetCharacter
              petType={pet?.type as any}
              happiness={pet?.happiness || 50}
              size="xl"
              customImage={pet ? getPetImage(pet.type, pet.happiness) : undefined}
            />
          </div>
          <div>
            <div className="space-y-2">
              {isEditingName ? (
                <div className="space-y-2">
                  <input
                    type="text"
                    value={newPetName}
                    onChange={(e) => setNewPetName(e.target.value)}
                    className="text-xl font-medium text-center bg-white dark:bg-gray-700 text-gray-900 dark:text-white border-2 border-blue-300 rounded-lg px-3 py-1 focus:outline-none focus:border-blue-500"
                    maxLength={50}
                    placeholder="Enter pet name"
                    autoFocus
                  />
                  <div className="space-x-2">
                    <button
                      onClick={handleSavePetName}
                      disabled={isUpdatingName || !newPetName.trim()}
                      className="px-3 py-1 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 disabled:opacity-50"
                    >
                      {isUpdatingName ? 'Saving...' : 'Save'}
                    </button>
                    <button
                      onClick={handleCancelEditName}
                      className="px-3 py-1 bg-gray-500 text-white rounded-lg text-sm hover:bg-gray-600"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-2">
                  <div className="flex items-center justify-center gap-2">
                    <h4 className="text-xl font-medium text-gray-900 dark:text-white">{pet?.name || 'Unknown'}</h4>
                    <button
                      onClick={handleStartEditName}
                      className="text-blue-500 hover:text-blue-600 text-sm"
                      title="Edit pet name"
                    >
                      ✏️
                    </button>
                  </div>
                  <p className="text-gray-600 dark:text-gray-400">{pet ? getPetDisplayName(pet.type) : 'Unknown'}</p>
                  {pet?.name === 'Test pet' && (
                    <div className="text-sm text-orange-600 bg-orange-50 px-3 py-1 rounded-full inline-block">
                      💡 Click the ✏️ icon to give your pet a proper name!
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 max-w-xs mx-auto">
            <div className="bg-green-50 dark:bg-green-900 p-3 rounded-lg">
              <div className="text-sm text-green-600 dark:text-green-400">Happiness</div>
              <div className="text-lg font-semibold text-green-700 dark:text-green-300">{pet?.happiness || 0}%</div>
            </div>
            <div className="bg-purple-50 dark:bg-purple-900 p-3 rounded-lg">
              <div className="text-sm text-purple-600 dark:text-purple-400">Current Points</div>
              <div className="text-lg font-semibold text-purple-700 dark:text-purple-300">{pointBalance?.currentBalance || 0}</div>
            </div>
          </div>
          
          {/* Pet Interaction Buttons */}
          <div className="flex justify-center gap-4 mt-6">
            <button
              onClick={onFeedPet}
              className="flex items-center gap-2 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
            >
              🍽️ Feed Pet
            </button>
            <button
              onClick={() => console.log('🎾 Play with pet')}
              className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              🎾 Play
            </button>
          </div>

          {/* Points Debug Section - only show if points are 0 */}
          {(pointBalance?.currentBalance === 0) && (
            <div className="mt-6 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border">
              <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                🛠️ Points Troubleshooting
              </h4>
              <div className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
                <p>• Make sure you're logged in</p>
                <p>• Check if backend server is running</p>
                <p>• Try the "Get Test Points" button above</p>
                <p>• Visit admin panel to award points manually</p>
              </div>
              <div className="mt-3 flex gap-2 flex-wrap">
                <button
                  onClick={onLoadPetData}
                  className="px-3 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600"
                >
                  🔄 Refresh Data
                </button>
                <button
                  onClick={onCheckDailyLogin}
                  className="px-3 py-1 bg-green-500 text-white rounded text-sm hover:bg-green-600"
                >
                  🎁 Try Daily Login
                </button>
                <button
                  onClick={onAwardTestPoints}
                  className="px-3 py-1 bg-purple-500 text-white rounded text-sm hover:bg-purple-600"
                >
                  🎁 Get Test Points
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
      <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm">
        <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Pet Stats</h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">Created:</span>
            <span className="text-gray-900 dark:text-white">{pet?.createdAt ? new Date(pet.createdAt).toLocaleDateString() : 'Unknown'}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">Last Fed:</span>
            <span className="text-gray-900 dark:text-white">{pet?.lastFed ? new Date(pet.lastFed).toLocaleDateString() : 'Unknown'}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">Items Owned:</span>
            <span className="text-gray-900 dark:text-white">{petItems?.length || 0}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">Total Points Earned:</span>
            <span className="text-gray-900 dark:text-white">{pointBalance?.totalEarned || pet?.totalPointsEarned || 0}</span>
          </div>
        </div>
      </div>
      
      {/* Delete Pet Section */}
      {onDeletePet && (
        <div className="bg-red-50 dark:bg-red-900/20 rounded-lg p-6 shadow-sm border border-red-200 dark:border-red-800">
          <h3 className="text-lg font-semibold mb-4 text-red-800 dark:text-red-300">⚠️ Danger Zone</h3>
          <div className="space-y-4">
            <div className="text-sm text-red-700 dark:text-red-400">
              <p><strong>Delete Pet:</strong> This action cannot be undone!</p>
              <ul className="mt-2 ml-4 list-disc space-y-1">
                <li>Your pet will be permanently deleted</li>
                <li>All pet items will be lost</li>
                <li>You can create a new pet afterwards</li>
                <li>Your points will remain unchanged</li>
              </ul>
            </div>
            
            {!showDeleteConfirm ? (
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm"
              >
                🗑️ Delete Pet
              </button>
            ) : (
              <div className="space-y-3">
                <div className="p-3 bg-red-100 dark:bg-red-900/40 rounded-lg border border-red-300 dark:border-red-700">
                  <p className="text-red-800 dark:text-red-300 font-medium">
                    Are you sure you want to delete "{pet?.name}"?
                  </p>
                  <p className="text-red-700 dark:text-red-400 text-sm mt-1">
                    This action is permanent and cannot be undone.
                  </p>
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={handleDeletePet}
                    disabled={isDeleting}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors text-sm"
                  >
                    {isDeleting ? '🔄 Deleting...' : '✅ Yes, Delete Pet'}
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={isDeleting}
                    className="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 disabled:opacity-50 transition-colors text-sm"
                  >
                    ❌ Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default PetInfo; 