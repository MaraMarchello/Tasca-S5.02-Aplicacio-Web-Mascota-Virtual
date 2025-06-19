import React from 'react';

interface GitOutputProps {
  output: string;
  error?: string;
  isError?: boolean;
  className?: string;
}

interface OutputLine {
  text: string;
  type: 'normal' | 'error' | 'warning' | 'success' | 'info' | 'branch' | 'commit' | 'file';
}

const GitOutput: React.FC<GitOutputProps> = ({ output, error, isError = false, className = '' }) => {
  const parseOutputLines = (text: string): OutputLine[] => {
    if (!text) return [];
    
    const lines = text.split('\n');
    return lines.map(line => {
      const trimmedLine = line.trim();
      
      // Error patterns
      if (trimmedLine.startsWith('error:') || trimmedLine.startsWith('fatal:')) {
        return { text: line, type: 'error' };
      }
      
      // Warning patterns
      if (trimmedLine.startsWith('warning:')) {
        return { text: line, type: 'warning' };
      }
      
      // Success patterns
      if (trimmedLine.includes('successfully') || trimmedLine.includes('completed')) {
        return { text: line, type: 'success' };
      }
      
      // Branch patterns
      if (trimmedLine.startsWith('*') || trimmedLine.includes('branch')) {
        return { text: line, type: 'branch' };
      }
      
      // Commit patterns
      if (trimmedLine.startsWith('commit ') || trimmedLine.match(/^\w{7,40}$/)) {
        return { text: line, type: 'commit' };
      }
      
      // File patterns
      if (trimmedLine.includes('.') && (trimmedLine.includes('modified:') || trimmedLine.includes('new file:') || trimmedLine.includes('deleted:'))) {
        return { text: line, type: 'file' };
      }
      
      // Info patterns
      if (trimmedLine.startsWith('On branch') || trimmedLine.startsWith('Your branch')) {
        return { text: line, type: 'info' };
      }
      
      return { text: line, type: 'normal' };
    });
  };

  const getLineClassName = (type: OutputLine['type']): string => {
    const baseClasses = 'font-mono text-sm leading-relaxed';
    
    switch (type) {
      case 'error':
        return `${baseClasses} text-red-400`;
      case 'warning':
        return `${baseClasses} text-yellow-400`;
      case 'success':
        return `${baseClasses} text-green-400`;
      case 'info':
        return `${baseClasses} text-blue-400`;
      case 'branch':
        return `${baseClasses} text-purple-400`;
      case 'commit':
        return `${baseClasses} text-orange-400`;
      case 'file':
        return `${baseClasses} text-cyan-400`;
      default:
        return `${baseClasses} text-gray-300 dark:text-gray-400`;
    }
  };

  const formatText = (text: string, type: OutputLine['type']): React.ReactNode => {
    // Handle commit hashes
    if (type === 'commit') {
      const commitHashRegex = /^commit ([a-f0-9]{40})/;
      const match = text.match(commitHashRegex);
      if (match) {
        return (
          <>
            <span className="text-orange-300">commit </span>
            <span className="text-orange-400 font-bold">{match[1].substring(0, 7)}</span>
            <span className="text-gray-500">{match[1].substring(7)}</span>
          </>
        );
      }
    }
    
    // Handle branch indicators
    if (type === 'branch' && text.includes('*')) {
      return text.replace('*', '→');
    }
    
    // Handle file status
    if (type === 'file') {
      if (text.includes('modified:')) {
        return text.replace('modified:', '📝 modified:');
      }
      if (text.includes('new file:')) {
        return text.replace('new file:', '✨ new file:');
      }
      if (text.includes('deleted:')) {
        return text.replace('deleted:', '🗑️ deleted:');
      }
    }
    
    return text;
  };

  const outputLines = parseOutputLines(output);
  const errorLines = error ? parseOutputLines(error) : [];
  const allLines = isError ? errorLines : [...outputLines, ...errorLines];

  if (allLines.length === 0) {
    return null;
  }

  return (
    <div className={`git-output ${className}`}>
      <div className="bg-gray-900 dark:bg-gray-800 rounded-lg p-4 overflow-auto max-h-96">
        <div className="space-y-1">
          {allLines.map((line, index) => (
            <div
              key={index}
              className={getLineClassName(line.type)}
            >
              {formatText(line.text, line.type)}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default GitOutput; 