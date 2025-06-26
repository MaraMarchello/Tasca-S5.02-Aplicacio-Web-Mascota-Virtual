import React, { useState, useEffect } from 'react';
import Card from '../ui/Card';
import Button from '../ui/Button';

interface GitStats {
  completedScenarios: number;
  totalPointsEarned: number;
  averageCommandsPerScenario: number;
  learningStreak: number;
  categoryProgress: Record<string, CategoryProgress>;
  levelProgress: Record<string, LevelProgress>;
  recentActivity: RecentActivity[];
  completedAchievements: number;
  totalAchievements: number;
}

interface CategoryProgress {
  category: string;
  totalScenarios: number;
  completedScenarios: number;
  completionPercentage: number;
}

interface LevelProgress {
  level: string;
  totalScenarios: number;
  completedScenarios: number;
  completionPercentage: number;
}

interface RecentActivity {
  scenarioTitle: string;
  completedAt: string;
  pointsEarned: number;
  category: string;
  level: string;
}

interface GitProgressCardProps {
  userId?: number;
  className?: string;
}

export const GitProgressCard: React.FC<GitProgressCardProps> = ({ userId, className = "" }) => {
  const [gitStats, setGitStats] = useState<GitStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchGitStats();
  }, [userId]);

  const fetchGitStats = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      
      // Check if user is authenticated
      if (!token) {
        setError('Please log in to view Git stats');
        return;
      }

      const response = await fetch('/api/v1/git/dashboard', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.status === 401 || response.status === 403) {
        setError('Please log in to view Git stats');
        return;
      }

      if (!response.ok) {
        throw new Error(`Failed to fetch Git stats: ${response.status}`);
      }

      const data = await response.json();
      
      // Transform backend data to match frontend interface
      const transformedStats: GitStats = {
        completedScenarios: data.completedScenarios || 0,
        totalPointsEarned: data.totalPointsEarned || 0,
        averageCommandsPerScenario: data.userStats?.averageCommandsPerScenario || 0,
        learningStreak: data.learningStreak || 0,
        categoryProgress: data.categoryProgress || {},
        levelProgress: data.levelProgress || {},
        recentActivity: data.recentActivity || [],
        completedAchievements: data.completedAchievements || 0,
        totalAchievements: data.totalAchievements || 0,
      };
      
      setGitStats(transformedStats);
    } catch (err) {
      console.error('Error fetching Git stats:', err);
      setError(err instanceof Error ? err.message : 'Failed to load Git stats');
    } finally {
      setLoading(false);
    }
  };

  const getCategoryIcon = (category: string) => {
    const icons: Record<string, string> = {
      'BASICS': '📚',
      'BRANCHING': '🌿',
      'MERGING': '🔀',
      'CONFLICTS': '⚔️',
      'COLLABORATION': '🤝',
      'ADVANCED': '🎯'
    };
    return icons[category] || '📝';
  };

  const getLevelColor = (level: string) => {
    const colors: Record<string, string> = {
      'BEGINNER': 'text-green-600',
      'INTERMEDIATE': 'text-yellow-600',
      'ADVANCED': 'text-red-600'
    };
    return colors[level] || 'text-gray-600';
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <Card className={`${className} animate-pulse`}>
        <div className="p-6">
          <div className="h-6 bg-gray-200 rounded mb-4"></div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded w-3/4"></div>
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
          </div>
        </div>
      </Card>
    );
  }

  if (error) {
    const isAuthError = error.includes('log in');
    return (
      <Card className={`${className} ${isAuthError ? 'border-blue-200' : 'border-red-200'}`}>
        <div className="p-6 text-center">
          <div className={`mb-2 ${isAuthError ? 'text-blue-500' : 'text-red-500'}`}>
            {isAuthError ? '🔐' : '⚠️'}
          </div>
          <h3 className="text-lg font-semibold text-gray-800 mb-2">
            {isAuthError ? 'Authentication Required' : 'Error Loading Git Stats'}
          </h3>
          <p className={`mb-4 ${isAuthError ? 'text-blue-600' : 'text-red-600'}`}>{error}</p>
          {isAuthError ? (
            <Button 
              onClick={() => window.location.href = '/login'} 
              variant="primary" 
              size="sm"
            >
              Go to Login
            </Button>
          ) : (
            <Button onClick={fetchGitStats} variant="outline" size="sm">
              Try Again
            </Button>
          )}
        </div>
      </Card>
    );
  }

  if (!gitStats) {
    return (
      <Card className={className}>
        <div className="p-6 text-center text-gray-500">
          <div className="text-4xl mb-2">🎯</div>
          <p>Start your Git learning journey!</p>
          <Button 
            onClick={() => window.location.href = '/git-coach'} 
            className="mt-4"
            size="sm"
          >
            Begin Learning
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <div className="p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
            <span className="text-2xl">🚀</span>
            Git Learning Progress
          </h3>
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <span className="flex items-center gap-1">
              🔥 {gitStats.learningStreak} day streak
            </span>
          </div>
        </div>

        {/* Key Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="text-center p-3 bg-blue-50 rounded-lg">
            <div className="text-2xl font-bold text-blue-600">{gitStats.completedScenarios}</div>
            <div className="text-xs text-blue-600">Scenarios</div>
          </div>
          <div className="text-center p-3 bg-green-50 rounded-lg">
            <div className="text-2xl font-bold text-green-600">{gitStats.totalPointsEarned}</div>
            <div className="text-xs text-green-600">Points</div>
          </div>
          <div className="text-center p-3 bg-purple-50 rounded-lg">
            <div className="text-2xl font-bold text-purple-600">
              {Math.round(gitStats.averageCommandsPerScenario)}
            </div>
            <div className="text-xs text-purple-600">Avg Commands</div>
          </div>
          <div className="text-center p-3 bg-yellow-50 rounded-lg">
            <div className="text-2xl font-bold text-yellow-600">
              {gitStats.completedAchievements}/{gitStats.totalAchievements}
            </div>
            <div className="text-xs text-yellow-600">Achievements</div>
          </div>
        </div>

        {/* Category Progress */}
        <div className="mb-6">
          <h4 className="font-medium text-gray-700 mb-3">Progress by Category</h4>
          <div className="space-y-2">
            {Object.values(gitStats.categoryProgress).map((category) => (
              <div key={category.category} className="flex items-center gap-3">
                <span className="text-lg">{getCategoryIcon(category.category)}</span>
                <div className="flex-1">
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-sm font-medium capitalize">
                      {category.category.toLowerCase()}
                    </span>
                    <span className="text-xs text-gray-500">
                      {category.completedScenarios}/{category.totalScenarios}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div 
                      className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${category.completionPercentage}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Level Progress */}
        <div className="mb-6">
          <h4 className="font-medium text-gray-700 mb-3">Progress by Level</h4>
          <div className="flex gap-4">
            {Object.values(gitStats.levelProgress).map((level) => (
              <div key={level.level} className="flex-1 text-center">
                <div className={`text-sm font-medium ${getLevelColor(level.level)} mb-1`}>
                  {level.level}
                </div>
                <div className="text-xs text-gray-500 mb-2">
                  {level.completedScenarios}/{level.totalScenarios}
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div 
                    className={`h-2 rounded-full transition-all duration-300 ${
                      level.level === 'BEGINNER' ? 'bg-green-500' :
                      level.level === 'INTERMEDIATE' ? 'bg-yellow-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${level.completionPercentage}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Activity */}
        {gitStats.recentActivity.length > 0 && (
          <div className="mb-4">
            <h4 className="font-medium text-gray-700 mb-3">Recent Activity</h4>
            <div className="space-y-2 max-h-32 overflow-y-auto">
              {gitStats.recentActivity.slice(0, 3).map((activity, index) => (
                <div key={index} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-sm">{getCategoryIcon(activity.category)}</span>
                    <div>
                      <div className="text-sm font-medium truncate max-w-32">
                        {activity.scenarioTitle}
                      </div>
                      <div className="text-xs text-gray-500">
                        {formatDate(activity.completedAt)}
                      </div>
                    </div>
                  </div>
                  <div className="text-sm font-medium text-green-600">
                    +{activity.pointsEarned}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Action Button */}
        <div className="pt-4 border-t">
          <Button 
            onClick={() => window.location.href = '/git-coach'}
            className="w-full"
            variant="outline"
          >
            Continue Learning Git 🚀
          </Button>
        </div>
      </div>
    </Card>
  );
}; 