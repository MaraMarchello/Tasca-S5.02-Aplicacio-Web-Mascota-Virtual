import React, { useState, useRef, useEffect, useCallback } from 'react';

interface GitCommand {
  command: string;
  description: string;
  category: 'basic' | 'branching' | 'merging' | 'remote' | 'advanced';
  options?: string[];
}

interface GitPromptProps {
  onCommandSubmit: (command: string) => void;
  commandHistory: string[];
  isLoading?: boolean;
  currentDirectory?: string;
  currentBranch?: string;
  className?: string;
  placeholder?: string;
}

const GitPrompt: React.FC<GitPromptProps> = ({
  onCommandSubmit,
  commandHistory,
  isLoading = false,
  currentDirectory = '~/project',
  currentBranch = 'main',
  className = '',
  placeholder = 'Enter git command...'
}) => {
  const [input, setInput] = useState('');
  const [historyIndex, setHistoryIndex] = useState(-1);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  const [autocompleteIndex, setAutocompleteIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const gitCommands: GitCommand[] = [
    // Basic commands
    { command: 'git status', description: 'Show the working tree status', category: 'basic' },
    { command: 'git add', description: 'Add file contents to the index', category: 'basic', options: ['.', '-A', '--all', '-u'] },
    { command: 'git commit', description: 'Record changes to the repository', category: 'basic', options: ['-m', '-a', '--amend'] },
    { command: 'git log', description: 'Show commit logs', category: 'basic', options: ['--oneline', '--graph', '--all'] },
    { command: 'git diff', description: 'Show changes between commits', category: 'basic', options: ['--cached', '--staged'] },
    { command: 'git init', description: 'Create an empty Git repository', category: 'basic' },
    
    // Branching commands
    { command: 'git branch', description: 'List, create, or delete branches', category: 'branching', options: ['-a', '-r', '-d', '-D'] },
    { command: 'git checkout', description: 'Switch branches or restore files', category: 'branching', options: ['-b', '-'] },
    { command: 'git switch', description: 'Switch branches', category: 'branching', options: ['-c', '-'] },
    { command: 'git merge', description: 'Join two or more development histories', category: 'merging', options: ['--no-ff', '--squash'] },
    
    // Remote commands
    { command: 'git clone', description: 'Clone a repository into a new directory', category: 'remote' },
    { command: 'git fetch', description: 'Download objects and refs from another repository', category: 'remote' },
    { command: 'git pull', description: 'Fetch from and integrate with another repository', category: 'remote' },
    { command: 'git push', description: 'Update remote refs along with associated objects', category: 'remote', options: ['-u', '--force', '--set-upstream'] },
    
    // Advanced commands
    { command: 'git rebase', description: 'Reapply commits on top of another base tip', category: 'advanced', options: ['-i', '--interactive'] },
    { command: 'git reset', description: 'Reset current HEAD to the specified state', category: 'advanced', options: ['--soft', '--mixed', '--hard'] },
    { command: 'git stash', description: 'Stash the changes in a dirty working directory', category: 'advanced', options: ['push', 'pop', 'list', 'drop'] },
    { command: 'git cherry-pick', description: 'Apply the changes introduced by some existing commits', category: 'advanced' },
  ];

  const getFilteredCommands = useCallback(() => {
    if (!input.trim()) return [];
    
    const inputLower = input.toLowerCase();
    const filtered = gitCommands.filter(cmd => 
      cmd.command.toLowerCase().includes(inputLower) ||
      cmd.description.toLowerCase().includes(inputLower)
    );
    
    // If input starts with a command, also show options
    const baseCommand = input.split(' ')[0];
    const matchingCommand = gitCommands.find(cmd => cmd.command === baseCommand);
    if (matchingCommand && matchingCommand.options && input.includes(' ')) {
      const currentOption = input.split(' ').pop() || '';
      const optionSuggestions = matchingCommand.options
        .filter(option => option.toLowerCase().includes(currentOption.toLowerCase()))
        .map(option => ({
          command: `${baseCommand} ${option}`,
          description: `${matchingCommand.description} (${option})`,
          category: matchingCommand.category
        }));
      
      return [...filtered, ...optionSuggestions].slice(0, 8);
    }
    
    return filtered.slice(0, 8);
  }, [input]);

  const filteredCommands = getFilteredCommands();

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInput(value);
    setHistoryIndex(-1);
    setShowAutocomplete(value.trim().length > 0);
    setAutocompleteIndex(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (isLoading) return;

    switch (e.key) {
      case 'Enter':
        e.preventDefault();
        if (showAutocomplete && filteredCommands.length > 0) {
          setInput(filteredCommands[autocompleteIndex].command);
          setShowAutocomplete(false);
        } else if (input.trim()) {
          onCommandSubmit(input.trim());
          setInput('');
          setHistoryIndex(-1);
          setShowAutocomplete(false);
        }
        break;

      case 'ArrowUp':
        e.preventDefault();
        if (showAutocomplete && filteredCommands.length > 0) {
          setAutocompleteIndex(prev => 
            prev > 0 ? prev - 1 : filteredCommands.length - 1
          );
        } else if (commandHistory.length > 0) {
          const newIndex = historyIndex < commandHistory.length - 1 ? historyIndex + 1 : historyIndex;
          setHistoryIndex(newIndex);
          setInput(commandHistory[commandHistory.length - 1 - newIndex] || '');
        }
        break;

      case 'ArrowDown':
        e.preventDefault();
        if (showAutocomplete && filteredCommands.length > 0) {
          setAutocompleteIndex(prev => 
            prev < filteredCommands.length - 1 ? prev + 1 : 0
          );
        } else if (historyIndex > 0) {
          const newIndex = historyIndex - 1;
          setHistoryIndex(newIndex);
          setInput(commandHistory[commandHistory.length - 1 - newIndex] || '');
        } else if (historyIndex === 0) {
          setHistoryIndex(-1);
          setInput('');
        }
        break;

      case 'Tab':
        e.preventDefault();
        if (showAutocomplete && filteredCommands.length > 0) {
          setInput(filteredCommands[autocompleteIndex].command);
          setShowAutocomplete(false);
        }
        break;

      case 'Escape':
        setShowAutocomplete(false);
        setAutocompleteIndex(0);
        break;
    }
  };

  const handleAutocompleteClick = (command: string) => {
    setInput(command);
    setShowAutocomplete(false);
    inputRef.current?.focus();
  };

  const getCategoryColor = (category: GitCommand['category']): string => {
    switch (category) {
      case 'basic': return 'text-green-400';
      case 'branching': return 'text-blue-400';
      case 'merging': return 'text-purple-400';
      case 'remote': return 'text-orange-400';
      case 'advanced': return 'text-red-400';
      default: return 'text-gray-400';
    }
  };

  const getCategoryIcon = (category: GitCommand['category']): string => {
    switch (category) {
      case 'basic': return '📁';
      case 'branching': return '🌿';
      case 'merging': return '🔀';
      case 'remote': return '🌐';
      case 'advanced': return '⚡';
      default: return '💻';
    }
  };

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  return (
    <div className={`git-prompt relative ${className}`}>
      <div className="bg-gray-900 dark:bg-gray-800 rounded-lg border border-gray-700 dark:border-gray-600">
        {/* Terminal Header */}
        <div className="flex items-center justify-between px-4 py-2 bg-gray-800 dark:bg-gray-700 rounded-t-lg border-b border-gray-700 dark:border-gray-600">
          <div className="flex items-center space-x-2">
            <div className="flex space-x-1">
              <div className="w-3 h-3 bg-red-500 rounded-full"></div>
              <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
            </div>
            <span className="text-sm text-gray-300 font-mono">Git Terminal</span>
          </div>
          <div className="text-xs text-gray-400 font-mono">
            {currentDirectory}
          </div>
        </div>

        {/* Command Input */}
        <div className="p-4">
          <div className="flex items-center space-x-2">
            <span className="text-green-400 font-mono text-sm">
              user@codemate:{currentDirectory}
            </span>
            <span className="text-blue-400 font-mono text-sm">
              ({currentBranch})
            </span>
            <span className="text-white font-mono text-sm">$</span>
            <div className="flex-1 relative">
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
                placeholder={placeholder}
                disabled={isLoading}
                className="w-full bg-transparent text-white font-mono text-sm outline-none placeholder-gray-500 disabled:opacity-50"
              />
              {isLoading && (
                <div className="absolute right-2 top-1/2 transform -translate-y-1/2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-400"></div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Autocomplete Dropdown */}
      {showAutocomplete && filteredCommands.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-gray-800 dark:bg-gray-700 border border-gray-600 rounded-lg shadow-lg z-50 max-h-64 overflow-y-auto">
          {filteredCommands.map((cmd, index) => (
            <div
              key={`${cmd.command}-${index}`}
              className={`px-4 py-2 cursor-pointer transition-colors ${
                index === autocompleteIndex
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-700'
              }`}
              onClick={() => handleAutocompleteClick(cmd.command)}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="text-lg">{getCategoryIcon(cmd.category)}</span>
                  <code className="font-mono text-sm">{cmd.command}</code>
                </div>
                <span className={`text-xs ${getCategoryColor(cmd.category)}`}>
                  {cmd.category}
                </span>
              </div>
              <div className="text-xs text-gray-400 mt-1 ml-6">
                {cmd.description}
              </div>
            </div>
          ))}
          <div className="px-4 py-2 text-xs text-gray-500 border-t border-gray-600">
            Use ↑↓ to navigate, Tab or Enter to select, Esc to close
          </div>
        </div>
      )}
    </div>
  );
};

export default GitPrompt; 