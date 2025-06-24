import React, { useState } from 'react';
import Layout from '../components/layout/Layout';
import { aiApi } from '../utils/api';
import { useToast } from '../contexts/ToastContext';

const StackTracePage: React.FC = () => {
  const [stackTrace, setStackTrace] = useState('');
  const [explanation, setExplanation] = useState('');
  const [loading, setLoading] = useState(false);
  const { showError, showSuccess } = useToast();

  const handleAnalyze = async () => {
    if (!stackTrace.trim()) {
      showError('Please enter a stack trace to analyze.');
      return;
    }

    setLoading(true);
    try {
      const response = await aiApi.explainError(stackTrace);
      
      // Format the AI response for display
      let formattedExplanation = '';
      
      if (response.answer) {
        formattedExplanation += `**Analysis:**\n${response.answer}\n\n`;
      }
      
      if (response.explanation) {
        formattedExplanation += `**Detailed Explanation:**\n${response.explanation}\n\n`;
      }
      
      if (response.codeSnippet) {
        formattedExplanation += `**Example Fix:**\n\`\`\`java\n${response.codeSnippet}\n\`\`\`\n\n`;
      }
      
      if (response.references) {
        formattedExplanation += `**References:**\n${response.references}`;
      }
      
      setExplanation(formattedExplanation.trim());
      showSuccess('Stack trace analyzed successfully!');
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      console.error('Stack trace analysis error:', error);
      showError(`Failed to analyze stack trace: ${errorMessage}`);
      setExplanation(`**Error:** Unable to analyze the stack trace at this time.\n\n**Reason:** ${errorMessage}\n\n**Suggestion:** Please check your internet connection and try again. If the problem persists, the AI service may be temporarily unavailable.`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
            🔍 Stack Trace Explainer
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Paste your Java stack trace below and get an explanation
          </p>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
          <div className="space-y-4">
            <div>
              <label htmlFor="stackTrace" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Stack Trace
              </label>
              <textarea
                id="stackTrace"
                value={stackTrace}
                onChange={(e) => setStackTrace(e.target.value)}
                className="w-full h-48 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                placeholder="Paste your Java stack trace here..."
              />
            </div>

            <button
              onClick={handleAnalyze}
              disabled={loading || !stackTrace.trim()}
              className={`w-full py-3 px-4 rounded-lg font-medium transition-colors ${
                loading || !stackTrace.trim()
                  ? 'bg-gray-300 dark:bg-gray-600 cursor-not-allowed'
                  : 'bg-blue-500 hover:bg-blue-600 text-white'
              }`}
            >
              {loading ? 'Analyzing...' : 'Explain Stack Trace'}
            </button>
          </div>
        </div>

        {explanation && (
          <div className="bg-green-50 dark:bg-green-900 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-green-800 dark:text-green-200 mb-3">
              🤖 AI Analysis Results
            </h3>
            <div className="text-green-700 dark:text-green-300 whitespace-pre-wrap prose prose-sm max-w-none">
              {explanation}
            </div>
          </div>
        )}

        <div className="bg-blue-50 dark:bg-blue-900 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-800 dark:text-blue-200 mb-3">
            💡 Tips for Better Error Handling
          </h3>
          <ul className="list-disc list-inside text-blue-700 dark:text-blue-300 space-y-1">
            <li>Always read the stack trace from top to bottom</li>
            <li>Look for the first occurrence of your code (not library code)</li>
            <li>Pay attention to line numbers and method names</li>
            <li>Common exceptions: NullPointerException, ArrayIndexOutOfBoundsException, ClassCastException</li>
          </ul>
        </div>
      </div>
    </Layout>
  );
};

export default StackTracePage; 