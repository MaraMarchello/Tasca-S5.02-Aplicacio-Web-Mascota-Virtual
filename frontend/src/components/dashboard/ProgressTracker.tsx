import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../ui';

interface ProgressItem {
  id: string;
  title: string;
  description: string;
  current: number;
  target: number;
  unit: string;
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'error';
  icon: string;
  milestones?: number[];
}

interface ProgressTrackerProps {
  progressItems: ProgressItem[];
  className?: string;
}

const ProgressTracker: React.FC<ProgressTrackerProps> = ({
  progressItems,
  className = ''
}) => {
  const colorClasses = {
    primary: {
      bg: 'bg-primary-500',
      light: 'bg-primary-100 dark:bg-primary-900',
      text: 'text-primary-600 dark:text-primary-400'
    },
    secondary: {
      bg: 'bg-secondary-500',
      light: 'bg-secondary-100 dark:bg-secondary-900',
      text: 'text-secondary-600 dark:text-secondary-400'
    },
    success: {
      bg: 'bg-green-500',
      light: 'bg-green-100 dark:bg-green-900',
      text: 'text-green-600 dark:text-green-400'
    },
    warning: {
      bg: 'bg-yellow-500',
      light: 'bg-yellow-100 dark:bg-yellow-900',
      text: 'text-yellow-600 dark:text-yellow-400'
    },
    error: {
      bg: 'bg-red-500',
      light: 'bg-red-100 dark:bg-red-900',
      text: 'text-red-600 dark:text-red-400'
    }
  };

  const ProgressBar: React.FC<{ item: ProgressItem }> = ({ item }) => {
    const percentage = Math.min((item.current / item.target) * 100, 100);
    const colors = colorClasses[item.color];
    
    return (
      <div className={`p-4 rounded-lg ${colors.light}`}>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-3">
            <div className={`w-10 h-10 ${colors.bg} rounded-lg flex items-center justify-center text-white`}>
              {item.icon}
            </div>
            <div>
              <h4 className="font-semibold text-gray-800 dark:text-white">{item.title}</h4>
              <p className="text-sm text-gray-600 dark:text-gray-400">{item.description}</p>
            </div>
          </div>
          <div className="text-right">
            <div className={`text-lg font-bold ${colors.text}`}>
              {item.current.toLocaleString()}/{item.target.toLocaleString()}
            </div>
            <div className="text-sm text-gray-500 dark:text-gray-400">{item.unit}</div>
          </div>
        </div>

        {/* Progress bar with milestones */}
        <div className="relative">
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
            <div
              className={`h-3 ${colors.bg} rounded-full transition-all duration-1000 ease-out relative overflow-hidden`}
              style={{ width: `${percentage}%` }}
            >
              {/* Animated shine effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse" />
            </div>
          </div>

          {/* Milestone markers */}
          {item.milestones && (
            <div className="absolute top-0 w-full h-3">
              {item.milestones.map((milestone, index) => {
                const milestonePercentage = (milestone / item.target) * 100;
                const isReached = item.current >= milestone;
                
                return (
                  <div
                    key={index}
                    className="absolute top-0 transform -translate-x-1/2"
                    style={{ left: `${milestonePercentage}%` }}
                  >
                    <div
                      className={`w-3 h-3 rounded-full border-2 ${
                        isReached
                          ? `${colors.bg} border-white`
                          : 'bg-gray-300 dark:bg-gray-600 border-gray-400 dark:border-gray-500'
                      }`}
                    />
                    <div className="absolute top-4 left-1/2 transform -translate-x-1/2 text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                      {milestone.toLocaleString()}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Progress percentage */}
        <div className="flex justify-between items-center mt-3">
          <div className="text-sm text-gray-600 dark:text-gray-400">
            Progress: {percentage.toFixed(1)}%
          </div>
          {percentage >= 100 ? (
            <div className="flex items-center text-green-600 dark:text-green-400 text-sm font-semibold">
              <span className="mr-1">🎉</span>
              Complete!
            </div>
          ) : (
            <div className="text-sm text-gray-500 dark:text-gray-400">
              {(item.target - item.current).toLocaleString()} remaining
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <Card variant="elevated" className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <span>📊</span>
          <span>Progress Tracking</span>
        </CardTitle>
      </CardHeader>
      
      <CardContent>
        <div className="space-y-4">
          {progressItems.map(item => (
            <ProgressBar key={item.id} item={item} />
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export default ProgressTracker; 