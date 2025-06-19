import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Card, Button } from '../ui';
import GitPrompt from './GitPrompt';
import GitOutput from './GitOutput';
import CommandHistory, { CommandHistoryItem } from './CommandHistory';
import { useToast } from '../../contexts/ToastContext';

interface GitTerminalProps {
  repositoryId?: number;
  scenarioId?: string;
  currentStep?: number;
  onStepComplete?: (step: number) => void;
  onCommandExecute?: (command: string, result: any) => void;
  className?: string;
}

interface GitCommandResult {
  successful: boolean;
  exitCode: number;
  output: string;
  errorOutput: string;
  commitHash?: string;
}

interface RepositoryState {
  repositoryId: number;
  currentBranch: string;
  commits: any[];
  branches: any[];
}

const GitTerminal: React.FC<GitTerminalProps> = ({
  repositoryId,
  scenarioId,
  currentStep = 0,
  onStepComplete,
  onCommandExecute,
  className = ''
}) => {
  const [commandHistory, setCommandHistory] = useState<CommandHistoryItem[]>([]);
  const [currentOutput, setCurrentOutput] = useState<string>('');
  const [currentError, setCurrentError] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);
  const [repositoryState, setRepositoryState] = useState<RepositoryState | null>(null);
  const [showHistory, setShowHistory] = useState(false);
  const [terminalSize, setTerminalSize] = useState<'normal' | 'expanded'>('normal');
  
  const terminalRef = useRef<HTMLDivElement>(null);
  const { showSuccess, showError } = useToast();

  // Initialize repository state
  useEffect(() => {
    if (repositoryId) {
      fetchRepositoryState();
    }
  }, [repositoryId]);

  const fetchRepositoryState = async () => {
    if (!repositoryId) return;
    
    try {
      const response = await fetch(`/api/v1/git/repository/${repositoryId}/state`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const state = await response.json();
        setRepositoryState(state);
      }
    } catch (error) {
      console.error('Failed to fetch repository state:', error);
    }
  };

  const executeCommand = async (command: string): Promise<GitCommandResult> => {
    if (!repositoryId) {
      throw new Error('No repository selected');
    }

    const response = await fetch(`/api/v1/git/repository/${repositoryId}/execute`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        command,
        scenarioId,
        stepNumber: currentStep
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  };

  const handleCommandSubmit = useCallback(async (command: string) => {
    if (!command.trim() || isLoading) return;

    setIsLoading(true);
    setCurrentOutput('');
    setCurrentError('');

    const startTime = Date.now();
    const commandId = `cmd-${Date.now()}-${Math.random()}`;

    try {
      // Add command to history immediately
      const historyItem: CommandHistoryItem = {
        id: commandId,
        command,
        output: '',
        timestamp: new Date(),
        successful: false,
        executionTime: 0
      };

      setCommandHistory(prev => [...prev, historyItem]);

      // Execute command
      const result = await executeCommand(command);
      const executionTime = Date.now() - startTime;

      // Update output
      setCurrentOutput(result.output);
      setCurrentError(result.errorOutput);

      // Update command history with results
      setCommandHistory(prev => 
        prev.map(item => 
          item.id === commandId 
            ? {
                ...item,
                output: result.output,
                error: result.errorOutput,
                successful: result.successful,
                executionTime
              }
            : item
        )
      );

      // Refresh repository state
      await fetchRepositoryState();

      // Show feedback
      if (result.successful) {
        showSuccess(`Command executed successfully in ${executionTime}ms`);
      } else {
        showError('Command failed to execute');
      }

      // Notify parent component
      if (onCommandExecute) {
        onCommandExecute(command, result);
      }

      // Check if step is complete
      if (scenarioId && currentStep !== undefined && result.successful && onStepComplete) {
        // This would typically be handled by the parent component
        // based on scenario validation logic
        onStepComplete(currentStep + 1);
      }

    } catch (error) {
      const executionTime = Date.now() - startTime;
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      
      setCurrentError(errorMessage);
      showError(`Command execution failed: ${errorMessage}`);

      // Update command history with error
      setCommandHistory(prev => 
        prev.map(item => 
          item.id === commandId 
            ? {
                ...item,
                error: errorMessage,
                successful: false,
                executionTime
              }
            : item
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [repositoryId, scenarioId, currentStep, isLoading, onCommandExecute, showSuccess, showError]);

  const handleCommandSelect = (_command: string) => {
    // This will be handled by the GitPrompt component
    // when user clicks on a command in history
  };

  const clearTerminal = () => {
    setCurrentOutput('');
    setCurrentError('');
    setCommandHistory([]);
    showSuccess('Terminal cleared');
  };

  const exportHistory = () => {
    const historyText = commandHistory
      .map(item => `[${item.timestamp.toISOString()}] ${item.command}\n${item.output}${item.error ? '\nError: ' + item.error : ''}`)
      .join('\n\n');
    
    const blob = new Blob([historyText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `git-terminal-history-${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showSuccess('Command history exported');
  };

  const toggleTerminalSize = () => {
    setTerminalSize(prev => prev === 'normal' ? 'expanded' : 'normal');
  };

  const commandHistoryStrings = commandHistory.map(item => item.command);

  return (
    <div className={`git-terminal ${className}`}>
      <Card variant="elevated" className={`transition-all duration-300 ${terminalSize === 'expanded' ? 'fixed inset-4 z-50' : ''}`}>
        {/* Terminal Header */}
        <div className="flex items-center justify-between p-4 border-b border-border-light dark:border-border-dark">
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              <span className="text-2xl">💻</span>
              <h3 className="text-lg font-semibold text-text-light dark:text-text-dark">
                Git Terminal
              </h3>
            </div>
            {repositoryState && (
              <div className="flex items-center space-x-2 text-sm text-gray-600 dark:text-gray-400">
                <span className="bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 px-2 py-1 rounded">
                  {repositoryState.currentBranch}
                </span>
                <span className="text-gray-400">•</span>
                <span>{repositoryState.commits.length} commits</span>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowHistory(!showHistory)}
              className="text-xs"
            >
              {showHistory ? '📋 Hide History' : '📜 Show History'}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={exportHistory}
              className="text-xs"
              disabled={commandHistory.length === 0}
            >
              💾 Export
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={clearTerminal}
              className="text-xs"
            >
              🧹 Clear
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={toggleTerminalSize}
              className="text-xs"
            >
              {terminalSize === 'expanded' ? '📱 Normal' : '📺 Expand'}
            </Button>
            {terminalSize === 'expanded' && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setTerminalSize('normal')}
                className="text-xs"
              >
                ✕
              </Button>
            )}
          </div>
        </div>

        {/* Terminal Content */}
        <div className={`flex ${terminalSize === 'expanded' ? 'h-full' : 'h-96'}`}>
          {/* Main Terminal Area */}
          <div className={`flex-1 flex flex-col ${showHistory ? 'border-r border-border-light dark:border-border-dark' : ''}`}>
            {/* Output Area */}
            <div className="flex-1 p-4 overflow-auto bg-gray-50 dark:bg-gray-900" ref={terminalRef}>
              {(currentOutput || currentError) ? (
                <GitOutput 
                  output={currentOutput}
                  error={currentError}
                  isError={!currentOutput && !!currentError}
                />
              ) : (
                <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
                  <div className="text-4xl mb-4">🚀</div>
                  <p className="text-lg mb-2">Welcome to Git Terminal</p>
                  <p className="text-sm">
                    {repositoryId 
                      ? 'Start typing git commands below' 
                      : 'Create or select a repository to begin'
                    }
                  </p>
                  {scenarioId && (
                    <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
                      <p className="text-sm text-blue-800 dark:text-blue-200">
                        📚 Scenario Mode: Follow the instructions to complete Step {currentStep + 1}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Command Input */}
            <div className="p-4 border-t border-border-light dark:border-border-dark">
              <GitPrompt
                onCommandSubmit={handleCommandSubmit}
                commandHistory={commandHistoryStrings}
                isLoading={isLoading}
                currentDirectory="~/project"
                currentBranch={repositoryState?.currentBranch || 'main'}
                placeholder={repositoryId ? 'Enter git command...' : 'Create a repository first...'}
              />
            </div>
          </div>

          {/* Command History Sidebar */}
          {showHistory && (
            <div className="w-80 border-l border-border-light dark:border-border-dark">
              <CommandHistory
                history={commandHistory}
                onCommandSelect={handleCommandSelect}
                className="h-full"
              />
            </div>
          )}
        </div>

        {/* Status Bar */}
        <div className="px-4 py-2 bg-gray-100 dark:bg-gray-800 border-t border-border-light dark:border-border-dark">
          <div className="flex items-center justify-between text-xs text-gray-600 dark:text-gray-400">
            <div className="flex items-center space-x-4">
              <span>Commands: {commandHistory.length}</span>
              <span>
                Success Rate: {
                  commandHistory.length > 0 
                    ? Math.round((commandHistory.filter(h => h.successful).length / commandHistory.length) * 100)
                    : 0
                }%
              </span>
              {isLoading && (
                <span className="flex items-center space-x-1">
                  <div className="animate-spin rounded-full h-3 w-3 border-b border-blue-400"></div>
                  <span>Executing...</span>
                </span>
              )}
            </div>
            <div className="flex items-center space-x-2">
              <span>Press ↑↓ for history, Tab for autocomplete</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default GitTerminal; 