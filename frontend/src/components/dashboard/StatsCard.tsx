import React, { useState, useEffect } from 'react';
import { Card, CardContent } from '../ui';

interface StatsCardProps {
  title: string;
  value: number;
  previousValue?: number;
  icon: string;
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'error';
  format?: 'number' | 'percentage' | 'currency';
  suffix?: string;
  trend?: 'up' | 'down' | 'neutral';
  className?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  previousValue,
  icon,
  color,
  format = 'number',
  suffix = '',
  trend,
  className = ''
}) => {
  const [displayValue, setDisplayValue] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    setIsAnimating(true);
    const duration = 1500;
    const steps = 60;
    const increment = value / steps;
    let current = 0;
    
    const timer = setInterval(() => {
      current += increment;
      if (current >= value) {
        setDisplayValue(value);
        clearInterval(timer);
        setIsAnimating(false);
      } else {
        setDisplayValue(Math.floor(current));
      }
    }, duration / steps);

    return () => clearInterval(timer);
  }, [value]);

  const formatValue = (val: number) => {
    switch (format) {
      case 'percentage':
        return `${val}%`;
      case 'currency':
        return `$${val.toLocaleString()}`;
      default:
        return val.toLocaleString();
    }
  };

  const getTrendChange = () => {
    if (!previousValue || previousValue === 0) return null;
    const change = ((value - previousValue) / previousValue) * 100;
    return Math.abs(change).toFixed(1);
  };

  const colorClasses = {
    primary: {
      bg: 'bg-primary-50 dark:bg-primary-950',
      icon: 'bg-primary-500',
      text: 'text-primary-600 dark:text-primary-400'
    },
    secondary: {
      bg: 'bg-secondary-50 dark:bg-secondary-950',
      icon: 'bg-secondary-500',
      text: 'text-secondary-600 dark:text-secondary-400'
    },
    success: {
      bg: 'bg-green-50 dark:bg-green-950',
      icon: 'bg-green-500',
      text: 'text-green-600 dark:text-green-400'
    },
    warning: {
      bg: 'bg-yellow-50 dark:bg-yellow-950',
      icon: 'bg-yellow-500',
      text: 'text-yellow-600 dark:text-yellow-400'
    },
    error: {
      bg: 'bg-red-50 dark:bg-red-950',
      icon: 'bg-red-500',
      text: 'text-red-600 dark:text-red-400'
    }
  };

  const trendColors = {
    up: 'text-green-600 dark:text-green-400',
    down: 'text-red-600 dark:text-red-400',
    neutral: 'text-gray-600 dark:text-gray-400'
  };

  const trendIcons = {
    up: '↗️',
    down: '↘️',
    neutral: '➡️'
  };

  return (
    <Card variant="elevated" className={`${colorClasses[color].bg} ${className}`}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">
              {title}
            </p>
            <div className="flex items-baseline space-x-2">
              <p className={`text-3xl font-bold ${colorClasses[color].text} ${isAnimating ? 'animate-pulse' : ''}`}>
                {formatValue(displayValue)}{suffix}
              </p>
              {getTrendChange() && (
                <div className={`flex items-center text-sm ${trend ? trendColors[trend] : 'text-gray-600 dark:text-gray-400'}`}>
                  <span className="mr-1">{trend ? trendIcons[trend] : ''}</span>
                  <span>{getTrendChange()}%</span>
                </div>
              )}
            </div>
          </div>
          
          <div className={`w-12 h-12 ${colorClasses[color].icon} rounded-xl flex items-center justify-center text-white text-xl`}>
            {icon}
          </div>
        </div>
        
        {/* Progress bar for percentage values */}
        {format === 'percentage' && (
          <div className="mt-4">
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
              <div
                className={`h-2 ${colorClasses[color].icon} rounded-full transition-all duration-1500 ease-out`}
                style={{ width: `${Math.min(displayValue, 100)}%` }}
              />
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default StatsCard; 