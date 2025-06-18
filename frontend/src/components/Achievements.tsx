import React, { useState, useEffect } from 'react';
import { achievementApi, UserAchievement } from '../utils/api';

const Achievements: React.FC = () => {
  const [userAchievements, setUserAchievements] = useState<UserAchievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadAchievements();
  }, []);

  const loadAchievements = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await achievementApi.getUserAchievements();
      if (response.success && response.data) {
        setUserAchievements(response.data);
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to load achievements');
    } finally {
      setLoading(false);
    }
  };

  const getAchievementIcon = (type: string) => {
    switch (type) {
      case 'PET_CREATION': return '🐾';
      case 'PET_FEEDING': return '🍽️';
      case 'ITEM_PURCHASE': return '🛒';
      case 'PROBLEM_SOLVING': return '🧩';
      case 'AI_INTERACTION': return '🤖';
      default: return '🏆';
    }
  };

  const getProgressPercentage = (current: number, required: number) => {
    return Math.min((current / required) * 100, 100);
  };

  const getProgressColor = (isCompleted: boolean, percentage: number) => {
    if (isCompleted) return 'bg-green-500';
    if (percentage >= 75) return 'bg-blue-500';
    if (percentage >= 50) return 'bg-yellow-500';
    if (percentage >= 25) return 'bg-orange-500';
    return 'bg-gray-400';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading achievements...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="text-center text-red-600">
          <p>❌ {error}</p>
        </div>
      </div>
    );
  }

  const completedAchievements = userAchievements.filter(ua => ua.isCompleted);
  const inProgressAchievements = userAchievements.filter(ua => !ua.isCompleted);

  return (
    <div className="bg-white rounded-lg shadow-md">
      <div className="p-4 border-b border-gray-200">
        <h2 className="text-xl font-bold text-gray-800">Achievements</h2>
        <p className="text-sm text-gray-600">
          {completedAchievements.length} of {userAchievements.length} completed
        </p>
      </div>

      <div className="p-4">
        {userAchievements.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <p>🏆 No achievements available</p>
            <p className="text-sm">Start using CodeMate to unlock achievements!</p>
          </div>
        ) : (
          <div className="space-y-4">
            {/* Completed Achievements */}
            {completedAchievements.length > 0 && (
              <div>
                <h3 className="text-lg font-semibold text-green-600 mb-3 flex items-center">
                  <span className="mr-2">🎉</span>
                  Completed ({completedAchievements.length})
                </h3>
                <div className="grid gap-3">
                  {completedAchievements.map((userAchievement) => (
                    <div
                      key={userAchievement.id}
                      className="border border-green-200 bg-green-50 rounded-lg p-4"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="text-3xl">
                          {getAchievementIcon(userAchievement.achievement.type)}
                        </div>
                        <div className="flex-1">
                          <h4 className="font-semibold text-gray-800">
                            {userAchievement.achievement.name}
                          </h4>
                          <p className="text-sm text-gray-600">
                            {userAchievement.achievement.description}
                          </p>
                          <div className="flex items-center space-x-2 mt-2">
                            <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                              ✅ Completed
                            </span>
                            {userAchievement.completedAt && (
                              <span className="text-xs text-gray-500">
                                {new Date(userAchievement.completedAt).toLocaleDateString()}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="text-2xl text-green-500">
                          ✨
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* In Progress Achievements */}
            {inProgressAchievements.length > 0 && (
              <div>
                <h3 className="text-lg font-semibold text-blue-600 mb-3 flex items-center">
                  <span className="mr-2">🎯</span>
                  In Progress ({inProgressAchievements.length})
                </h3>
                <div className="grid gap-3">
                  {inProgressAchievements.map((userAchievement) => {
                    const progress = getProgressPercentage(
                      userAchievement.currentProgress,
                      userAchievement.achievement.requiredValue
                    );
                    
                    return (
                      <div
                        key={userAchievement.id}
                        className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors"
                      >
                        <div className="flex items-center space-x-3">
                          <div className="text-3xl opacity-60">
                            {getAchievementIcon(userAchievement.achievement.type)}
                          </div>
                          <div className="flex-1">
                            <h4 className="font-semibold text-gray-800">
                              {userAchievement.achievement.name}
                            </h4>
                            <p className="text-sm text-gray-600">
                              {userAchievement.achievement.description}
                            </p>
                            
                            {/* Progress Bar */}
                            <div className="mt-3">
                              <div className="flex justify-between items-center mb-1">
                                <span className="text-xs text-gray-600">Progress</span>
                                <span className="text-xs font-medium text-gray-800">
                                  {userAchievement.currentProgress} / {userAchievement.achievement.requiredValue}
                                </span>
                              </div>
                              <div className="w-full bg-gray-200 rounded-full h-2">
                                <div
                                  className={`h-2 rounded-full transition-all duration-300 ${getProgressColor(false, progress)}`}
                                  style={{ width: `${progress}%` }}
                                ></div>
                              </div>
                              <div className="text-xs text-gray-500 mt-1">
                                {Math.round(progress)}% complete
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Achievements; 