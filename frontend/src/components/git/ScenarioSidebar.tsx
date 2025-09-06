import React, { useMemo } from 'react';
import { Card, Button } from '../ui';
import { GitScenario, UserProgress } from '../../types/git';

interface ScenarioSidebarProps {
  scenario: GitScenario;
  progress?: UserProgress | null;
  currentStep?: number;
  onShowGuidance?: () => void;
  onShowHint?: () => void;
  onRestart?: () => void;
}

type ParsedStep = { 
  title: string; 
  instructions?: string; 
  guidance?: string; 
  hint?: string; 
  stepNumber: number;
};

const ScenarioSidebar: React.FC<ScenarioSidebarProps> = ({
  scenario,
  progress,
  currentStep = 0,
  onShowGuidance,
  onShowHint,
  onRestart
}) => {
  const steps: ParsedStep[] = useMemo(() => {
    try {
      if (!scenario.expectedCommands) return [];
      const parsed = JSON.parse(scenario.expectedCommands);
      
      // Handle new JSON structure with detailed step information
      if (parsed && Array.isArray(parsed.steps)) {
        return parsed.steps.map((s: any, idx: number) => ({
          stepNumber: idx + 1,
          title: s.title || `Step ${idx + 1}`,
          instructions: s.instructions || '',
          guidance: s.guidance || '',
          hint: s.hint || ''
        }));
      }
      
      // Handle legacy JSON structure (array of commands)
      if (Array.isArray(parsed)) {
        return parsed.map((s: any, idx: number) => ({
          stepNumber: idx + 1,
          title: s.title || s.guidance || `Step ${idx + 1}`,
          instructions: s.guidance || '',
          guidance: s.guidance || '',
          hint: s.hint || ''
        }));
      }
    } catch (e) {
      console.warn('Failed to parse scenario steps:', e);
    }
    return [];
  }, [scenario.expectedCommands]);

  return (
    <aside className="w-full md:w-80 lg:w-96">
      <Card variant="elevated" className="p-4 space-y-4">
        <div>
          <h3 className="text-lg font-semibold">Scenario</h3>
          <div className="text-sm text-gray-600 dark:text-gray-400">{scenario.title}</div>
        </div>

        <div className="space-y-3">
          <div className="text-sm font-medium">Steps ({steps.length})</div>
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {steps.map((step, i) => {
              const isCurrent = i === (currentStep ?? 0);
              const isDone = progress?.currentStep !== undefined && i < progress.currentStep;
              const isUpcoming = !isDone && !isCurrent;
              
              return (
                <div key={i} className={`
                  p-3 rounded-lg border transition-all duration-200
                  ${isCurrent 
                    ? 'border-blue-400 bg-blue-50 dark:bg-blue-900/20 shadow-sm' 
                    : isDone 
                      ? 'border-green-300 bg-green-50 dark:bg-green-900/20' 
                      : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800'
                  }
                `}>
                  {/* Step Header */}
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">
                        {isDone ? '✅' : isCurrent ? '🔄' : isUpcoming ? '⭕' : '⚪'}
                      </span>
                      <div>
                        <div className={`text-sm font-medium ${isCurrent ? 'text-blue-700 dark:text-blue-300' : isDone ? 'text-green-700 dark:text-green-300' : 'text-gray-700 dark:text-gray-300'}`}>
                          Step {step.stepNumber}: {step.title}
                        </div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">
                          {isDone ? 'Completed' : isCurrent ? 'In Progress' : 'Pending'}
                        </div>
                      </div>
                    </div>
                    <span className="text-xs text-gray-400 bg-white dark:bg-gray-700 px-2 py-1 rounded">
                      {i + 1}/{steps.length}
                    </span>
                  </div>
                  
                  {/* Step Instructions - Show for current and upcoming steps */}
                  {(isCurrent || isUpcoming) && step.instructions && (
                    <div className="mb-2">
                      <div className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                        📋 Instructions:
                      </div>
                      <div className="text-sm text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 p-2 rounded border-l-2 border-blue-300">
                        {step.instructions}
                      </div>
                    </div>
                  )}
                  
                  {/* Step Guidance - Show for current step */}
                  {isCurrent && step.guidance && (
                    <div className="mb-2">
                      <div className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                        💡 Guidance:
                      </div>
                      <div className="text-sm text-blue-700 dark:text-blue-300 bg-blue-50 dark:bg-blue-900/30 p-2 rounded border-l-2 border-blue-400">
                        {step.guidance}
                      </div>
                    </div>
                  )}
                  
                  {/* Step Hint - Show for current step */}
                  {isCurrent && step.hint && (
                    <div>
                      <div className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                        🧭 Hint:
                      </div>
                      <div className="text-sm text-green-700 dark:text-green-300 bg-green-50 dark:bg-green-900/30 p-2 rounded border-l-2 border-green-400 font-mono">
                        {step.hint}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
            
            {steps.length === 0 && (
              <div className="text-center py-8">
                <div className="text-gray-400 text-4xl mb-2">📋</div>
                <div className="text-sm text-gray-500 dark:text-gray-400">
                  Steps will appear here when available.
                </div>
                <div className="text-xs text-gray-400 mt-1">
                  Start the scenario to see detailed instructions.
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={onShowGuidance}>💡 Guidance</Button>
          <Button variant="outline" size="sm" onClick={onShowHint}>🧭 Hint</Button>
          <Button variant="outline" size="sm" onClick={onRestart}>🔁 Restart</Button>
        </div>
      </Card>
    </aside>
  );
};

export default ScenarioSidebar;


