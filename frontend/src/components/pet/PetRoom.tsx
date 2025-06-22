import React, { useState, useEffect } from 'react';
import PetCharacter from './PetCharacter';
import { getPetImage } from '../../utils/petUtils';

interface SimplePet {
  id: number;
  name: string;
  type: string; // Can be any string to match our Pet type
  happiness: number;
  totalPointsEarned: number;
  lastFed: string;
  createdAt: string;
  updatedAt: string;
}

interface SimplePetItem {
  id: number;
  itemTemplate: {
    id: number;
    name: string;
    description: string;
    price: number;
    type: string;
    happinessBoost: number;
  };
  quantity: number;
  isEquipped: boolean;
  purchasedAt: string;
}

interface PetRoomProps {
  pet: SimplePet;
  items: SimplePetItem[];
  onFeedPet: () => Promise<void> | void;
  onPlayWithPet?: () => Promise<void> | void;
  onUseItem?: (itemId: number) => Promise<void>;
  onPetInteraction?: () => void;
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
  const [petEmotion, setPetEmotion] = useState<'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'>('happy');
  const [isInteracting, setIsInteracting] = useState(false);

  // Update pet emotion based on happiness
  useEffect(() => {
    if (pet.happiness >= 80) setPetEmotion('happy');
    else if (pet.happiness >= 60) setPetEmotion('excited');
    else if (pet.happiness >= 40) setPetEmotion('hungry');
    else setPetEmotion('sad');
  }, [pet.happiness]);

  // Room background themes
  const roomThemes = {
    cozy: 'bg-gradient-to-b from-amber-100 via-orange-50 to-orange-100 dark:from-amber-900 dark:via-orange-900 dark:to-orange-800',
    modern: 'bg-gradient-to-b from-slate-100 via-gray-50 to-slate-200 dark:from-slate-800 dark:via-slate-700 dark:to-slate-900',
    garden: 'bg-gradient-to-b from-green-100 via-emerald-50 to-green-200 dark:from-green-900 dark:via-emerald-900 dark:to-green-800',
    space: 'bg-gradient-to-b from-purple-900 via-indigo-800 to-purple-800'
  };

  // Get item icon
  const getItemIcon = (name: string, type: string) => {
    const iconMap: { [key: string]: string } = {
      'Coffee Bean': '☕',
      'Energy Drink': '⚡',
      'Healthy Snack': '🥜',
      'Premium Food': '🍖',
      'Coding Hat': '🎩',
      'Lucky Charm': '🍀'
    };
    return iconMap[name] || (type === 'FOOD' ? '🍽️' : type === 'ACCESSORY' ? '👔' : '📦');
  };

  const handlePetClick = async () => {
    if (onPetInteraction) {
      setIsInteracting(true);
      try {
        await onPetInteraction();
      } finally {
        setIsInteracting(false);
      }
    }
  };

  const handleItemUse = async (itemId: number) => {
    const item = items.find(i => i.id === itemId);
    if (!item || !onUseItem) return;

    try {
      await onUseItem(itemId);
      setSelectedItem(null);
    } catch (error) {
      console.error('Failed to use item:', error);
    }
  };

  const getCustomImage = () => {
    return getPetImage(pet.type, pet.happiness);
  };

  return (
    <div className={`min-h-[600px] rounded-xl p-8 transition-all duration-500 ${roomThemes[roomTheme]} relative overflow-hidden`}>
      {/* Decorative Elements */}
      <div className="absolute top-4 left-4 text-2xl opacity-30">🪴</div>
      <div className="absolute top-6 right-8 text-xl opacity-25">⭐</div>
      <div className="absolute bottom-6 left-8 text-3xl opacity-20">🏠</div>
      <div className="absolute bottom-4 right-4 text-2xl opacity-30">✨</div>

      {/* Room Header */}
      <div className="flex items-center justify-between mb-6 relative z-10">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 dark:text-white">
            🏠 {pet.name}'s Room
          </h2>
          <p className="text-sm text-gray-600 dark:text-gray-300">
            A cozy place for your pet to relax and play
          </p>
        </div>
        
        {/* Room Theme Selector */}
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-600 dark:text-gray-300 mr-2">Theme:</span>
          {Object.entries(roomThemes).map(([theme, _]) => (
            <button
              key={theme}
              onClick={() => setRoomTheme(theme as keyof typeof roomThemes)}
              className={`w-8 h-8 rounded-full border-2 transition-all hover:scale-110 ${
                roomTheme === theme ? 'border-white scale-110 shadow-lg' : 'border-gray-400'
              } ${
                theme === 'cozy' ? 'bg-orange-400' :
                theme === 'modern' ? 'bg-slate-400' :
                theme === 'garden' ? 'bg-green-400' :
                'bg-purple-600'
              }`}
              title={theme.charAt(0).toUpperCase() + theme.slice(1)}
            />
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 relative z-10">
        {/* Pet Interaction Area */}
        <div className="lg:col-span-2">
          <div className="bg-white/70 dark:bg-gray-800/70 backdrop-blur-sm rounded-xl p-8 shadow-lg border border-white/20">
            {/* Pet Display */}
            <div className="flex flex-col items-center justify-center h-80 mb-6">
              <div className="mb-6">
                <PetCharacter
                  petType={pet.type as any}
                  happiness={pet.happiness}
                  size="xl"
                  emotion={petEmotion}
                  onClick={handlePetClick}
                  className={`transition-transform duration-200 ${isInteracting ? 'scale-110' : 'hover:scale-105'} cursor-pointer`}
                  customImage={getCustomImage()}
                />
              </div>
              
              {/* Pet Status Messages */}
              <div className="text-center">
                <h3 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">
                  {pet.name}
                </h3>
                <div className="bg-white/60 dark:bg-gray-700/60 rounded-full px-4 py-2 mb-4">
                  <p className="text-sm text-gray-700 dark:text-gray-300">
                    {petEmotion === 'hungry' && "🍽️ I'm hungry! Feed me something tasty!"}
                    {petEmotion === 'sleeping' && "😴 I'm feeling sleepy..."}
                    {petEmotion === 'happy' && "😊 I'm feeling fantastic! Thanks for taking care of me!"}
                    {petEmotion === 'sad' && "😢 I need some love and attention..."}
                    {petEmotion === 'excited' && "🎉 I'm so excited! Let's have some fun!"}
                  </p>
                </div>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="flex justify-center space-x-4">
              <button
                onClick={onFeedPet}
                className="flex items-center gap-2 px-6 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors shadow-md"
              >
                🍽️ Feed Pet
              </button>
              <button
                onClick={onPlayWithPet || (() => console.log('Playing with pet!'))}
                className="flex items-center gap-2 px-6 py-3 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors shadow-md"
              >
                🎾 Play
              </button>
              <button
                onClick={handlePetClick}
                className="flex items-center gap-2 px-6 py-3 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition-colors shadow-md"
              >
                🤗 Pet
              </button>
            </div>
          </div>
        </div>

        {/* Stats and Inventory Sidebar */}
        <div className="space-y-6">
          {/* Pet Stats */}
          <div className="bg-white/70 dark:bg-gray-800/70 backdrop-blur-sm rounded-xl p-6 shadow-lg border border-white/20">
            <h3 className="text-lg font-bold text-gray-800 dark:text-white mb-4">Pet Stats</h3>
            
            {/* Happiness Bar */}
            <div className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="flex items-center space-x-2">
                  <span>😊</span>
                  <span className="font-medium">Happiness</span>
                </span>
                <span className="text-gray-600 dark:text-gray-400">{pet.happiness}/100</span>
              </div>
              <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                <div
                  className="h-3 rounded-full bg-gradient-to-r from-pink-400 to-pink-600 transition-all duration-500"
                  style={{ width: `${pet.happiness}%` }}
                />
              </div>
            </div>

            {/* Additional Stats */}
            <div className="mt-6 space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Total Points:</span>
                <span className="font-medium text-gray-800 dark:text-white">{pet.totalPointsEarned}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Last Fed:</span>
                <span className="font-medium text-gray-800 dark:text-white">
                  {new Date(pet.lastFed).toLocaleDateString()}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Items Owned:</span>
                <span className="font-medium text-gray-800 dark:text-white">{items.length}</span>
              </div>
            </div>
          </div>

          {/* Item Inventory */}
          <div className="bg-white/70 dark:bg-gray-800/70 backdrop-blur-sm rounded-xl p-6 shadow-lg border border-white/20">
            <h3 className="text-lg font-bold text-gray-800 dark:text-white mb-4">Pet Items</h3>
            
            {items.length > 0 ? (
              <>
                <div className="grid grid-cols-2 gap-3 mb-4">
                  {items.map((item) => (
                    <button
                      key={item.id}
                      onClick={() => setSelectedItem(selectedItem === item.id ? null : item.id)}
                      className={`relative p-3 rounded-lg border-2 transition-all duration-200 hover:scale-105 ${
                        selectedItem === item.id
                          ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/50'
                          : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 hover:border-blue-300'
                      }`}
                    >
                      <div className="text-2xl mb-1">
                        {getItemIcon(item.itemTemplate.name, item.itemTemplate.type)}
                      </div>
                      <div className="text-xs font-medium text-gray-700 dark:text-gray-300 truncate">
                        {item.itemTemplate.name}
                      </div>
                      {item.quantity > 1 && (
                        <div className="absolute -top-1 -right-1 w-5 h-5 bg-blue-500 text-white text-xs rounded-full flex items-center justify-center">
                          {item.quantity}
                        </div>
                      )}
                      {item.isEquipped && (
                        <div className="absolute top-1 left-1 text-xs">✨</div>
                      )}
                    </button>
                  ))}
                </div>
                
                {selectedItem && (
                  <div className="p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
                    {(() => {
                      const item = items.find(i => i.id === selectedItem);
                      return item ? (
                        <div>
                          <div className="flex items-center justify-between mb-2">
                            <span className="font-medium text-gray-800 dark:text-white">
                              {item.itemTemplate.name}
                            </span>
                            <span className="text-2xl">
                              {getItemIcon(item.itemTemplate.name, item.itemTemplate.type)}
                            </span>
                          </div>
                          <p className="text-xs text-gray-600 dark:text-gray-400 mb-3">
                            {item.itemTemplate.description}
                          </p>
                          {item.itemTemplate.happinessBoost > 0 && (
                            <p className="text-xs text-green-600 dark:text-green-400 mb-3">
                              +{item.itemTemplate.happinessBoost} Happiness
                            </p>
                          )}
                          <button
                            onClick={() => handleItemUse(selectedItem)}
                            className="w-full px-3 py-2 bg-blue-500 text-white rounded-lg text-sm hover:bg-blue-600 transition-colors"
                          >
                            Use Item
                          </button>
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
                <p className="text-xs">Visit the shop to buy items for your pet!</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PetRoom; 