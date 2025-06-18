import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../ui';

interface Achievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  progress: number;
  maxProgress: number;
  unlocked: boolean;
  unlockedAt?: string;
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
  points: number;
}

interface AchievementShowcaseProps {
  achievements: Achievement[];
  className?: string;
}

const AchievementShowcase: React.FC<AchievementShowcaseProps> = ({
  achievements,
  className = ''
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  const categories = ['all', ...Array.from(new Set(achievements.map(a => a.category)))];
  
  const filteredAchievements = selectedCategory === 'all' 
    ? achievements 
    : achievements.filter(a => a.category === selectedCategory);

  const unlockedCount = achievements.filter(a => a.unlocked).length;
  const totalPoints = achievements.filter(a => a.unlocked).reduce((sum, a) => sum + a.points, 0);

  const rarityColors = {
    common: {
      bg: 'bg-gray-100 dark:bg-gray-800',
      border: 'border-gray-300 dark:border-gray-600',
      text: 'text-gray-700 dark:text-gray-300',
      glow: ''
    },
    rare: {
      bg: 'bg-blue-50 dark:bg-blue-950',
      border: 'border-blue-300 dark:border-blue-600',
      text: 'text-blue-700 dark:text-blue-300',
      glow: 'shadow-blue-500/20'
    },
    epic: {
      bg: 'bg-purple-50 dark:bg-purple-950',
      border: 'border-purple-300 dark:border-purple-600',
      text: 'text-purple-700 dark:text-purple-300',
      glow: 'shadow-purple-500/20'
    },
    legendary: {
      bg: 'bg-yellow-50 dark:bg-yellow-950',
      border: 'border-yellow-300 dark:border-yellow-600',
      text: 'text-yellow-700 dark:text-yellow-300',
      glow: 'shadow-yellow-500/20'
    }
  };

  const AchievementCard: React.FC<{ achievement: Achievement }> = ({ achievement }) => {
    const rarity = rarityColors[achievement.rarity];
    const progressPercentage = Math.min((achievement.progress / achievement.maxProgress) * 100, 100);
    
    return (
      <div
        className={`relative p-4 rounded-xl border-2 transition-all duration-300 hover:scale-105 ${
          achievement.unlocked 
            ? `${rarity.bg} ${rarity.border} ${rarity.glow} shadow-lg` 
            : 'bg-gray-50 dark:bg-gray-900 border-gray-200 dark:border-gray-700 opacity-60'
        }`}
      >
        {/* Rarity indicator */}
        <div className={`absolute top-2 right-2 px-2 py-1 rounded-full text-xs font-bold ${
          achievement.unlocked ? rarity.text : 'text-gray-500 dark:text-gray-400'
        }`}>
          {achievement.rarity.toUpperCase()}
        </div>

        {/* Achievement icon */}
        <div className="text-center mb-3">
          <div className={`w-16 h-16 mx-auto rounded-full flex items-center justify-center text-2xl ${
            achievement.unlocked 
              ? 'bg-white dark:bg-gray-800 shadow-md' 
              : 'bg-gray-200 dark:bg-gray-700'
          }`}>
            {achievement.unlocked ? achievement.icon : '🔒'}
          </div>
        </div>

        {/* Achievement info */}
        <div className="text-center">
          <h4 className={`font-bold text-sm mb-1 ${
            achievement.unlocked 
              ? 'text-gray-800 dark:text-white' 
              : 'text-gray-500 dark:text-gray-400'
          }`}>
            {achievement.name}
          </h4>
          <p className={`text-xs mb-3 ${
            achievement.unlocked 
              ? 'text-gray-600 dark:text-gray-300' 
              : 'text-gray-400 dark:text-gray-500'
          }`}>
            {achievement.description}
          </p>

          {/* Progress bar */}
          {!achievement.unlocked && (
            <div className="mb-2">
              <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
                <span>{achievement.progress}</span>
                <span>{achievement.maxProgress}</span>
              </div>
              <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                <div
                  className="h-2 bg-primary-500 rounded-full transition-all duration-500"
                  style={{ width: `${progressPercentage}%` }}
                />
              </div>
            </div>
          )}

          {/* Points */}
          <div className={`text-xs font-semibold ${
            achievement.unlocked 
              ? 'text-yellow-600 dark:text-yellow-400' 
              : 'text-gray-400 dark:text-gray-500'
          }`}>
            {achievement.points} points
          </div>

          {/* Unlock date */}
          {achievement.unlocked && achievement.unlockedAt && (
            <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Unlocked {new Date(achievement.unlockedAt).toLocaleDateString()}
            </div>
          )}
        </div>

        {/* Shine effect for unlocked achievements */}
        {achievement.unlocked && (
          <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-transparent via-white/10 to-transparent -skew-x-12 animate-shine" />
        )}
      </div>
    );
  };

  return (
    <Card variant="elevated" className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center space-x-2">
            <span>🏆</span>
            <span>Achievements</span>
          </CardTitle>
          <div className="text-sm text-gray-600 dark:text-gray-400">
            {unlockedCount}/{achievements.length} unlocked • {totalPoints} points
          </div>
        </div>
        
        {/* Category filter */}
        <div className="flex flex-wrap gap-2 mt-4">
          {categories.map(category => (
            <button
              key={category}
              onClick={() => setSelectedCategory(category)}
              className={`px-3 py-1 rounded-full text-sm font-medium transition-all ${
                selectedCategory === category
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
              }`}
            >
              {category.charAt(0).toUpperCase() + category.slice(1)}
            </button>
          ))}
        </div>
      </CardHeader>
      
      <CardContent>
        {filteredAchievements.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filteredAchievements.map(achievement => (
              <AchievementCard key={achievement.id} achievement={achievement} />
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">
            <div className="text-4xl mb-2">🎯</div>
            <p>No achievements in this category yet</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default AchievementShowcase; 