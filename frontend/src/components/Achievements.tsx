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

  const getAchievementIcon = (code: string) => {
    switch (code) {
      // Pet-related achievements
      case 'PET_OWNER': return '🐾';
      case 'PET_FEEDER': return '🍽️';
      case 'PET_NAMER': return '✏️';
      case 'PET_INTERACTOR': return '❤️';
      case 'PET_CARETAKER': return '🏠';
      case 'PET_HAPPINESS_MASTER': return '😊';
      case 'DAILY_FEEDER': return '📅';
      case 'PET_FASHIONISTA': return '👗';
      case 'FOOD_CONNOISSEUR': return '🍴';

      // Shopping achievements
      case 'SHOPPER': return '🛒';
      case 'FIRST_PURCHASE': return '💳';
      case 'ITEM_COLLECTOR': return '📦';
      case 'BIG_SPENDER': return '💰';
      case 'BARGAIN_HUNTER': return '🏷️';
      case 'INVENTORY_MANAGER': return '📋';
      case 'PREMIUM_BUYER': return '💎';

      // Problem solving achievements
      case 'PROBLEM_SOLVER': return '🧩';
      case 'DEBUGGER': return '🐛';
      case 'ERROR_HUNTER': return '🎯';
      case 'STACK_TRACE_EXPERT': return '📊';
      case 'PROBLEM_CRUSHER': return '💪';

      // AI achievements
      case 'AI_USER': return '🤖';
      case 'AI_CURIOUS': return '🤔';
      case 'AI_CONVERSATIONALIST': return '💬';
      case 'AI_POWER_USER': return '⚡';
      case 'CODE_HELPER': return '💻';

      // Git achievements - Quick Start
      case 'GIT_VISITOR': return '👀';
      case 'GIT_TERMINAL_USER': return '💻';
      case 'GIT_VISUALIZATION_USER': return '📊';
      case 'GIT_FIRST_COMMAND': return '⌨️';
      case 'GIT_SCENARIO_STARTER': return '🎬';
      case 'GIT_CURIOUS': return '🤔';
      case 'GIT_STUDENT': return '🎒';
      case 'GIT_PRACTITIONER': return '🔧';
      
      // Git achievements - Learning Progress
      case 'GIT_FIRST_STEPS': return '👶';
      case 'GIT_BEGINNER': return '🌱';
      case 'GIT_BASICS_MASTER': return '📚';
      case 'GIT_BRANCHING_PRO': return '🌳';
      case 'GIT_MERGE_MASTER': return '🔀';
      case 'GIT_CONFLICT_RESOLVER': return '⚔️';
      case 'GIT_ADVANCED_USER': return '🚀';
      case 'GIT_WORKFLOW_EXPERT': return '👨‍💼';
      case 'GIT_GURU': return '🧙‍♂️';
      case 'GIT_EFFICIENT': return '⚡';
      case 'GIT_PERFECTIONIST': return '💯';
      case 'GIT_SPEED_DEMON': return '🏃‍♂️';
      case 'GIT_DAILY_LEARNER': return '📖';
      case 'GIT_DEDICATED': return '🎖️';
      case 'GIT_PERSISTENT': return '🏋️‍♂️';
      case 'GIT_HELP_SEEKER': return '🆘';
      case 'GIT_EXPERIMENTER': return '🔬';
      case 'GIT_COMMAND_MASTER': return '⌨️';

      // Quick start achievements
      case 'FIRST_LOGIN': return '🚪';
      case 'PROFILE_EXPLORER': return '👤';
      case 'DASHBOARD_VISITOR': return '📊';

      // Learning achievements
      case 'QUICK_LEARNER': return '⚡';
      case 'HELP_SEEKER': return '🙋‍♂️';
      case 'KNOWLEDGE_SEEKER': return '🎓';
      case 'CONSISTENT_USER': return '📈';
      case 'WEEK_WARRIOR': return '🗓️';
      case 'MONTH_MASTER': return '📅';

      // Social & engagement achievements
      case 'EXPLORER': return '🗺️';
      case 'FEATURE_TESTER': return '🧪';
      case 'POWER_USER': return '⚡';
      case 'POINT_COLLECTOR': return '💰';
      case 'ACHIEVEMENT_HUNTER': return '🏹';
      case 'COMPLETIONIST': return '✅';

      // Time-based achievements
      case 'EARLY_BIRD': return '🌅';
      case 'NIGHT_OWL': return '🦉';
      case 'WEEKEND_WARRIOR': return '🎮';
      case 'SPEED_USER': return '💨';

      // Milestone achievements
      case 'HUNDRED_CLUB': return '💯';
      case 'VETERAN_USER': return '🏅';
      case 'DEDICATION_MASTER': return '👑';

      default: return '🏆';
    }
  };

  const getAchievementCategory = (code: string) => {
    if (code.startsWith('PET_')) return 'Pet Care';
    if (code.startsWith('GIT_')) return 'Git Learning';
    if (code.includes('SHOP') || code.includes('PURCHASE') || code.includes('SPEND')) return 'Shopping';
    if (code.includes('AI_') || code.includes('CODE_HELPER')) return 'AI Assistant';
    if (code.includes('ERROR') || code.includes('PROBLEM') || code.includes('DEBUG')) return 'Problem Solving';
    if (code.includes('LOGIN') || code.includes('VISITOR') || code.includes('EXPLORER')) return 'Getting Started';
    if (code.includes('LEARN') || code.includes('KNOWLEDGE') || code.includes('HELP')) return 'Learning';
    if (code.includes('TIME') || code.includes('EARLY') || code.includes('NIGHT') || code.includes('WEEKEND')) return 'Time-based';
    if (code.includes('POINT') || code.includes('ACHIEVEMENT') || code.includes('HUNDRED') || code.includes('VETERAN')) return 'Milestones';
    return 'General';
  };

  const getRarityColor = (pointsReward: number) => {
    if (pointsReward >= 1000) return { bg: 'bg-yellow-50', border: 'border-yellow-300', text: 'text-yellow-700', badge: 'bg-yellow-100' };
    if (pointsReward >= 500) return { bg: 'bg-purple-50', border: 'border-purple-300', text: 'text-purple-700', badge: 'bg-purple-100' };
    if (pointsReward >= 200) return { bg: 'bg-blue-50', border: 'border-blue-300', text: 'text-blue-700', badge: 'bg-blue-100' };
    return { bg: 'bg-gray-50', border: 'border-gray-300', text: 'text-gray-700', badge: 'bg-gray-100' };
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

  const completedAchievements = userAchievements.filter(ua => ua.completed);
  const inProgressAchievements = userAchievements.filter(ua => !ua.completed);

  // Group achievements by category
  const groupedAchievements = userAchievements.reduce((groups, achievement) => {
    const category = getAchievementCategory(achievement.achievement.code);
    if (!groups[category]) {
      groups[category] = [];
    }
    groups[category].push(achievement);
    return groups;
  }, {} as Record<string, typeof userAchievements>);

  const totalPoints = completedAchievements.reduce((sum, ua) => sum + (ua.achievement.pointsReward || 0), 0);

  return (
    <div className="bg-white rounded-lg shadow-md">
      <div className="p-4 border-b border-gray-200">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-xl font-bold text-gray-800">Achievements</h2>
            <p className="text-sm text-gray-600">
              {completedAchievements.length} of {userAchievements.length} completed
            </p>
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-yellow-600">{totalPoints}</div>
            <div className="text-xs text-gray-500">Points Earned</div>
          </div>
        </div>
      </div>

      <div className="p-4">
        {userAchievements.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <p>🏆 No achievements available</p>
            <p className="text-sm">Start using CodeMate to unlock achievements!</p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Quick Stats */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div className="text-center p-3 bg-green-50 rounded-lg">
                <div className="text-2xl font-bold text-green-600">{completedAchievements.length}</div>
                <div className="text-xs text-green-700">Completed</div>
              </div>
              <div className="text-center p-3 bg-blue-50 rounded-lg">
                <div className="text-2xl font-bold text-blue-600">{inProgressAchievements.length}</div>
                <div className="text-xs text-blue-700">In Progress</div>
              </div>
              <div className="text-center p-3 bg-yellow-50 rounded-lg">
                <div className="text-2xl font-bold text-yellow-600">{totalPoints}</div>
                <div className="text-xs text-yellow-700">Points</div>
              </div>
              <div className="text-center p-3 bg-purple-50 rounded-lg">
                <div className="text-2xl font-bold text-purple-600">{Object.keys(groupedAchievements).length}</div>
                <div className="text-xs text-purple-700">Categories</div>
              </div>
            </div>

            {/* Achievements by Category */}
            {Object.entries(groupedAchievements).map(([category, achievements]) => (
              <div key={category} className="space-y-3">
                <h3 className="text-lg font-semibold text-gray-800 flex items-center">
                  <span className="mr-2">📂</span>
                  {category} ({achievements.filter(a => a.completed).length}/{achievements.length})
                </h3>
                <div className="grid gap-3">
                  {achievements.map((userAchievement) => {
                    const rarity = getRarityColor(userAchievement.achievement.pointsReward);
                    const progress = getProgressPercentage(
                      userAchievement.currentProgress,
                      userAchievement.achievement.targetValue
                    );
                    
                    return (
                      <div
                        key={userAchievement.id}
                        className={`border rounded-lg p-4 transition-all duration-200 hover:shadow-md ${
                          userAchievement.completed 
                            ? `${rarity.bg} ${rarity.border} border-2` 
                            : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <div className="flex items-center space-x-3">
                          <div className={`text-3xl ${userAchievement.completed ? '' : 'opacity-60'}`}>
                            {getAchievementIcon(userAchievement.achievement.code)}
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center space-x-2">
                              <h4 className="font-semibold text-gray-800">
                                {userAchievement.achievement.name}
                              </h4>
                              <span className={`text-xs px-2 py-1 rounded ${rarity.badge} ${rarity.text}`}>
                                {userAchievement.achievement.pointsReward} pts
                              </span>
                            </div>
                            <p className="text-sm text-gray-600 mt-1">
                              {userAchievement.achievement.description}
                            </p>
                            
                            {userAchievement.completed ? (
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
                            ) : (
                              <div className="mt-3">
                                <div className="flex justify-between items-center mb-1">
                                  <span className="text-xs text-gray-600">Progress</span>
                                  <span className="text-xs font-medium text-gray-800">
                                    {userAchievement.currentProgress} / {userAchievement.achievement.targetValue}
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
                            )}
                          </div>
                          {userAchievement.completed && (
                            <div className="text-2xl text-green-500">
                              ✨
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}

            {/* Legacy sections for backward compatibility */}
            {false && completedAchievements.length > 0 && (
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
                          {getAchievementIcon(userAchievement.achievement.code)}
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
                      userAchievement.achievement.targetValue
                    );
                    
                    return (
                      <div
                        key={userAchievement.id}
                        className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors"
                      >
                        <div className="flex items-center space-x-3">
                          <div className="text-3xl opacity-60">
                            {getAchievementIcon(userAchievement.achievement.code)}
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
                                  {userAchievement.currentProgress} / {userAchievement.achievement.targetValue}
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