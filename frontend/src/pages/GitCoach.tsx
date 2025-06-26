import React, { useState, useEffect } from 'react';
import Layout from '../components/layout/Layout';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, Button } from '../components/ui';
import { GitTerminal, GitTerminalDemo, GitVisualizationDemo } from '../components/git';
import { GitScenario, UserProgress } from '../types/git';

const GitCoach: React.FC = () => {
  const [scenarios, setScenarios] = useState<GitScenario[]>([]);
  const [userProgress, setUserProgress] = useState<UserProgress[]>([]);
  const [selectedScenario, setSelectedScenario] = useState<GitScenario | null>(null);
  const [currentRepository, setCurrentRepository] = useState<number | null>(null);
  const [currentProgress, setCurrentProgress] = useState<UserProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<'scenarios' | 'terminal' | 'demo' | 'visualization'>('scenarios');
  const [trackedViews, setTrackedViews] = useState<Set<string>>(new Set());

  useEffect(() => {
    fetchScenarios();
    fetchUserProgress();
  }, []);

  const fetchScenarios = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        console.log('No token found, skipping scenarios fetch for demo mode');
        setLoading(false);
        return;
      }

      const response = await fetch('/api/v1/git/scenarios', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setScenarios(data || []);
      } else if (response.status === 401 || response.status === 403) {
        console.log('User not authenticated, showing demo mode');
      } else {
        console.error('Failed to fetch scenarios:', response.status);
        setError(`Failed to load scenarios: ${response.status}`);
      }
    } catch (error) {
      console.error('Failed to fetch scenarios:', error);
      setError('Failed to connect to server');
    }
  };

  const fetchUserProgress = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        console.log('No token found, skipping user progress fetch for demo mode');
        setLoading(false);
        return;
      }

      const response = await fetch('/api/v1/git/progress', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setUserProgress(data || []);
      } else if (response.status === 401 || response.status === 403) {
        console.log('User not authenticated, showing demo mode');
      } else {
        console.error('Failed to fetch user progress:', response.status);
      }
    } catch (error) {
      console.error('Failed to fetch user progress:', error);
    } finally {
      setLoading(false);
    }
  };

  const startScenario = async (scenario: GitScenario) => {
    try {
      // Start the scenario
      const progressResponse = await fetch(`/api/v1/git/scenarios/${scenario.scenarioId}/start`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (progressResponse.ok) {
        const progress = await progressResponse.json();
        setCurrentProgress(progress);
      }

      // Create a virtual repository for this scenario
      const repoResponse = await fetch('/api/v1/git/repository/create', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          scenarioId: scenario.scenarioId,
          repositoryName: `${scenario.scenarioId}-practice`
        })
      });

      if (repoResponse.ok) {
        const repository = await repoResponse.json();
        setCurrentRepository(repository.id);
      }

      setSelectedScenario(scenario);
      setView('terminal');
      
    } catch (error) {
      console.error('Failed to start scenario:', error);
    }
  };

  const getLevelColor = (level: GitScenario['level']): string => {
    switch (level) {
      case 'BEGINNER': return 'text-green-600 bg-green-100 dark:bg-green-900 dark:text-green-300';
      case 'INTERMEDIATE': return 'text-blue-600 bg-blue-100 dark:bg-blue-900 dark:text-blue-300';
      case 'ADVANCED': return 'text-orange-600 bg-orange-100 dark:bg-orange-900 dark:text-orange-300';
      case 'EXPERT': return 'text-red-600 bg-red-100 dark:bg-red-900 dark:text-red-300';
      default: return 'text-gray-600 bg-gray-100 dark:bg-gray-900 dark:text-gray-300';
    }
  };

  const getCategoryIcon = (category: GitScenario['category']): string => {
    switch (category) {
      case 'BASICS': return '📁';
      case 'BRANCHING': return '🌿';
      case 'MERGING': return '🔀';
      case 'CONFLICTS': return '⚔️';
      case 'COLLABORATION': return '👥';
      case 'ADVANCED_WORKFLOWS': return '⚡';
      default: return '💻';
    }
  };

  const getProgressForScenario = (scenarioId: string): UserProgress | undefined => {
    return userProgress?.find(p => p.scenario?.scenarioId === scenarioId);
  };

  const getProgressIcon = (progress?: UserProgress): string => {
    if (!progress) return '⭕';
    switch (progress.status) {
      case 'COMPLETED': return '✅';
      case 'IN_PROGRESS': return '🔄';
      case 'FAILED': return '❌';
      case 'ABANDONED': return '⏸️';
      default: return '⭕';
    }
  };

  const handleCommandExecute = (command: string, result: any) => {
    // Handle command execution feedback
    console.log('Command executed:', command, result);
  };

  const handleStepComplete = (step: number) => {
    // Handle scenario step completion
    console.log('Step completed:', step);
    fetchUserProgress();
  };

  const trackViewUsage = async (viewType: string) => {
    const token = localStorage.getItem('token');
    if (!token || trackedViews.has(viewType)) return;

    try {
      let endpoint = '';
      switch (viewType) {
        case 'terminal':
        case 'demo':
          endpoint = '/api/v1/achievements/track/git-terminal';
          break;
        case 'visualization':
          endpoint = '/api/v1/achievements/track/git-visualization';
          break;
        default:
          return;
      }

      await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      setTrackedViews(prev => new Set([...prev, viewType]));
    } catch (error) {
      console.error('Failed to track view usage:', error);
    }
  };

  const handleViewChange = (newView: string) => {
    setView(newView as any);
    trackViewUsage(newView);
  };

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          <span className="ml-3 text-gray-600">Loading Git Coach...</span>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {view === 'scenarios' ? (
          <>
            {/* Header */}
            <div className="text-center">
              <h1 className="text-3xl font-bold text-text-light dark:text-text-dark mb-4">
                🐙 Git Coach
              </h1>
              <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto mb-4">
                Learn Git commands and best practices with personalized guidance and interactive tutorials.
              </p>
              <div className="flex space-x-4 mb-6">
                <Button
                  variant="secondary"
                  onClick={() => handleViewChange('demo')}
                >
                  🚀 Terminal
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => handleViewChange('visualization')}
                >
                  🌳 Visualization
                </Button>
              </div>
              
              {!localStorage.getItem('token') && (
                <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg border border-blue-200 dark:border-blue-800 mb-6">
                  <h4 className="font-medium text-blue-800 dark:text-blue-200 mb-2">🎯 Demo Mode</h4>
                  <p className="text-sm text-blue-700 dark:text-blue-300">
                    You're viewing in demo mode. Login to access full scenarios and track your progress!
                  </p>
                </div>
              )}

              {error && (
                <div className="bg-red-50 dark:bg-red-900/20 p-4 rounded-lg border border-red-200 dark:border-red-800 mb-6">
                  <h4 className="font-medium text-red-800 dark:text-red-200 mb-2">⚠️ Connection Error</h4>
                  <p className="text-sm text-red-700 dark:text-red-300 mb-3">
                    {error}
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setError(null);
                      fetchScenarios();
                      fetchUserProgress();
                    }}
                  >
                    🔄 Retry
                  </Button>
                </div>
              )}
            </div>

            {/* Stats Overview */}
            {localStorage.getItem('token') && (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card variant="elevated">
                  <CardContent className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                        {userProgress?.filter(p => p.status === 'COMPLETED').length || 0}
                      </div>
                      <div className="text-sm text-gray-600 dark:text-gray-400">Completed</div>
                    </div>
                  </CardContent>
                </Card>
                
                <Card variant="elevated">
                  <CardContent className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                        {userProgress?.filter(p => p.status === 'IN_PROGRESS').length || 0}
                      </div>
                      <div className="text-sm text-gray-600 dark:text-gray-400">In Progress</div>
                    </div>
                  </CardContent>
                </Card>
                
                <Card variant="elevated">
                  <CardContent className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">
                        {userProgress?.reduce((sum, p) => sum + (p.pointsEarned || 0), 0) || 0}
                      </div>
                      <div className="text-sm text-gray-600 dark:text-gray-400">Points Earned</div>
                    </div>
                  </CardContent>
                </Card>
                
                <Card variant="elevated">
                  <CardContent className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                        {scenarios.length > 0 && userProgress ? Math.round(((userProgress.filter(p => p.status === 'COMPLETED').length) / scenarios.length) * 100) : 0}%
                      </div>
                      <div className="text-sm text-gray-600 dark:text-gray-400">Completion</div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* Scenarios Grid */}
            {scenarios.length === 0 && !error ? (
              <div className="text-center py-12">
                <div className="text-6xl mb-4">🎯</div>
                <h3 className="text-xl font-semibold mb-2">No Scenarios Available</h3>
                <p className="text-gray-600 dark:text-gray-400 mb-4">
                  {localStorage.getItem('token') 
                    ? 'Scenarios are being loaded or the server is starting up.'
                    : 'Login to access interactive Git learning scenarios.'
                  }
                </p>
                <div className="flex justify-center space-x-4">
                  <Button
                    variant="outline"
                    onClick={() => handleViewChange('demo')}
                  >
                    🚀 Try Demo Terminal
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => handleViewChange('visualization')}
                  >
                    🌳 View Visualization
                  </Button>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {scenarios.map((scenario) => {
                const progress = getProgressForScenario(scenario.scenarioId);
                return (
                  <Card key={scenario.id} variant="elevated" className="hover:shadow-lg transition-shadow">
                    <CardHeader>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-2xl">{getCategoryIcon(scenario.category)}</span>
                        <div className="flex items-center space-x-2">
                          <span className="text-lg">{getProgressIcon(progress)}</span>
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${getLevelColor(scenario.level)}`}>
                            {scenario.level}
                          </span>
                        </div>
                      </div>
                      <CardTitle className="text-lg">{scenario.title}</CardTitle>
                      <CardDescription>{scenario.description}</CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-3">
                        <div className="flex items-center justify-between text-sm text-gray-600 dark:text-gray-400">
                          <span>⏱️ {scenario.estimatedMinutes} min</span>
                          <span>🏆 {scenario.pointsReward} points</span>
                        </div>
                        
                        {progress && progress.status === 'IN_PROGRESS' && (
                          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                            <div 
                              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                              style={{ width: `${(progress.currentStep / progress.totalSteps) * 100}%` }}
                            ></div>
                          </div>
                        )}
                        
                        <Button
                          variant={progress?.status === 'COMPLETED' ? 'outline' : 'primary'}
                          className="w-full"
                          onClick={() => startScenario(scenario)}
                        >
                          {progress?.status === 'COMPLETED' 
                            ? '🔄 Practice Again' 
                            : progress?.status === 'IN_PROGRESS'
                            ? '▶️ Continue'
                            : '🚀 Start Learning'
                          }
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
              </div>
            )}
          </>
        ) : view === 'terminal' ? (
          <>
            {/* Terminal View */}
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-4">
                <Button
                  variant="outline"
                  onClick={() => handleViewChange('scenarios')}
                >
                  ← Back to Scenarios
                </Button>
                {selectedScenario && (
                  <div className="flex items-center space-x-2">
                    <span className="text-2xl">{getCategoryIcon(selectedScenario.category)}</span>
                    <div>
                      <h2 className="text-xl font-semibold">{selectedScenario.title}</h2>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        {selectedScenario.description}
                      </p>
                    </div>
                  </div>
                )}
              </div>
              
              {currentProgress && (
                <div className="flex items-center space-x-4">
                  <div className="text-sm text-gray-600 dark:text-gray-400">
                    Step {currentProgress.currentStep} of {currentProgress.totalSteps}
                  </div>
                  <div className="w-32 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                    <div 
                      className="bg-green-600 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${(currentProgress.currentStep / currentProgress.totalSteps) * 100}%` }}
                    ></div>
                  </div>
                </div>
              )}
            </div>

            <GitTerminal
              repositoryId={currentRepository || undefined}
              scenarioId={selectedScenario?.scenarioId}
              currentStep={currentProgress?.currentStep}
              onStepComplete={handleStepComplete}
              onCommandExecute={handleCommandExecute}
              className="min-h-[600px]"
            />
          </>
        ) : view === 'demo' ? (
          <>
            {/* Demo View */}
            <div className="flex items-center justify-between mb-4">
              <Button
                variant="outline"
                onClick={() => setView('scenarios')}
              >
                ← Back to Scenarios
              </Button>
              <h2 className="text-xl font-semibold">Terminal Interface</h2>
            </div>
            <GitTerminalDemo />
          </>
        ) : view === 'visualization' ? (
          <>
            {/* Visualization Demo View */}
            <div className="flex items-center justify-between mb-4">
              <Button
                variant="outline"
                onClick={() => setView('scenarios')}
              >
                ← Back to Scenarios
              </Button>
              <h2 className="text-xl font-semibold">Git Visualization</h2>
            </div>
            <GitVisualizationDemo />
          </>
        ) : null}
       </div>
     </Layout>
   );
 };

export default GitCoach; 