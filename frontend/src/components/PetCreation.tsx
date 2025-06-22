import React, { useState } from 'react';
import { petApi, Pet } from '../utils/api';
import { PetCharacter } from './pet';
import { getPetImage } from '../utils/petUtils';


interface PetCreationProps {
  onPetCreated: (pet: Pet) => void;
}

const PetCreation: React.FC<PetCreationProps> = ({ onPetCreated }) => {
  const [petName, setPetName] = useState('');
  const [petType, setPetType] = useState<'DUKE_JAVA' | 'COFFEE_BEAN' | 'CODEMATE_MASCOT'>('CODEMATE_MASCOT');
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');

  const petTypes = [
    { value: 'CODEMATE_MASCOT' as const, label: 'CodeMate Mascot', emoji: '🤖', description: 'The official CodeMate mascot' },
    { value: 'DUKE_JAVA' as const, label: 'Duke Java', emoji: '☕', description: 'A coffee-loving companion' },
    { value: 'COFFEE_BEAN' as const, label: 'Coffee Bean', emoji: '🫘', description: 'A cute little bean' }
  ];

  const handleCreatePet = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!petName.trim()) {
      setError('Please enter a name for your pet');
      return;
    }

    setIsCreating(true);
    setError('');

    try {
      const response = await petApi.createPet({
        name: petName.trim(),
        type: petType
      });

      if (response.success && response.data) {
        onPetCreated(response.data);
      } else {
        setError(response.message || 'Failed to create pet');
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to create pet');
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 max-w-md mx-auto">
      <div className="text-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800 mb-2">Create Your Virtual Pet</h2>
        <p className="text-gray-600">Welcome to CodeMate! Let's create your coding companion.</p>
      </div>

      <form onSubmit={handleCreatePet} className="space-y-6">
        {/* Pet Name Input */}
        <div>
          <label htmlFor="petName" className="block text-sm font-medium text-gray-700 mb-2">
            Pet Name
          </label>
          <input
            type="text"
            id="petName"
            value={petName}
            onChange={(e) => setPetName(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter your pet's name"
            maxLength={50}
            disabled={isCreating}
          />
        </div>

        {/* Pet Type Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-3">
            Choose Pet Type
          </label>
          <div className="space-y-3">
            {petTypes.map((type) => (
              <label
                key={type.value}
                className={`flex items-center p-3 border rounded-lg cursor-pointer transition-colors ${
                  petType === type.value
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-300 hover:border-gray-400'
                } ${isCreating ? 'cursor-not-allowed opacity-50' : ''}`}
              >
                <input
                  type="radio"
                  name="petType"
                  value={type.value}
                  checked={petType === type.value}
                  onChange={(e) => setPetType(e.target.value as 'DUKE_JAVA' | 'COFFEE_BEAN' | 'CODEMATE_MASCOT')}
                  className="sr-only"
                  disabled={isCreating}
                />
                <div className="flex items-center flex-1">
                  <div className="mr-4 flex-shrink-0">
                    <PetCharacter
                      petType={type.value}
                      emotion="happy"
                      happiness={80}
                      size="sm"
                      customImage={getPetImage(type.value, 80)}
                    />
                  </div>
                  <div>
                    <div className="font-medium text-gray-800">{type.label}</div>
                    <div className="text-sm text-gray-600">{type.description}</div>
                  </div>
                </div>
                {petType === type.value && (
                  <div className="text-blue-500">
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                  </div>
                )}
              </label>
            ))}
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="text-red-500 text-sm text-center">
            {error}
          </div>
        )}

        {/* Create Button */}
        <button
          type="submit"
          disabled={isCreating || !petName.trim()}
          className={`w-full py-3 px-4 rounded-lg font-medium transition-colors ${
            isCreating || !petName.trim()
              ? 'bg-gray-300 cursor-not-allowed'
              : 'bg-blue-500 hover:bg-blue-600 text-white'
          }`}
        >
          {isCreating ? 'Creating Your Pet...' : 'Create My Pet'}
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-gray-500">
        <p>🎮 Your pet will help you track your coding progress!</p>
        <p>🏆 Earn points and unlock achievements as you code!</p>
      </div>
    </div>
  );
};

export default PetCreation; 