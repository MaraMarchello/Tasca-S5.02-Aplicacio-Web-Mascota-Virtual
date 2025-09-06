import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, Button } from '../ui';
import GitPrompt from './GitPrompt';
import GitOutput from './GitOutput';
import CommandHistory, { CommandHistoryItem } from './CommandHistory';
import demoGitService from '../../services/demoGitService';
import { GitRepository } from '../../types/git';

const GitTerminalDemo: React.FC = () => {
  const [activeDemo, setActiveDemo] = useState<'terminal' | 'prompt' | 'output' | 'history'>('terminal');
  const [demoHistory, setDemoHistory] = useState<CommandHistoryItem[]>([
    {
      id: '1',
      command: 'git status',
      output: 'On branch main\nYour branch is up to date with \'origin/main\'.\n\nnothing to commit, working tree clean',
      timestamp: new Date(Date.now() - 60000),
      successful: true,
      executionTime: 45
    },
    {
      id: '2',
      command: 'git add .',
      output: '',
      timestamp: new Date(Date.now() - 30000),
      successful: true,
      executionTime: 23
    },
    {
      id: '3',
      command: 'git commit -m "Add new feature"',
      output: '[main 7a8b9c2] Add new feature\n 3 files changed, 45 insertions(+), 2 deletions(-)',
      timestamp: new Date(Date.now() - 15000),
      successful: true,
      executionTime: 156
    },
    {
      id: '4',
      command: 'git push origin feature-branch',
      output: '',
      error: 'error: src refspec feature-branch does not match any\nerror: failed to push some refs to \'origin\'',
      timestamp: new Date(),
      successful: false,
      executionTime: 89
    }
  ]);

  // Demo terminal state
  const [currentOutput, setCurrentOutput] = useState<string>('');
  const [currentError, setCurrentError] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  
  // Real Git execution state
  const [demoRepository, setDemoRepository] = useState<GitRepository | null>(null);
  const [isRealGitEnabled, setIsRealGitEnabled] = useState<boolean>(false);
  const [initializationError, setInitializationError] = useState<string>('');

  // Initialize demo repository on component mount
  useEffect(() => {
    initializeDemoRepository();
  }, []);

  const initializeDemoRepository = async () => {
    try {
      setIsLoading(true);
      setInitializationError('');
      
      // Check if real Git is enabled and create demo repository
      const isEnabled = await demoGitService.isRealGitEnabled();
      setIsRealGitEnabled(isEnabled);
      
      if (isEnabled) {
        const repository = await demoGitService.getOrCreateDemoRepository();
        setDemoRepository(repository);
        console.log('Demo repository initialized:', repository);
      } else {
        console.warn('Real Git execution not enabled - falling back to simulation mode');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to initialize demo repository';
      setInitializationError(errorMessage);
      console.error('Demo initialization error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDemoCommand = async (command: string) => {
    if (isLoading) return;

    setIsLoading(true);
    setCurrentOutput('');
    setCurrentError('');

    const startTime = Date.now();
    const commandId = Date.now().toString();

    try {
      let result;
      let successful = true;
      let output = '';
      let errorOutput = '';

      if (isRealGitEnabled && demoRepository) {
        // Execute real Git command
        console.log('Executing real Git command:', command);
        const executeResponse = await demoGitService.executeCommand(command);
        result = executeResponse.result;
        successful = result.successful;
        output = result.output;
        errorOutput = result.errorOutput;
      } else {
        // Fall back to simulation mode
        console.log('Using simulation mode for command:', command);
        const mockOutput = generateMockOutput(command);
        successful = Math.random() > 0.15; // 85% success rate
        output = successful ? mockOutput : '';
        errorOutput = successful ? '' : 'Command failed: Invalid command or repository state';
        
        // Simulate network delay for consistency
        await new Promise(resolve => setTimeout(resolve, Math.random() * 500 + 200));
      }

      const executionTime = Date.now() - startTime;

      const newCommand: CommandHistoryItem = {
        id: commandId,
        command,
        output,
        error: errorOutput,
        timestamp: new Date(),
        successful,
        executionTime
      };

      setDemoHistory(prev => [...prev, newCommand]);
      setCurrentOutput(output);
      setCurrentError(errorOutput);

    } catch (error) {
      const executionTime = Date.now() - startTime;
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      
      console.error('Command execution failed:', error);

      const newCommand: CommandHistoryItem = {
        id: commandId,
        command,
        output: '',
        error: errorMessage,
        timestamp: new Date(),
        successful: false,
        executionTime
      };

      setDemoHistory(prev => [...prev, newCommand]);
      setCurrentOutput('');
      setCurrentError(errorMessage);

      // If authentication error, suggest refresh
      if (errorMessage.includes('expired') || errorMessage.includes('401')) {
        setCurrentError(errorMessage + ' Try refreshing the page and logging in again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const generateMockOutput = (command: string): string => {
    const cmd = command.toLowerCase().trim();
    
    if (cmd === 'git status') {
      return `On branch main
Your branch is ahead of 'origin/main' by 2 commits.
  (use "git push" to publish your local commits)

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
	new file:   src/components/git/GitTerminal.tsx
	new file:   src/components/git/GitPrompt.tsx
	modified:   src/pages/GitCoach.tsx

Untracked files:
  (use "git add <file>..." to include in what will be committed)
	src/types/git.ts`;
    }
    
    if (cmd.startsWith('git add')) {
      return '';
    }
    
    if (cmd.startsWith('git commit')) {
      const hash = Math.random().toString(36).substring(2, 9);
      return `[main ${hash}] ${cmd.includes('-m') ? 'Demo commit message' : 'Demo commit'}
 ${Math.floor(Math.random() * 5) + 1} files changed, ${Math.floor(Math.random() * 50) + 1} insertions(+), ${Math.floor(Math.random() * 10)} deletions(-)`;
    }
    
    if (cmd === 'git log' || cmd === 'git log --oneline') {
      return `7a8b9c2 Add new feature
3d4e5f6 Fix bug in authentication
1a2b3c4 Initial commit`;
    }
    
    if (cmd === 'git branch' || cmd === 'git branch -a') {
      return `* main
  feature/user-auth
  hotfix/security-patch
  remotes/origin/main
  remotes/origin/develop`;
    }
    
    if (cmd.startsWith('git checkout') || cmd.startsWith('git switch')) {
      const branchName = cmd.includes('feature') ? 'feature/demo-branch' : 'main';
      return `Switched to branch '${branchName}'`;
    }
    
    if (cmd === 'git diff') {
      return `diff --git a/src/demo.js b/src/demo.js
index 1a2b3c4..5d6e7f8 100644
--- a/src/demo.js
+++ b/src/demo.js
@@ -1,3 +1,6 @@
 function demo() {
+  // This is a demo change
   console.log('Hello World');
+  console.log('Demo terminal working!');
 }`;
    }
    
    if (cmd.startsWith('git push')) {
      return `Enumerating objects: 5, done.
Counting objects: 100% (5/5), done.
Delta compression using up to 8 threads
Compressing objects: 100% (3/3), done.
Writing objects: 100% (3/3), 324 bytes | 324.00 KiB/s, done.
Total 3 (delta 1), reused 0 (delta 0), pack-reused 0
To https://github.com/user/demo-repo.git
   1a2b3c4..7a8b9c2  main -> main`;
    }
    
    return `Demo output for: ${command}
This is a simulated response showing how the terminal would work.
Try different Git commands to see various outputs!`;
  };

  const clearTerminal = () => {
    setCurrentOutput('');
    setCurrentError('');
    setDemoHistory([]);
  };

  const resetDemoRepository = async () => {
    if (!isRealGitEnabled) return;
    
    try {
      setIsLoading(true);
      setCurrentOutput('');
      setCurrentError('');
      
      console.log('Resetting demo repository...');
      const newRepository = await demoGitService.resetDemoRepository();
      setDemoRepository(newRepository);
      setDemoHistory([]);
      
      setCurrentOutput('Demo repository has been reset. You can start fresh with git init.');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to reset repository';
      console.error('Error resetting demo repository:', error);
      setCurrentError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const exportHistory = () => {
    const historyText = demoHistory
      .map(item => `[${item.timestamp.toISOString()}] ${item.command}\n${item.output}${item.error ? '\nError: ' + item.error : ''}`)
      .join('\n\n');
    
    const blob = new Blob([historyText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `git-terminal-demo-history-${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const demoOutput = `On branch main
Your branch is ahead of 'origin/main' by 2 commits.
  (use "git push" to publish your local commits)

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
	new file:   src/components/git/GitTerminal.tsx
	new file:   src/components/git/GitPrompt.tsx
	modified:   src/pages/GitCoach.tsx

Untracked files:
  (use "git add <file>..." to include in what will be committed)
	src/types/git.ts`;

  return (
    <div className="space-y-6">
      {/* Demo Navigation */}
      <Card variant="elevated">
        <CardHeader>
          <CardTitle>🚀 Git Terminal Components Demo</CardTitle>
          <CardDescription>
            Interactive demonstration of our new Git terminal interface components
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            <Button
              variant={activeDemo === 'terminal' ? 'primary' : 'outline'}
              size="sm"
              onClick={() => setActiveDemo('terminal')}
            >
              🖥️ Full Terminal
            </Button>
            <Button
              variant={activeDemo === 'prompt' ? 'primary' : 'outline'}
              size="sm"
              onClick={() => setActiveDemo('prompt')}
            >
              ⌨️ Command Prompt
            </Button>
            <Button
              variant={activeDemo === 'output' ? 'primary' : 'outline'}
              size="sm"
              onClick={() => setActiveDemo('output')}
            >
              📄 Output Display
            </Button>
            <Button
              variant={activeDemo === 'history' ? 'primary' : 'outline'}
              size="sm"
              onClick={() => setActiveDemo('history')}
            >
              📜 Command History
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Demo Content */}
      {activeDemo === 'terminal' && (
        <div className="space-y-4">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Complete Git Terminal Demo</CardTitle>
              <CardDescription>
                Full terminal interface with command input, output display, and history sidebar - Try typing Git commands!
              </CardDescription>
            </CardHeader>
          </Card>
          
          {/* Demo Terminal */}
          <Card variant="elevated" className="transition-all duration-300">
            {/* Terminal Header */}
            <div className="flex items-center justify-between p-4 border-b border-border-light dark:border-border-dark">
              <div className="flex items-center space-x-3">
                <div className="flex items-center space-x-2">
                  <span className="text-2xl">💻</span>
                  <h3 className="text-lg font-semibold text-text-light dark:text-text-dark">
                    Git Terminal Demo
                  </h3>
                </div>
                <div className="flex items-center space-x-2 text-sm text-gray-600 dark:text-gray-400">
                  <span className="bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 px-2 py-1 rounded">
                    main
                  </span>
                  <span className="text-gray-400">•</span>
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    isRealGitEnabled 
                      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200' 
                      : 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200'
                  }`}>
                    {isRealGitEnabled ? '🔴 Real Git' : '🎭 Simulation'}
                  </span>
                  {demoRepository && (
                    <>
                      <span className="text-gray-400">•</span>
                      <span className="text-xs">ID: {demoRepository.id}</span>
                    </>
                  )}
                </div>
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
                  disabled={demoHistory.length === 0}
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
                {isRealGitEnabled && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={resetDemoRepository}
                    className="text-xs"
                    disabled={isLoading}
                  >
                    🔄 Reset Repo
                  </Button>
                )}
              </div>
            </div>

            {/* Terminal Content */}
            <div className={`flex ${showHistory ? 'h-96' : 'h-80'}`}>
              {/* Main Terminal Area */}
              <div className={`flex-1 flex flex-col ${showHistory ? 'border-r border-border-light dark:border-border-dark' : ''}`}>
                {/* Output Area */}
                <div className="flex-1 p-4 overflow-auto bg-gray-50 dark:bg-gray-900">
                  {(currentOutput || currentError) ? (
                    <GitOutput 
                      output={currentOutput}
                      error={currentError}
                      isError={!currentOutput && !!currentError}
                    />
                  ) : (
                    <div className="text-center text-gray-500 dark:text-gray-400 mt-8">
                      <div className="text-4xl mb-4">🚀</div>
                      <p className="text-lg mb-2">
                        Welcome to Git Terminal Demo
                      </p>
                      <p className="text-sm mb-4">
                        {isRealGitEnabled 
                          ? 'Real Git execution enabled! Commands will be executed in an isolated environment.'
                          : 'Simulation mode active. Commands will return mock responses.'
                        }
                      </p>
                      
                      {initializationError && (
                        <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 rounded-lg border border-red-200 dark:border-red-800">
                          <p className="text-sm text-red-800 dark:text-red-200">
                            ⚠️ {initializationError}
                          </p>
                          <button 
                            onClick={initializeDemoRepository}
                            className="mt-2 text-xs text-red-600 dark:text-red-400 underline"
                          >
                            Try again
                          </button>
                        </div>
                      )}
                      
                      <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
                        <p className="text-sm text-blue-800 dark:text-blue-200">
                          💡 Try commands like: git init, git status, git add ., git commit -m "message", git log
                        </p>
                        {isRealGitEnabled && (
                          <p className="text-xs text-blue-600 dark:text-blue-400 mt-2">
                            🔄 Use "Reset Repo" button to start with a fresh repository
                          </p>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {/* Command Input */}
                <div className="border-t border-border-light dark:border-border-dark">
                  <GitPrompt
                    onCommandSubmit={handleDemoCommand}
                    commandHistory={demoHistory.map(h => h.command)}
                    isLoading={isLoading}
                    currentDirectory="~/demo-project"
                    currentBranch="main"
                    placeholder="Enter git command..."
                  />
                </div>
              </div>

              {/* Command History Sidebar */}
              {showHistory && (
                <div className="w-80 border-l border-border-light dark:border-border-dark">
                  <CommandHistory
                    history={demoHistory}
                    onCommandSelect={handleDemoCommand}
                    className="h-full"
                  />
                </div>
              )}
            </div>

            {/* Status Bar */}
            <div className="px-4 py-2 bg-gray-100 dark:bg-gray-800 border-t border-border-light dark:border-border-dark">
              <div className="flex items-center justify-between text-xs text-gray-600 dark:text-gray-400">
                <div className="flex items-center space-x-4">
                  <span>Commands: {demoHistory.length}</span>
                  <span>
                    Success Rate: {
                      demoHistory.length > 0 
                        ? Math.round((demoHistory.filter(h => h.successful).length / demoHistory.length) * 100)
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
      )}

      {activeDemo === 'prompt' && (
        <div className="space-y-4">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Git Command Prompt</CardTitle>
              <CardDescription>
                Interactive command input with autocomplete, history navigation, and syntax highlighting
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-lg">
                  <h4 className="font-medium mb-2">Features:</h4>
                  <ul className="text-sm space-y-1 text-gray-600 dark:text-gray-400">
                    <li>• Type "git" to see autocomplete suggestions</li>
                    <li>• Use ↑↓ arrows to navigate command history</li>
                    <li>• Tab or Enter to select autocomplete options</li>
                    <li>• Esc to close autocomplete dropdown</li>
                    <li>• Real-time command validation and hints</li>
                  </ul>
                </div>
                <GitPrompt
                  onCommandSubmit={handleDemoCommand}
                  commandHistory={demoHistory.map(h => h.command)}
                  currentBranch="feature/git-terminal"
                  currentDirectory="~/codemate"
                />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {activeDemo === 'output' && (
        <div className="space-y-4">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Git Output Display</CardTitle>
              <CardDescription>
                Syntax-highlighted output with color coding for different types of information
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-lg">
                  <h4 className="font-medium mb-2">Color Coding:</h4>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-red-400 rounded"></div>
                      <span>Errors</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-yellow-400 rounded"></div>
                      <span>Warnings</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-green-400 rounded"></div>
                      <span>Success</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-blue-400 rounded"></div>
                      <span>Info</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-purple-400 rounded"></div>
                      <span>Branches</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-orange-400 rounded"></div>
                      <span>Commits</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="w-3 h-3 bg-cyan-400 rounded"></div>
                      <span>Files</span>
                    </div>
                  </div>
                </div>
                <GitOutput output={demoOutput} />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {activeDemo === 'history' && (
        <div className="space-y-4">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle>Command History</CardTitle>
              <CardDescription>
                Interactive command history with execution details and reusable commands
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-lg mb-4">
                <h4 className="font-medium mb-2">Features:</h4>
                <ul className="text-sm space-y-1 text-gray-600 dark:text-gray-400">
                  <li>• Click any command to reuse it</li>
                  <li>• View execution time and success status</li>
                  <li>• Expandable output and error details</li>
                  <li>• Success rate tracking</li>
                  <li>• Timestamp information</li>
                </ul>
              </div>
            </CardContent>
          </Card>
          <CommandHistory
            history={demoHistory}
            onCommandSelect={(command) => {
              console.log('Selected command:', command);
              handleDemoCommand(command);
            }}
          />
        </div>
      )}

      {/* Implementation Notes */}
    </div>
  );
};

export default GitTerminalDemo; 