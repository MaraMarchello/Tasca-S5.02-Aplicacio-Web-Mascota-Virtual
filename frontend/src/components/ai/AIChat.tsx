import React, { useState, useRef, useEffect } from 'react';
import { Button } from '../ui';
import { useToast } from '../../contexts/ToastContext';

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
    setInputValue('');
    setIsLoading(true);

    try {
      // Mock AI response - replace with actual API call
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      const response = generateMockResponse(userMessage.content);
      
      setMessages(prev => prev.map(msg => 
        msg.id === loadingMessage.id 
          ? { ...msg, content: response.content, isLoading: false }
          : msg
      ));

      // Handle special responses
      if (response.type === 'code' && onCodeGenerated) {
        onCodeGenerated(response.code || '');
      } else if (response.type === 'explanation' && onCodeExplained) {
        onCodeExplained(response.content);
      }

    } catch (error) {
      showError('Failed to get AI response');
      setMessages(prev => prev.filter(msg => msg.id !== loadingMessage.id));
    } finally {
      setIsLoading(false);
    }
  };

  const generateMockResponse = (userInput: string) => {
    const input = userInput.toLowerCase();
    
    if (input.includes('hello') || input.includes('hi')) {
      return {
        type: 'chat',
        content: 'Hello! I\'m ready to help you with your Java programming. What would you like to work on?'
      };
    }
    
    if (input.includes('create') || input.includes('generate') || input.includes('write')) {
      if (input.includes('class')) {
        return {
          type: 'code',
          content: 'Here\'s a basic Java class structure:',
          code: `public class MyClass {
    private String name;
    
    public MyClass(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "MyClass{name='" + name + "'}";
    }
}`
        };
      }
      
      if (input.includes('method') || input.includes('function')) {
        return {
          type: 'code',
          content: 'Here\'s a sample method:',
          code: `public void exampleMethod(String parameter) {
    System.out.println("Parameter received: " + parameter);
    // Add your logic here
}`
        };
      }
    }
    
    if (input.includes('explain') || input.includes('what is') || input.includes('how')) {
      return {
        type: 'explanation',
        content: `Great question! Let me explain:

**Java Concepts:**
- **Classes**: Templates for creating objects
- **Methods**: Functions that belong to a class
- **Variables**: Store data values
- **Inheritance**: Classes can inherit from other classes
- **Polymorphism**: Objects can take multiple forms

**Best Practices:**
- Use meaningful variable names
- Follow camelCase naming convention
- Keep methods focused on single tasks
- Add comments for complex logic

Would you like me to elaborate on any specific concept?`
      };
    }
    
    if (input.includes('debug') || input.includes('error') || input.includes('fix')) {
      return {
        type: 'chat',
        content: `I'd be happy to help debug your code! Here are some common Java issues and solutions:

**Common Errors:**
1. **NullPointerException**: Check for null values before using objects
2. **ArrayIndexOutOfBoundsException**: Verify array indices are within bounds
3. **Compilation errors**: Check syntax, missing semicolons, unmatched braces

**Debugging Tips:**
- Use System.out.println() for simple debugging
- Check variable values at different points
- Verify method parameters and return types

Please share your specific code or error message, and I'll provide targeted help!`
      };
    }
    
    return {
      type: 'chat',
      content: `I understand you're asking about: "${userInput}"

I can help you with:
- **Code Generation**: Creating Java classes, methods, and snippets
- **Code Explanation**: Breaking down complex concepts
- **Debugging**: Finding and fixing issues
- **Best Practices**: Writing clean, efficient code

What specific aspect would you like to explore?`
    };
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
            {message.content}
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