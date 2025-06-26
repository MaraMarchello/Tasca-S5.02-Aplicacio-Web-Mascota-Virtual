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
  codeSnippet?: string;
}

interface Conversation {
  id: number;
  title: string;
  contextType: string;
  programmingLanguage: string;
  updatedAt: string;
}

interface AIChatProps {
  onCodeGenerated?: (code: string) => void;
  onCodeExplained?: (explanation: string) => void;
  initialContext?: string;
  className?: string;
  mode?: 'chat' | 'explain' | 'debug' | 'refactor' | 'generate';
  currentCode?: string;
}

const AIChat: React.FC<AIChatProps> = ({
  onCodeGenerated,
  onCodeExplained,
  initialContext,
  className = '',
  mode = 'chat',
  currentCode
}) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [codeInput, setCodeInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [currentConversation, setCurrentConversation] = useState<Conversation | null>(null);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [showConversations, setShowConversations] = useState(false);
  const [showCodeExecution, setShowCodeExecution] = useState(false);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const { showError, showSuccess } = useToast();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    initializeChat();
  }, [mode, initialContext]);

  useEffect(() => {
    if (currentCode) {
      setCodeInput(currentCode);
    }
  }, [currentCode]);

  const initializeChat = async () => {
    // Load conversations
    try {
      const conversationsData = await aiApi.getConversations(0, 10);
      setConversations(conversationsData.content || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }

    // Set initial system message based on mode
    const systemMessage = getSystemMessage(mode);
    setMessages([{
      id: '1',
      type: 'system',
      content: systemMessage,
      timestamp: new Date()
    }]);

    // Add context if provided
    if (initialContext) {
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'system',
        content: `Context: ${initialContext}`,
        timestamp: new Date()
      }]);
    }
  };

  const getSystemMessage = (mode: string): string => {
    switch (mode) {
      case 'explain':
        return 'I\'m here to explain code and concepts. Share your code or ask about programming concepts!';
      case 'debug':
        return 'I\'m ready to help debug your code. Share your error messages or problematic code!';
      case 'refactor':
        return 'I can help improve your code quality and structure. Share the code you\'d like to refactor!';
      case 'generate':
        return 'I can generate code based on your requirements. Describe what you need!';
      default:
        return 'Hello! I\'m your AI coding assistant with conversation memory. I can help you with Java programming, explain code, debug issues, generate code snippets, and even execute code. How can I assist you today?';
    }
  };

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue.trim(),
      timestamp: new Date(),
      codeSnippet: codeInput.trim() || undefined
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
    const currentCodeSnippet = codeInput.trim() || undefined;
    setInputValue('');
    setIsLoading(true);

    try {
      let aiResponse: AIAssistanceResponse;
      
      if (currentConversation) {
        // Continue existing conversation
        aiResponse = await aiApi.continueConversation(
          currentConversation.id,
          currentInput,
          currentCodeSnippet
        );
      } else {
        // Start new conversation with memory
        const contextType = mode === 'chat' ? 'general' : mode;
        aiResponse = await aiApi.chatWithMemory(
          currentInput,
          contextType,
          'java',
          currentCodeSnippet
        );
        
        // Refresh conversations list
        try {
          const conversationsData = await aiApi.getConversations(0, 10);
          setConversations(conversationsData.content || []);
          if (conversationsData.content && conversationsData.content.length > 0) {
            setCurrentConversation(conversationsData.content[0]);
          }
        } catch (error) {
          console.error('Failed to refresh conversations:', error);
        }
      }
      
      // Format the AI response for display
      const formattedResponse = formatAIResponse(aiResponse);
      
      setMessages(prev => prev.map(msg => 
        msg.id === loadingMessage.id 
          ? { 
              ...msg, 
              content: formattedResponse.content, 
              isLoading: false,
              codeSnippet: aiResponse.codeSnippet
            }
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

  const handleExecuteCode = async () => {
    if (!codeInput.trim()) {
      showError('Please enter some code to execute');
      return;
    }

    setIsLoading(true);
    try {
      const result = await aiApi.executeCode(codeInput.trim());
      setExecutionResult(result);
      setShowCodeExecution(true);
      
      if (result.success) {
        showSuccess('Code executed successfully!');
      } else {
        showError('Code execution failed');
      }
    } catch (error) {
      console.error('Code execution error:', error);
      showError('Failed to execute code: ' + (error instanceof Error ? error.message : 'Unknown error'));
    } finally {
      setIsLoading(false);
    }
  };

  const loadConversation = async (conversation: Conversation) => {
    try {
      setIsLoading(true);
      const messages = await aiApi.getConversationMessages(conversation.id);
      
      // Convert API messages to UI messages
      const uiMessages: Message[] = messages.map((msg: any) => ({
        id: msg.id.toString(),
        type: msg.messageType.toLowerCase(),
        content: msg.content,
        timestamp: new Date(msg.createdAt),
        codeSnippet: msg.codeSnippet
      }));
      
      setMessages(uiMessages);
      setCurrentConversation(conversation);
      setShowConversations(false);
    } catch (error) {
      console.error('Failed to load conversation:', error);
      showError('Failed to load conversation');
    } finally {
      setIsLoading(false);
    }
  };

  const startNewConversation = () => {
    setCurrentConversation(null);
    setMessages([{
      id: '1',
      type: 'system',
      content: getSystemMessage(mode),
      timestamp: new Date()
    }]);
    setShowConversations(false);
  };

  // Helper functions for AI integration

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
            <h3 className="font-semibold text-gray-800 dark:text-white">
              {currentConversation ? currentConversation.title : `Code Assistant (${mode})`}
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {currentConversation ? 'Conversation with memory' : 'Ready to help with Java'}
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <Button
            onClick={() => setShowConversations(!showConversations)}
            variant="secondary"
            size="sm"
            className="text-xs"
          >
            History
          </Button>
          <Button
            onClick={startNewConversation}
            variant="secondary"
            size="sm"
            className="text-xs"
          >
            New Chat
          </Button>
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-xs text-gray-500 dark:text-gray-400">Online</span>
          </div>
        </div>
      </div>

      {/* Conversations Sidebar */}
      {showConversations && (
        <div className="max-h-48 overflow-y-auto border-b border-border-light dark:border-border-dark bg-gray-50 dark:bg-gray-900 p-2">
          <div className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Recent Conversations</div>
          {conversations.length === 0 ? (
            <div className="text-xs text-gray-500 dark:text-gray-400">No conversations yet</div>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => loadConversation(conv)}
                className={`p-2 rounded cursor-pointer text-sm mb-1 ${
                  currentConversation?.id === conv.id
                    ? 'bg-primary-100 dark:bg-primary-900 text-primary-800 dark:text-primary-200'
                    : 'bg-white dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700'
                }`}
              >
                <div className="font-medium truncate">{conv.title}</div>
                <div className="text-xs text-gray-500 dark:text-gray-400">
                  {conv.contextType} • {new Date(conv.updatedAt).toLocaleDateString()}
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Code Execution Result Modal */}
      {showCodeExecution && executionResult && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-2xl max-h-96 overflow-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">Code Execution Result</h3>
              <button
                onClick={() => setShowCodeExecution(false)}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <span className={`inline-block px-2 py-1 rounded text-sm ${
                  executionResult.success ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                }`}>
                  {executionResult.success ? 'Success' : 'Error'}
                </span>
                <span className="ml-2 text-sm text-gray-500">
                  Execution time: {executionResult.executionTime}ms
                </span>
              </div>
              <div>
                <div className="text-sm font-medium mb-1">Output:</div>
                <pre className="bg-gray-900 text-green-400 p-3 rounded text-sm overflow-x-auto">
                  {executionResult.success ? executionResult.output : executionResult.error}
                </pre>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Input Area */}
      <div className="p-4 border-t border-border-light dark:border-border-dark bg-white dark:bg-gray-800">
        {/* Code Input Section */}
        <div className="mb-3">
          <div className="flex items-center justify-between mb-2">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Code (optional)
            </label>
            <div className="flex space-x-2">
              <Button
                onClick={handleExecuteCode}
                disabled={!codeInput.trim() || isLoading}
                variant="secondary"
                size="sm"
                className="text-xs"
              >
                Execute
              </Button>
              <Button
                onClick={() => setCodeInput('')}
                disabled={!codeInput.trim()}
                variant="secondary"
                size="sm"
                className="text-xs"
              >
                Clear
              </Button>
            </div>
          </div>
          <textarea
            value={codeInput}
            onChange={(e) => setCodeInput(e.target.value)}
            placeholder="Enter Java code here (will be included in your message)..."
            className="w-full px-3 py-2 border border-border-light dark:border-border-dark rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-light dark:bg-surface-dark text-text-light dark:text-text-dark font-mono text-sm"
            rows={4}
            disabled={isLoading}
          />
        </div>

        {/* Message Input */}
        <div className="flex items-end space-x-3">
          <div className="flex-1">
            <textarea
              ref={inputRef}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Ask me anything about Java programming..."
              className="w-full px-3 py-2 border border-border-light dark:border-border-dark rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-light dark:bg-surface-dark text-text-light dark:text-text-dark"
              rows={2}
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