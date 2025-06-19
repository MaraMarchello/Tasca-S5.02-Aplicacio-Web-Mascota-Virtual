import React from 'react';

interface CommandHistoryItem {
  id: string;
  command: string;
  output: string;
  error?: string;
  timestamp: Date;
  successful: boolean;
  executionTime?: number;
}

interface CommandHistoryProps {
  history: CommandHistoryItem[];
  onCommandSelect?: (command: string) => void;
  className?: string;
  maxItems?: number;
}

const CommandHistory: React.FC<CommandHistoryProps> = ({ 
  history, 
  onCommandSelect, 
  className = '',
  maxItems = 50 
}) => {
  const displayHistory = history.slice(-maxItems).reverse();

  const formatTimestamp = (timestamp: Date): string => {
    return timestamp.toLocaleTimeString('en-US', { 
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const getStatusIcon = (successful: boolean): string => {
    return successful ? '✅' : '❌';
  };

  const getStatusColor = (successful: boolean): string => {
    return successful 
      ? 'text-green-400 border-green-400/20 bg-green-400/5' 
      : 'text-red-400 border-red-400/20 bg-red-400/5';
  };

  const handleCommandClick = (command: string) => {
    if (onCommandSelect) {
      onCommandSelect(command);
    }
  };

  if (displayHistory.length === 0) {
    return (
      <div className={`command-history ${className}`}>
        <div className="bg-surface-light dark:bg-surface-dark rounded-lg p-4">
          <div className="text-center text-gray-500 dark:text-gray-400">
            <div className="text-2xl mb-2">📜</div>
            <p className="text-sm">No commands executed yet</p>
            <p className="text-xs mt-1">Start typing git commands to see history</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`command-history ${className}`}>
      <div className="bg-surface-light dark:bg-surface-dark rounded-lg p-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-light dark:text-text-dark">
            Command History
          </h3>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            {displayHistory.length} / {maxItems}
          </span>
        </div>
        
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {displayHistory.map((item) => (
            <div
              key={item.id}
              className={`border rounded-lg p-3 transition-all duration-200 hover:shadow-md cursor-pointer ${getStatusColor(item.successful)}`}
              onClick={() => handleCommandClick(item.command)}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-2">
                  <span className="text-lg">{getStatusIcon(item.successful)}</span>
                  <code className="text-sm font-mono bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                    {item.command}
                  </code>
                </div>
                <div className="flex items-center space-x-2 text-xs text-gray-500 dark:text-gray-400">
                  {item.executionTime && (
                    <span>{item.executionTime}ms</span>
                  )}
                  <span>{formatTimestamp(item.timestamp)}</span>
                </div>
              </div>
              
              {(item.output || item.error) && (
                <div className="mt-2">
                  {item.output && (
                    <div className="text-xs font-mono text-gray-600 dark:text-gray-300 bg-gray-50 dark:bg-gray-800 p-2 rounded border-l-2 border-blue-400">
                      <div className="text-blue-400 mb-1">Output:</div>
                      <pre className="whitespace-pre-wrap break-words max-h-20 overflow-y-auto">
                        {item.output}
                      </pre>
                    </div>
                  )}
                  {item.error && (
                    <div className="text-xs font-mono text-red-400 bg-red-50 dark:bg-red-900/20 p-2 rounded border-l-2 border-red-400 mt-2">
                      <div className="text-red-400 mb-1">Error:</div>
                      <pre className="whitespace-pre-wrap break-words max-h-20 overflow-y-auto">
                        {item.error}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
        
        <div className="mt-4 text-xs text-gray-500 dark:text-gray-400 text-center">
          💡 Click on any command to reuse it
        </div>
      </div>
    </div>
  );
};

export default CommandHistory;
export type { CommandHistoryItem }; 