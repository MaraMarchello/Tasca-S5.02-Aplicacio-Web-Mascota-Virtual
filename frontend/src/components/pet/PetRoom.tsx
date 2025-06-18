import React, { useState, useEffect } from 'react';
import { clsx } from 'clsx';
import PetCharacter from './PetCharacter';
import { Card, CardHeader, CardTitle, CardContent } from '../ui';
import { Button } from '../ui';

interface PetRoomProps {
  pet: {
    id: number;
    name: string;
    type: 'CAT' | 'DOG' | 'BIRD' | 'FISH';
    happiness: number;
    hunger: number;
    energy: number;
    experience: number;
    level: number;
    totalPointsEarned: number;
    lastFed: string;
    skin?: string;
    accessories?: string[];
  };
  items: Array<{
    id: number;
    name: string;
    type: 'FOOD' | 'TOY' | 'ACCESSORY' | 'DECORATION';
    icon: string;
    quantity: number;
    happiness_boost?: number;
    hunger_restore?: number;
    energy_boost?: number;
  }>;
  onFeedPet: (itemId: number) => Promise<void>;
  onPlayWithPet: (itemId: number) => Promise<void>;
  onUseItem: (itemId: number) => Promise<void>;
  onPetInteraction: () => void;
}

const PetRoom: React.FC<PetRoomProps> = ({
  pet,
  items,
  onFeedPet,
  onPlayWithPet,
  onUseItem,
  onPetInteraction
}) => {
  const [selectedItem, setSelectedItem] = useState<number | null>(null);
  const [roomTheme, setRoomTheme] = useState<'cozy' | 'modern' | 'garden' | 'space'>('cozy');
  const [showStats, setShowStats] = useState(true);
  const [petEmotion, setPetEmotion] = useState<'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'>('happy');

  // Calculate XP progress
  const currentLevelXP = pet.level * 100;
  const nextLevelXP = (pet.level + 1) * 100;
  const xpProgress = ((pet.experience - currentLevelXP) / (nextLevelXP - currentLevelXP)) * 100;

  // Update pet emotion based on stats
  useEffect(() => {
    if (pet.hunger < 20) setPetEmotion('hungry');
    else if (pet.energy < 20) setPetEmotion('sleeping');
    else if (pet.happiness >= 80) setPetEmotion('happy');
    else if (pet.happiness < 40) setPetEmotion('sad');
    else setPetEmotion('excited');
  }, [pet.hunger, pet.energy, pet.happiness]);

  // Room background themes
  const roomThemes = {
    cozy: 'bg-gradient-to-b from-amber-100 to-orange-200 dark:from-amber-900 dark:to-orange-800',
    modern: 'bg-gradient-to-b from-slate-100 to-slate-300 dark:from-slate-800 dark:to-slate-900',
    garden: 'bg-gradient-to-b from-green-100 to-green-300 dark:from-green-900 dark:to-green-800',
    space: 'bg-gradient-to-b from-purple-900 to-indigo-900'
  };

  // Progress bar component
  const ProgressBar: React.FC<{ 
    label: string; 
    value: number; 
    max: number; 
    color: string; 
    icon: string 
  }> = ({ label, value, max, color, icon }) => (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-sm">
        <span className="flex items-center space-x-1">
          <span>{icon}</span>
          <span className="font-medium">{label}</span>
        </span>
        <span className="text-gray-600 dark:text-gray-400">{value}/{max}</span>
      </div>
      <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
        <div
          className={clsx(
            'h-3 rounded-full transition-all duration-500 ease-out',
            color
          )}
          style={{ width: `${Math.min((value / max) * 100, 100)}%` }}
        />
      </div>
    </div>
  );

  // Item grid component
  const ItemGrid: React.FC<{ items: typeof items; onItemClick: (id: number) => void }> = ({ 
    items, 
    onItemClick 
  }) => (
    <div className="grid grid-cols-4 gap-2">
      {items.map((item) => (
        <button
          key={item.id}
          onClick={() => onItemClick(item.id)}
          className={clsx(
            'relative p-3 rounded-xl border-2 transition-all duration-200 hover:scale-105',
            selectedItem === item.id
              ? 'border-primary-500 bg-primary-50 dark:bg-primary-950'
              : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-primary-300'
          )}
        >
          <div className="text-2xl mb-1">{item.icon}</div>
          <div className="text-xs font-medium text-gray-700 dark:text-gray-300 truncate">
            {item.name}
          </div>
          {item.quantity > 1 && (
            <div className="absolute -top-1 -right-1 w-5 h-5 bg-primary-500 text-white text-xs rounded-full flex items-center justify-center">
              {item.quantity}
            </div>
          )}
        </button>
      ))}
    </div>
  );

  const handleItemUse = async (itemId: number) => {
    const item = items.find(i => i.id === itemId);
    if (!item) return;

    try {
      switch (item.type) {
        case 'FOOD':
          await onFeedPet(itemId);
          break;
        case 'TOY':
          await onPlayWithPet(itemId);
          break;
        default:
          await onUseItem(itemId);
      }
      setSelectedItem(null);
    } catch (error) {
      console.error('Failed to use item:', error);
    }
  };

  return (
    <div className={clsx(
      'min-h-screen p-6 transition-all duration-500',
      roomThemes[roomTheme]
    )}>
      {/* Room Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">
          {pet.name}'s Room
        </h1>
        
        {/* Room Theme Selector */}
        <div className="flex items-center space-x-2">
          {Object.keys(roomThemes).map((theme) => (
            <button
              key={theme}
              onClick={() => setRoomTheme(theme as keyof typeof roomThemes)}
              className={clsx(
                'w-6 h-6 rounded-full border-2 transition-all',
                roomTheme === theme ? 'border-white scale-110' : 'border-gray-400 hover:scale-105',
                theme === 'cozy' && 'bg-orange-300',
                theme === 'modern' && 'bg-slate-400',
                theme === 'garden' && 'bg-green-400',
                theme === 'space' && 'bg-purple-600'
              )}
              title={theme.charAt(0).toUpperCase() + theme.slice(1)}
            />
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pet Interaction Area */}
        <div className="lg:col-span-2">
          <Card variant="elevated" className="h-full">
            <CardContent className="p-8">
              {/* Pet Display */}
              <div className="flex flex-col items-center justify-center h-64 mb-6">
                <PetCharacter
                  petType={pet.type}
                  happiness={pet.happiness}
                  size="xl"
                  skin={pet.skin}
                  accessories={pet.accessories}
                  emotion={petEmotion}
                  onClick={onPetInteraction}
                  className="mb-4"
                />
                
                {/* Pet Status Messages */}
                <div className="text-center">
                  <p className="text-lg font-semibold text-gray-800 dark:text-white mb-2">
                    {pet.name}
                  </p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {petEmotion === 'hungry' && "I'm hungry! 🍽️"}
                    {petEmotion === 'sleeping' && "I'm tired... 😴"}
                    {petEmotion === 'happy' && "I'm feeling great! 😊"}
                    {petEmotion === 'sad' && "I need some attention... 😢"}
                    {petEmotion === 'excited' && "Let's play! 🎉"}
                  </p>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="flex justify-center space-x-4">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    const foodItem = items.find(item => item.type === 'FOOD');
                    if (foodItem) handleItemUse(foodItem.id);
                  }}
                  disabled={!items.some(item => item.type === 'FOOD')}
                >
                  🍽️ Feed
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    const toyItem = items.find(item => item.type === 'TOY');
                    if (toyItem) handleItemUse(toyItem.id);
                  }}
                  disabled={!items.some(item => item.type === 'TOY')}
                >
                  🎾 Play
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onPetInteraction}
                >
                  🤗 Pet
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Stats and Inventory Sidebar */}
        <div className="space-y-6">
          {/* Pet Stats */}
          <Card variant="elevated">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Pet Stats</CardTitle>
                <button
                  onClick={() => setShowStats(!showStats)}
                  className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                >
                  {showStats ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
            </CardHeader>
            {showStats && (
              <CardContent className="space-y-4">
                <ProgressBar
                  label="Happiness"
                  value={pet.happiness}
                  max={100}
                  color="bg-gradient-to-r from-pink-400 to-pink-600"
                  icon="😊"
                />
                <ProgressBar
                  label="Hunger"
                  value={pet.hunger}
                  max={100}
                  color="bg-gradient-to-r from-orange-400 to-orange-600"
                  icon="🍽️"
                />
                <ProgressBar
                  label="Energy"
                  value={pet.energy}
                  max={100}
                  color="bg-gradient-to-r from-blue-400 to-blue-600"
                  icon="⚡"
                />
                
                {/* Level and XP */}
                <div className="pt-2 border-t border-gray-200 dark:border-gray-700">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">Level {pet.level}</span>
                    <span className="text-xs text-gray-600 dark:text-gray-400">
                      {pet.experience} XP
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                    <div
                      className="h-2 bg-gradient-to-r from-purple-400 to-purple-600 rounded-full transition-all duration-500"
                      style={{ width: `${Math.max(xpProgress, 0)}%` }}
                    />
                  </div>
                </div>
              </CardContent>
            )}
          </Card>

          {/* Item Inventory */}
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="text-lg">Inventory</CardTitle>
            </CardHeader>
            <CardContent>
              {items.length > 0 ? (
                <>
                  <ItemGrid items={items} onItemClick={setSelectedItem} />
                  
                  {selectedItem && (
                    <div className="mt-4 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                      {(() => {
                        const item = items.find(i => i.id === selectedItem);
                        return item ? (
                          <div>
                            <div className="flex items-center justify-between mb-2">
                              <span className="font-medium">{item.name}</span>
                              <span className="text-2xl">{item.icon}</span>
                            </div>
                            <div className="text-xs text-gray-600 dark:text-gray-400 mb-3">
                              {item.happiness_boost && `+${item.happiness_boost} Happiness`}
                              {item.hunger_restore && ` +${item.hunger_restore} Hunger`}
                              {item.energy_boost && ` +${item.energy_boost} Energy`}
                            </div>
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handleItemUse(selectedItem)}
                              className="w-full"
                            >
                              Use Item
                            </Button>
                          </div>
                        ) : null;
                      })()}
                    </div>
                  )}
                </>
              ) : (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                  <div className="text-4xl mb-2">📦</div>
                  <p className="text-sm">No items yet</p>
                  <p className="text-xs">Visit the shop to buy items!</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default PetRoom; 