import React, { useState, useCallback } from 'react';
import { Card, Button } from '../ui';
import CodeEditor from './CodeEditor';
import AIChat from './AIChat';
import { useToast } from '../../contexts/ToastContext';

interface AIHelperProps {
  className?: string;
}

type AIFunction = 'chat' | 'explain' | 'debug' | 'refactor' | 'generate';

const AIHelper: React.FC<AIHelperProps> = ({ className = '' }) => {
  const [activeFunction, setActiveFunction] = useState<AIFunction>('chat');
  const [code, setCode] = useState('');
  const [isEditorExpanded, setIsEditorExpanded] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState('java');
  const { showSuccess } = useToast();

  const handleCodeGenerated = useCallback((generatedCode: string) => {
    setCode(generatedCode);
    showSuccess('Code generated and inserted into editor!');
  }, [showSuccess]);

  const handleCodeExplained = useCallback((explanation: string) => {
    showSuccess('Code explanation provided in chat!');
  }, [showSuccess]);

  const handleCodeChange = useCallback((value: string | undefined) => {
    setCode(value || '');
  }, []);

  const handleRunCode = () => {
    showSuccess('Code execution feature coming soon!');
  };

  const handleFormatCode = () => {
    // Simple formatting - in real app, use a proper formatter
    const formatted = code
      .split('\n')
      .map(line => line.trim())
      .join('\n');
    setCode(formatted);
    showSuccess('Code formatted!');
  };

  const aiFunctions = [
    {
      id: 'chat' as AIFunction,
      name: 'Chat',
      icon: '💬',
      description: 'General AI assistance'
    },
    {
      id: 'explain' as AIFunction,
      name: 'Explain',
      icon: '📖',
      description: 'Explain code concepts'
    },
    {
      id: 'debug' as AIFunction,
      name: 'Debug',
      icon: '🐛',
      description: 'Find and fix issues'
    },
    {
      id: 'refactor' as AIFunction,
      name: 'Refactor',
      icon: '🔧',
      description: 'Improve code quality'
    },
    {
      id: 'generate' as AIFunction,
      name: 'Generate',
      icon: '⚡',
      description: 'Create new code'
    }
  ];

  const languages = [
    { id: 'java', name: 'Java', icon: '☕' },
    { id: 'javascript', name: 'JavaScript', icon: '🟨' },
    { id: 'python', name: 'Python', icon: '🐍' },
    { id: 'typescript', name: 'TypeScript', icon: '🔷' }
  ];

  const getContextForFunction = () => {
    switch (activeFunction) {
      case 'explain':
        return code ? `Please explain this code:\n\n${code}` : 'Ready to explain code concepts and syntax.';
      case 'debug':
        return code ? `Please help debug this code:\n\n${code}` : 'Ready to help find and fix code issues.';
      case 'refactor':
        return code ? `Please suggest improvements for this code:\n\n${code}` : 'Ready to help improve code quality and structure.';
      case 'generate':
        return 'Ready to generate new code based on your requirements.';
      default:
        return '';
    }
  };

  return (
    <div className={`h-full flex flex-col ${className}`}>
      {/* Header with AI Function Tabs */}
      <Card variant="elevated" className="mb-4">
        <div className="p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-gray-800 dark:text-white">
              🤖 AI Code Helper
            </h2>
            <div className="flex items-center space-x-2">
              <select
                value={selectedLanguage}
                onChange={(e) => setSelectedLanguage(e.target.value)}
                className="px-3 py-1 border border-border-light dark:border-border-dark rounded-md bg-surface-light dark:bg-surface-dark text-text-light dark:text-text-dark text-sm"
              >
                {languages.map(lang => (
                  <option key={lang.id} value={lang.id}>
                    {lang.icon} {lang.name}
                  </option>
                ))}
              </select>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsEditorExpanded(!isEditorExpanded)}
              >
                {isEditorExpanded ? '📱 Split' : '📺 Expand'}
              </Button>
            </div>
          </div>
          
          {/* AI Function Tabs */}
          <div className="flex flex-wrap gap-2">
            {aiFunctions.map((func) => (
              <button
                key={func.id}
                onClick={() => setActiveFunction(func.id)}
                className={`flex items-center space-x-2 px-4 py-2 rounded-lg border transition-all ${
                  activeFunction === func.id
                    ? 'bg-primary-500 text-white border-primary-500'
                    : 'bg-surface-light dark:bg-surface-dark border-border-light dark:border-border-dark text-text-light dark:text-text-dark hover:border-primary-300'
                }`}
              >
                <span>{func.icon}</span>
                <span className="text-sm font-medium">{func.name}</span>
              </button>
            ))}
          </div>
          
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            {aiFunctions.find(f => f.id === activeFunction)?.description}
          </p>
        </div>
      </Card>

      {/* Main Content Area */}
      <div className="flex-1 flex gap-4">
        {/* Code Editor Panel */}
        <div className={`${isEditorExpanded ? 'w-full' : 'w-1/2'} transition-all duration-300`}>
          <Card variant="elevated" className="h-full flex flex-col">
            <div className="p-4 border-b border-border-light dark:border-border-dark">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-gray-800 dark:text-white">
                  Code Editor
                </h3>
                <div className="flex items-center space-x-2">
                  <Button variant="outline" size="sm" onClick={handleFormatCode}>
                    🎨 Format
                  </Button>
                  <Button variant="primary" size="sm" onClick={handleRunCode}>
                    ▶️ Run
                  </Button>
                </div>
              </div>
            </div>
            <div className="flex-1 p-4">
              <CodeEditor
                value={code}
                onChange={handleCodeChange}
                language={selectedLanguage}
                height="100%"
                placeholder={`// Start coding in ${selectedLanguage}...\n// Ask the AI for help with:\n// - Code generation\n// - Debugging\n// - Explanations\n// - Best practices`}
              />
            </div>
          </Card>
        </div>

        {/* AI Chat Panel */}
        {!isEditorExpanded && (
          <div className="w-1/2">
            <Card variant="elevated" className="h-full">
              <AIChat
                onCodeGenerated={handleCodeGenerated}
                onCodeExplained={handleCodeExplained}
                initialContext={getContextForFunction()}
                className="h-full"
              />
            </Card>
          </div>
        )}
      </div>

      {/* Floating Action Buttons */}
      <div className="fixed bottom-6 right-6 flex flex-col space-y-3">
        <button
          onClick={() => setActiveFunction('generate')}
          className="w-12 h-12 bg-gradient-to-r from-green-500 to-green-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all hover:scale-110 flex items-center justify-center"
          title="Generate Code"
        >
          ⚡
        </button>
        <button
          onClick={() => setActiveFunction('explain')}
          className="w-12 h-12 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all hover:scale-110 flex items-center justify-center"
          title="Explain Code"
        >
          📖
        </button>
        <button
          onClick={() => setActiveFunction('debug')}
          className="w-12 h-12 bg-gradient-to-r from-red-500 to-red-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all hover:scale-110 flex items-center justify-center"
          title="Debug Code"
        >
          🐛
        </button>
      </div>
    </div>
  );
};

export default AIHelper; 