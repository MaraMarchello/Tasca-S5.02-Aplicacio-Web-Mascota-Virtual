import React from 'react';
import Layout from '../components/layout/Layout';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui';

const GitCoach: React.FC = () => {
  return (
    <Layout>
      <div className="space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-text-light dark:text-text-dark mb-4">
            🐙 Git Coach
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
            Learn Git commands and best practices with personalized guidance and interactive tutorials.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Interactive Git Tutorials</CardTitle>
              <CardDescription>
                Learn Git step by step with hands-on exercises
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <div className="w-16 h-16 bg-primary-100 dark:bg-primary-900 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <span className="text-2xl">📚</span>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Coming Soon</p>
              </div>
            </CardContent>
          </Card>

          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Git Visualization</CardTitle>
              <CardDescription>
                Visualize Git branches, commits, and merges
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <div className="w-16 h-16 bg-secondary-100 dark:bg-secondary-900 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <span className="text-2xl">🌳</span>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Coming Soon</p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </Layout>
  );
};

export default GitCoach; 