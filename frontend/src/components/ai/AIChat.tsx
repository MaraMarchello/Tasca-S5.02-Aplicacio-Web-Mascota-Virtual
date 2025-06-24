import React, { useState, useRef, useEffect } from 'react';
import { Button } from '../ui';
import { useToast } from '../../contexts/ToastContext';
import { aiApi, type AIAssistanceResponse } from '../../utils/api';

interface Message {
  id: string;
  type: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  isLoading?: boolean;
}

interface AIChatProps {
  onCodeGenerated?: (code: string) => void;
  onCodeExplained?: (explanation: string) => void;
  initialContext?: string;
  className?: string;
}

const AIChat: React.FC<AIChatProps> = ({
  onCodeGenerated,
  onCodeExplained,
  initialContext,
  className = ''
}) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      type: 'system',
      content: 'Hello! I\'m your AI coding assistant. I can help you with Java programming, explain code, debug issues, and generate code snippets. How can I assist you today?',
      timestamp: new Date()
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const { showError } = useToast();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (initialContext) {
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'system',
        content: `Context: ${initialContext}`,
        timestamp: new Date()
      }]);
    }
  }, [initialContext]);

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue.trim(),
      timestamp: new Date()
    };

    const loadingMessage: Message = {
      id: (Date.now() + 1).toString(),
      type: 'assistant',
      content: 'Thinking...',
      timestamp: new Date(),
      isLoading: true
    };

    setMessages(prev => [...prev, userMessage, loadingMessage]);
    const currentInput = inputValue.trim();
    setInputValue('');
    setIsLoading(true);

    try {
      let aiResponse: AIAssistanceResponse;
      
      // Determine the type of request based on content
      if (isErrorMessage(currentInput)) {
        // If it looks like a stack trace or error message
        aiResponse = await aiApi.explainError(currentInput);
      } else if (isGitError(currentInput)) {
        // If it looks like a Git error
        aiResponse = await aiApi.explainGitError(currentInput);
      } else {
        // General code assistance
        const context = initialContext || getContextFromMessages();
        aiResponse = await aiApi.getCodeAssistance(currentInput, context);
      }
      
      // Format the AI response for display
      const formattedResponse = formatAIResponse(aiResponse);
      
      setMessages(prev => prev.map(msg => 
        msg.id === loadingMessage.id 
          ? { ...msg, content: formattedResponse.content, isLoading: false }
          : msg
      ));

      // Handle special responses
      if (aiResponse.codeSnippet && onCodeGenerated) {
        onCodeGenerated(aiResponse.codeSnippet);
      }
      
      if (formattedResponse.isExplanation && onCodeExplained) {
        onCodeExplained(formattedResponse.content);
      }

    } catch (error) {
      console.error('AI API Error:', error);
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      
      // Show user-friendly error message
      showError(`AI Assistant Error: ${errorMessage}`);
      
      // Replace loading message with error message
      setMessages(prev => prev.map(msg => 
        msg.id === loadingMessage.id 
          ? { 
              ...msg, 
              content: `I apologize, but I'm having trouble connecting to the AI service right now. Please try again in a moment.\n\nError: ${errorMessage}`,
              isLoading: false 
            }
          : msg
      ));
    } finally {
      setIsLoading(false);
    }
  };

  // Helper functions for AI integration
  const isErrorMessage = (input: string): boolean => {
    const errorIndicators = [
      'exception', 'error', 'stacktrace', 'stack trace', 
      'at java.', 'at com.', 'at org.', 'caused by',
      'nullpointerexception', 'classnotfoundexception',
      'illegalargumentexception', 'indexoutofboundsexception'
    ];
    const lowerInput = input.toLowerCase();
    return errorIndicators.some(indicator => lowerInput.includes(indicator)) ||
           input.includes('\tat ') || // Stack trace line indicator
           /\w+Exception/.test(input); // Any word ending with Exception
  };

  const isGitError = (input: string): boolean => {
    const gitErrorIndicators = [
      'git:', 'fatal:', 'error:', 'merge conflict',
      'git push', 'git pull', 'git merge', 'git rebase',
      'remote:', 'branch', 'commit', 'repository'
    ];
    const lowerInput = input.toLowerCase();
    return gitErrorIndicators.some(indicator => lowerInput.includes(indicator));
  };

  const getContextFromMessages = (): string => {
    // Get the last few messages as context (excluding system messages)
    const recentMessages = messages
      .filter(msg => msg.type !== 'system')
      .slice(-5) // Last 5 messages
      .map(msg => `${msg.type}: ${msg.content}`)
      .join('\n');
    return recentMessages;
  };

  const formatAIResponse = (response: AIAssistanceResponse): { content: string; isExplanation: boolean } => {
    let content = '';
    let isExplanation = false;

    // Start with the main answer
    if (response.answer) {
      content += `**Answer:**\n${response.answer}\n\n`;
    }

    // Add explanation if available
    if (response.explanation) {
      content += `**Explanation:**\n${response.explanation}\n\n`;
      isExplanation = true;
    }

    // Add code snippet if available
    if (response.codeSnippet) {
      content += `**Code Example:**\n\`\`\`java\n${response.codeSnippet}\n\`\`\`\n\n`;
    }

    // Add references if available
    if (response.references) {
      content += `**References:**\n${response.references}`;
    }

    // If no structured response, just return the answer
    if (!content.trim() && response.answer) {
      content = response.answer;
    }

    return { content: content.trim(), isExplanation };
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const MessageBubble: React.FC<{ message: Message }> = ({ message }) => (
    <div className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-[80%] rounded-lg px-4 py-3 ${
        message.type === 'user'
          ? 'bg-primary-500 text-white'
          : message.type === 'system'
          ? 'bg-secondary-100 dark:bg-secondary-900 text-secondary-800 dark:text-secondary-200'
          : 'bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-200'
      }`}>
        {message.isLoading ? (
          <div className="flex items-center space-x-2">
            <div className="animate-pulse flex space-x-1">
              <div className="w-2 h-2 bg-current rounded-full animate-bounce"></div>
              <div className="w-2 h-2 bg-current rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
              <div className="w-2 h-2 bg-current rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
            </div>
            <span className="text-sm">Thinking...</span>
          </div>
        ) : (
          <div className="whitespace-pre-wrap text-sm leading-relaxed">
            {/* Basic markdown-like formatting for code blocks */}
            {message.content.split('```').map((part, index) => {
              if (index % 2 === 1) {
                // This is a code block
                const lines = part.split('\n');
                const code = lines.slice(1).join('\n');
                return (
                  <pre key={index} className="bg-gray-800 text-green-400 p-3 rounded-md my-2 overflow-x-auto text-xs">
                    <code>{code}</code>
                  </pre>
                );
              } else {
                // Regular text with basic markdown formatting
                return (
                  <span key={index} dangerouslySetInnerHTML={{
                    __html: part
                      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                      .replace(/\*(.*?)\*/g, '<em>$1</em>')
                      .replace(/\n/g, '<br>')
                  }} />
                );
              }
            })}
          </div>
        )}
        <div className="mt-2 text-xs opacity-70">
          {message.timestamp.toLocaleTimeString()}
        </div>
      </div>
    </div>
  );

  return (
    <div className={`flex flex-col h-full bg-surface-light dark:bg-surface-dark ${className}`}>
      {/* Chat Header */}
      <div className="flex items-center justify-between p-4 border-b border-border-light dark:border-border-dark bg-white dark:bg-gray-800">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-full flex items-center justify-center">
            <span className="text-white text-sm font-bold">AI</span>
          </div>
          <div>
            <h3 className="font-semibold text-gray-800 dark:text-white">Code Assistant</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">Ready to help with Java</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
          <span className="text-xs text-gray-500 dark:text-gray-400">Online</span>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 border-t border-border-light dark:border-border-dark bg-white dark:bg-gray-800">
        <div className="flex items-end space-x-3">
          <div className="flex-1">
            <textarea
              ref={inputRef}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Ask me anything about Java programming..."
              className="w-full px-3 py-2 border border-border-light dark:border-border-dark rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-light dark:bg-surface-dark text-text-light dark:text-text-dark"
              rows={3}
              disabled={isLoading}
            />
            <div className="mt-2 flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
              <span>Press Enter to send, Shift+Enter for new line</span>
              <span>{inputValue.length}/1000</span>
            </div>
          </div>
          <Button
            onClick={handleSendMessage}
            disabled={!inputValue.trim() || isLoading}
            variant="primary"
            size="sm"
            className="self-end"
          >
            {isLoading ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            ) : (
              <>
                <span>Send</span>
                <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default AIChat; 