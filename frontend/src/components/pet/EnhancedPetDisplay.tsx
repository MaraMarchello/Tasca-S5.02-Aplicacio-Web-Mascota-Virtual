import React, { useState, useEffect } from 'react';
import PetCharacter from './PetCharacter';
import PetRoom from './PetRoom';
import { Card, CardHeader, CardTitle, CardContent } from '../ui';
import { Button } from '../ui';
import { useToast } from '../../contexts/ToastContext';

interface EnhancedPetDisplayProps {
  pet: {
    id: number;
    name: string;
    type: 'CAT' | 'DOG' | 'BIRD' | 'FISH';
    happiness: number;
    hunger?: number;
    energy?: number;
    experience?: number;
    level?: number;
    totalPointsEarned: number;
    lastFed: string;
    skin?: string;
    accessories?: string[];
  };
  pointBalance: {
    currentBalance: number;
    totalEarned: number;
    totalSpent: number;
  };
  items?: Array<{
    id: number;
    name: string;
    type: 'FOOD' | 'TOY' | 'ACCESSORY' | 'DECORATION';
    icon: string;
    quantity: number;
    happiness_boost?: number;
    hunger_restore?: number;
    energy_boost?: number;
  }>;
  onPetUpdate: (updatedPet: any) => void;
  onPointsUpdate: (updatedBalance: any) => void;
  viewMode?: 'card' | 'room';
}

const EnhancedPetDisplay: React.FC<EnhancedPetDisplayProps> = ({
  pet,
  pointBalance: _pointBalance,
  items = [],
  onPetUpdate,
  onPointsUpdate: _onPointsUpdate,
  viewMode = 'card'
}) => {
  const { showSuccess, showError } = useToast();
  const [isFeeding, setIsFeeding] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [lastInteraction, setLastInteraction] = useState<Date | null>(null);
  const [achievementReaction, setAchievementReaction] = useState(false);

  // Enhanced pet data with defaults
  const enhancedPet = {
    ...pet,
    hunger: pet.hunger ?? Math.max(100 - Math.floor((Date.now() - new Date(pet.lastFed).getTime()) / (1000 * 60 * 60)), 0),
    energy: pet.energy ?? Math.min(pet.happiness + 20, 100),
    experience: pet.experience ?? pet.totalPointsEarned,
    level: pet.level ?? Math.floor(pet.totalPointsEarned / 100) + 1,
  };

  // Check for achievements when pet stats change
  useEffect(() => {
    const checkAchievements = () => {
      if (enhancedPet.happiness >= 90 && !achievementReaction) {
        setAchievementReaction(true);
        showSuccess('🎉 Your pet is extremely happy!');
        setTimeout(() => setAchievementReaction(false), 3000);
      }
    };

    checkAchievements();
  }, [enhancedPet.happiness, achievementReaction, showSuccess]);

  const handleFeedPet = async (_itemId?: number) => {
    setIsFeeding(true);
    try {
      // Mock API call - replace with actual API
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const updatedPet = {
        ...enhancedPet,
        happiness: Math.min(enhancedPet.happiness + 15, 100),
        hunger: Math.min(enhancedPet.hunger + 30, 100),
        lastFed: new Date().toISOString()
      };
      
      onPetUpdate(updatedPet);
      showSuccess(`${pet.name} enjoyed the meal! 🍽️`);
      setLastInteraction(new Date());
    } catch (error) {
      showError('Failed to feed pet');
    } finally {
      setIsFeeding(false);
    }
  };

  const handlePlayWithPet = async (_itemId?: number) => {
    setIsPlaying(true);
    try {
      // Mock API call - replace with actual API
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const updatedPet = {
        ...enhancedPet,
        happiness: Math.min(enhancedPet.happiness + 10, 100),
        energy: Math.max(enhancedPet.energy - 10, 0),
        experience: enhancedPet.experience + 5
      };
      
      onPetUpdate(updatedPet);
      showSuccess(`${pet.name} had fun playing! 🎾`);
      setLastInteraction(new Date());
    } catch (error) {
      showError('Failed to play with pet');
    } finally {
      setIsPlaying(false);
    }
  };

  const handleUseItem = async (itemId: number) => {
    try {
      const item = items.find(i => i.id === itemId);
      if (!item) return;

      // Mock API call - replace with actual API
      await new Promise(resolve => setTimeout(resolve, 500));
      
      const updatedPet = {
        ...enhancedPet,
        happiness: Math.min(enhancedPet.happiness + (item.happiness_boost || 0), 100),
        hunger: Math.min(enhancedPet.hunger + (item.hunger_restore || 0), 100),
        energy: Math.min(enhancedPet.energy + (item.energy_boost || 0), 100),
      };
      
      onPetUpdate(updatedPet);
      showSuccess(`Used ${item.name} on ${pet.name}! ${item.icon}`);
    } catch (error) {
      showError('Failed to use item');
    }
  };

  const handlePetInteraction = () => {
    const messages = [
      `${pet.name} purrs contentedly! 😸`,
      `${pet.name} nuzzles against you! 🥰`,
      `${pet.name} seems happy to see you! 😊`,
      `${pet.name} wags their tail! 🐕`,
      `${pet.name} chirps happily! 🐦`
    ];
    
    const randomMessage = messages[Math.floor(Math.random() * messages.length)];
    showSuccess(randomMessage);
    setLastInteraction(new Date());
    
    // Small happiness boost for interaction
    const updatedPet = {
      ...enhancedPet,
      happiness: Math.min(enhancedPet.happiness + 2, 100)
    };
    onPetUpdate(updatedPet);
  };

  const getTimeSinceLastFed = () => {
    const lastFedDate = new Date(pet.lastFed);
    const now = new Date();
    const diffInHours = Math.floor((now.getTime() - lastFedDate.getTime()) / (1000 * 60 * 60));
    
    if (diffInHours < 1) return 'Just fed';
    if (diffInHours === 1) return '1 hour ago';
    if (diffInHours < 24) return `${diffInHours} hours ago`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    return diffInDays === 1 ? '1 day ago' : `${diffInDays} days ago`;
  };

  // Room view
  if (viewMode === 'room') {
    return (
      <PetRoom
        pet={enhancedPet}
        items={items}
        onFeedPet={handleFeedPet}
        onPlayWithPet={handlePlayWithPet}
        onUseItem={handleUseItem}
        onPetInteraction={handlePetInteraction}
      />
    );
  }

  // Card view (default)
  return (
    <div className="space-y-6">
      {/* Main Pet Card */}
      <Card variant="elevated" className="overflow-hidden">
        <CardHeader className="bg-gradient-to-r from-primary-50 to-secondary-50 dark:from-primary-950 dark:to-secondary-950">
          <div className="flex items-center justify-between">
            <CardTitle className="text-2xl font-bold">
              {pet.name} the {pet.type.toLowerCase()}
            </CardTitle>
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-600 dark:text-gray-400">
                Level {enhancedPet.level}
              </span>
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" title="Online" />
            </div>
          </div>
        </CardHeader>
        
        <CardContent className="p-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Pet Character Display */}
            <div className="flex flex-col items-center justify-center space-y-4">
              <PetCharacter
                petType={pet.type}
                happiness={enhancedPet.happiness}
                size="xl"
                skin={pet.skin}
                accessories={pet.accessories}
                emotion={achievementReaction ? 'excited' : undefined}
                onClick={handlePetInteraction}
                className="mb-4"
              />
              
              {/* Pet Status */}
              <div className="text-center space-y-2">
                <div className="flex items-center justify-center space-x-4 text-sm">
                  <span className="flex items-center space-x-1">
                    <span>😊</span>
                    <span>{enhancedPet.happiness}%</span>
                  </span>
                  <span className="flex items-center space-x-1">
                    <span>🍽️</span>
                    <span>{enhancedPet.hunger}%</span>
                  </span>
                  <span className="flex items-center space-x-1">
                    <span>⚡</span>
                    <span>{enhancedPet.energy}%</span>
                  </span>
                </div>
                
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Last fed: {getTimeSinceLastFed()}
                </p>
                
                {lastInteraction && (
                  <p className="text-xs text-primary-600 dark:text-primary-400">
                    Last interaction: {lastInteraction.toLocaleTimeString()}
                  </p>
                )}
              </div>
            </div>

            {/* Pet Actions and Stats */}
            <div className="space-y-6">
              {/* Quick Actions */}
              <div className="space-y-3">
                <h3 className="font-semibold text-gray-800 dark:text-white">Quick Actions</h3>
                <div className="grid grid-cols-2 gap-3">
                  <Button
                    variant="primary"
                    onClick={() => handleFeedPet()}
                    disabled={isFeeding}
                    className="flex items-center justify-center space-x-2"
                  >
                    <span>🍽️</span>
                    <span>{isFeeding ? 'Feeding...' : 'Feed'}</span>
                  </Button>
                  
                  <Button
                    variant="secondary"
                    onClick={() => handlePlayWithPet()}
                    disabled={isPlaying}
                    className="flex items-center justify-center space-x-2"
                  >
                    <span>🎾</span>
                    <span>{isPlaying ? 'Playing...' : 'Play'}</span>
                  </Button>
                  
                  <Button
                    variant="outline"
                    onClick={handlePetInteraction}
                    className="flex items-center justify-center space-x-2"
                  >
                    <span>🤗</span>
                    <span>Pet</span>
                  </Button>
                  
                  <Button
                    variant="ghost"
                    onClick={() => {/* Open room view */}}
                    className="flex items-center justify-center space-x-2"
                  >
                    <span>🏠</span>
                    <span>Room</span>
                  </Button>
                </div>
              </div>

              {/* Experience Progress */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Experience</span>
                  <span className="text-sm text-gray-600 dark:text-gray-400">
                    {enhancedPet.experience} XP
                  </span>
                </div>
                <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                  <div
                    className="h-2 bg-gradient-to-r from-purple-400 to-purple-600 rounded-full transition-all duration-500"
                    style={{ 
                      width: `${Math.min(((enhancedPet.experience % 100) / 100) * 100, 100)}%` 
                    }}
                  />
                </div>
              </div>

              {/* Pet Care Tips */}
              <div className="bg-blue-50 dark:bg-blue-950 rounded-lg p-4">
                <h4 className="font-semibold text-blue-800 dark:text-blue-200 mb-2">
                  💡 Care Tips
                </h4>
                <ul className="text-sm text-blue-700 dark:text-blue-300 space-y-1">
                  <li>• Feed regularly to maintain happiness</li>
                  <li>• Play to build experience and bond</li>
                  <li>• Use items from your inventory</li>
                  <li>• Visit the pet room for more activities</li>
                </ul>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Achievements Notification */}
      {achievementReaction && (
        <Card variant="elevated" className="border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950">
          <CardContent className="p-4">
            <div className="flex items-center space-x-3">
              <span className="text-2xl animate-bounce">🏆</span>
              <div>
                <p className="font-semibold text-yellow-800 dark:text-yellow-200">
                  Achievement Unlocked!
                </p>
                <p className="text-sm text-yellow-700 dark:text-yellow-300">
                  Your pet reached maximum happiness!
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default EnhancedPetDisplay; 