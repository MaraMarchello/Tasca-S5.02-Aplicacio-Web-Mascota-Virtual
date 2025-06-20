import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi, getAuthToken } from '../utils/api';
import Layout from '../components/layout/Layout';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, Button } from '../components/ui';
import { StatsCard, AchievementShowcase, ProgressTracker, GitProgressCard } from '../components/dashboard';
import { useToast } from '../contexts/ToastContext';

interface User {
  id: number;
  name: string;
  email: string;
  authorities: string[];
}

const Dashboard: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const { showSuccess, showInfo } = useToast();

  useEffect(() => {
    const fetchUserData = async () => {
      const token = getAuthToken();
      if (!token) {
        navigate('/login');
        return;
      }

      try {
        const userData = await userApi.getCurrentUser();
        setUser(userData);
        showInfo(`Welcome back, ${userData.name}!`);
      } catch (error) {
        console.error('Error fetching user data:', error);
        navigate('/login');
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, [navigate, showInfo]);

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="text-center">
            <div className="w-16 h-16 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-2xl flex items-center justify-center mx-auto mb-4 animate-bounce-gentle">
              <span className="text-white font-bold text-2xl">C</span>
            </div>
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500 mx-auto mb-4"></div>
            <p className="text-gray-600 dark:text-gray-400">Loading your dashboard...</p>
          </div>
        </div>
      </Layout>
    );
  }

  if (!user) {
    return null; // Will redirect to login
  }

  const handleNavigateToPet = () => {
    navigate('/pet');
    showSuccess('Navigating to your pet!');
  };

  const handleNavigateToAchievements = () => {
    navigate('/pet?tab=achievements');
    showSuccess('Viewing your achievements!');
  };

  const handleNavigateToShop = () => {
    navigate('/pet?tab=shop');
    showSuccess('Welcome to the shop!');
  };

  const handleNavigateToAdmin = () => {
    navigate('/admin');
  };

  const isAdmin = user.authorities?.includes('ADMIN');

  // Mock data for enhanced dashboard components
  const mockStats = [
    { title: 'Total Points', value: 2450, previousValue: 2100, icon: '💰', color: 'primary' as const, trend: 'up' as const },
    { title: 'Achievements', value: 12, previousValue: 10, icon: '🏆', color: 'secondary' as const, trend: 'up' as const },
    { title: 'Pet Happiness', value: 85, icon: '😊', color: 'success' as const, format: 'percentage' as const },
    { title: 'Study Streak', value: 7, icon: '🔥', color: 'warning' as const, suffix: ' days' }
  ];

  const mockAchievements = [
    {
      id: '1',
      name: 'First Steps',
      description: 'Complete your first Java program',
      icon: '👶',
      category: 'learning',
      progress: 1,
      maxProgress: 1,
      unlocked: true,
      unlockedAt: '2024-01-15',
      rarity: 'common' as const,
      points: 50
    },
    {
      id: '2',
      name: 'Loop Master',
      description: 'Write 10 different loop implementations',
      icon: '🔄',
      category: 'coding',
      progress: 7,
      maxProgress: 10,
      unlocked: false,
      rarity: 'rare' as const,
      points: 150
    },
    {
      id: '3',
      name: 'Pet Whisperer',
      description: 'Keep your pet happy for 30 days',
      icon: '🐾',
      category: 'pet',
      progress: 15,
      maxProgress: 30,
      unlocked: false,
      rarity: 'epic' as const,
      points: 300
    },
    {
      id: '4',
      name: 'Java Guru',
      description: 'Master all Java fundamentals',
      icon: '☕',
      category: 'learning',
      progress: 3,
      maxProgress: 20,
      unlocked: false,
      rarity: 'legendary' as const,
      points: 1000
    }
  ];

  const mockProgress = [
    {
      id: '1',
      title: 'Java Fundamentals',
      description: 'Master the basics of Java programming',
      current: 15,
      target: 25,
      unit: 'lessons',
      color: 'primary' as const,
      icon: '📚',
      milestones: [5, 10, 15, 20]
    },
    {
      id: '2',
      title: 'Coding Challenges',
      description: 'Complete programming exercises',
      current: 32,
      target: 50,
      unit: 'challenges',
      color: 'secondary' as const,
      icon: '💪',
      milestones: [10, 25, 40]
    },
    {
      id: '3',
      title: 'Pet Care Points',
      description: 'Points spent on pet care',
      current: 850,
      target: 1000,
      unit: 'points',
      color: 'success' as const,
      icon: '❤️',
      milestones: [250, 500, 750]
    }
  ];

  return (
    <Layout user={user}>
      <div className="space-y-8">
        {/* Welcome Section */}
        <div className="text-center">
          <h1 className="text-4xl font-bold text-text-light dark:text-text-dark mb-4">
            Welcome back, {user.name}! 👋
          </h1>
          <p className="text-xl text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
            Your gamified Java learning companion. Track your progress, care for your pet, and master programming skills.
          </p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {mockStats.map((stat, index) => (
            <StatsCard key={index} {...stat} />
          ))}
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card variant="elevated" hover clickable onClick={handleNavigateToPet}>
            <CardHeader>
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-secondary-400 to-secondary-600 rounded-xl flex items-center justify-center">
                  <span className="text-2xl">🐾</span>
                </div>
                <div>
                  <CardTitle>My Pet</CardTitle>
                  <CardDescription>Check on your virtual companion</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Feed, play with, and customize your CodeMate pet. Earn points by completing coding challenges!
              </p>
            </CardContent>
          </Card>

          <Card variant="elevated" hover clickable onClick={handleNavigateToAchievements}>
            <CardHeader>
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-primary-400 to-primary-600 rounded-xl flex items-center justify-center">
                  <span className="text-2xl">🏆</span>
                </div>
                <div>
                  <CardTitle>Achievements</CardTitle>
                  <CardDescription>Track your progress</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Unlock badges and achievements as you master Java concepts and complete challenges.
              </p>
            </CardContent>
          </Card>

          <Card variant="elevated" hover clickable onClick={handleNavigateToShop}>
            <CardHeader>
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-xl flex items-center justify-center">
                  <span className="text-2xl">💰</span>
                </div>
                <div>
                  <CardTitle>Points & Shop</CardTitle>
                  <CardDescription>Spend your earned points</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Use points to buy items for your pet, unlock new features, and customize your experience.
              </p>
            </CardContent>
          </Card>

          <Card variant="elevated" hover clickable onClick={() => navigate('/git-coach')}>
            <CardHeader>
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-green-400 to-blue-500 rounded-xl flex items-center justify-center">
                  <span className="text-2xl">🚀</span>
                </div>
                <div>
                  <CardTitle>Git Coach</CardTitle>
                  <CardDescription>Learn Git interactively</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Master Git commands through interactive scenarios and visualizations.
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Progress Tracking */}
        <ProgressTracker progressItems={mockProgress} />

        {/* Git Learning Progress */}
        <GitProgressCard userId={user.id} />

        {/* Achievement Showcase */}
        <AchievementShowcase achievements={mockAchievements} />

        {/* User Profile Card */}
        <Card variant="outlined">
          <CardHeader>
            <CardTitle>Your Profile</CardTitle>
            <CardDescription>Account information and learning statistics</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h4 className="font-medium text-text-light dark:text-text-dark mb-2">Account Details</h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Name:</span>
                    <span className="font-medium text-text-light dark:text-text-dark">{user.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Email:</span>
                    <span className="font-medium text-text-light dark:text-text-dark">{user.email}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Role:</span>
                    <span className={`font-medium ${isAdmin ? 'text-purple-600 dark:text-purple-400' : 'text-text-light dark:text-text-dark'}`}>
                      {isAdmin ? 'Administrator' : 'Student'}
                    </span>
                  </div>
                </div>
              </div>
              <div>
                <h4 className="font-medium text-text-light dark:text-text-dark mb-2">Learning Journey</h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Current Level:</span>
                    <span className="font-medium text-secondary-600 dark:text-secondary-400">Beginner</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Next Milestone:</span>
                    <span className="font-medium text-primary-600 dark:text-primary-400">Loop Master</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Learning Streak:</span>
                    <span className="font-medium text-yellow-600 dark:text-yellow-400">7 days 🔥</span>
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Admin Section */}
        {isAdmin && (
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Administrator Panel</CardTitle>
              <CardDescription>Manage users, pets, and system settings</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Access the admin dashboard to manage users, monitor system activity, and configure application settings.
                  </p>
                </div>
                <Button variant="cta" onClick={handleNavigateToAdmin}>
                  Open Admin Panel
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Getting Started */}
        <Card>
          <CardHeader>
            <CardTitle>Getting Started</CardTitle>
            <CardDescription>New to CodeMate? Here's what you can do</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 bg-primary-100 dark:bg-primary-900 rounded-lg flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 dark:text-primary-400 font-bold">1</span>
                </div>
                <div>
                  <h5 className="font-medium text-text-light dark:text-text-dark">Create Your Pet</h5>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Start by creating your virtual companion in the Pet section.
                  </p>
                </div>
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 bg-secondary-100 dark:bg-secondary-900 rounded-lg flex items-center justify-center flex-shrink-0">
                  <span className="text-secondary-600 dark:text-secondary-400 font-bold">2</span>
                </div>
                <div>
                  <h5 className="font-medium text-text-light dark:text-text-dark">Earn Points</h5>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Complete Java challenges and exercises to earn points.
                  </p>
                </div>
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 bg-yellow-100 dark:bg-yellow-900 rounded-lg flex items-center justify-center flex-shrink-0">
                  <span className="text-yellow-600 dark:text-yellow-400 font-bold">3</span>
                </div>
                <div>
                  <h5 className="font-medium text-text-light dark:text-text-dark">Care for Pet</h5>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Use points to feed and care for your pet to keep it happy.
                  </p>
                </div>
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center flex-shrink-0">
                  <span className="text-purple-600 dark:text-purple-400 font-bold">4</span>
                </div>
                <div>
                  <h5 className="font-medium text-text-light dark:text-text-dark">Track Progress</h5>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Monitor your learning progress and unlock achievements.
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
};

export default Dashboard; 