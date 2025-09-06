export interface GitScenario {
  id: number;
  scenarioId: string;
  title: string;
  description: string;
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  category: 'BASICS' | 'BRANCHING' | 'MERGING' | 'CONFLICTS' | 'COLLABORATION' | 'ADVANCED_WORKFLOWS';
  pointsReward: number;
  estimatedMinutes: number;
  orderIndex: number;
  isActive: boolean;
  initialState?: string; // JSON representation of initial repository state
  expectedCommands?: string; // JSON array of expected commands
  successCriteria?: string; // JSON representation of success criteria
  tags?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserProgress {
  id: number;
  userId: number;
  scenario: GitScenario;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'ABANDONED';
  currentStep: number;
  totalSteps: number;
  commandsExecuted: number;
  hintsUsed: number;
  pointsEarned: number;
  startedAt: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface GitRepository {
  id: number;
  repositoryName: string;
  scenarioId?: string;
  userId: number;
  currentBranch: string;
  createdAt: string;
  lastModifiedAt: string;
  isActive: boolean;
}

export interface GitCommit {
  id: number;
  repositoryId: number;
  commitHash: string;
  message: string;
  author: string;
  authorEmail: string;
  timestamp: string;
  parentCommitId?: number;
  branchName: string;
  fileChanges: { [filename: string]: string };
}

export interface GitBranch {
  id: number;
  repositoryId: number;
  branchName: string;
  headCommitId?: number;
  isActive: boolean;
  createdAt: string;
  mergedAt?: string;
  isMerged: boolean;
}

export interface GitCommandResult {
  successful: boolean;
  exitCode: number;
  output: string;
  errorOutput: string;
  commitHash?: string;
  affectedFiles?: string[];
  executionTimeMs: number;
}

export interface GitCommandRequest {
  command: string;
  scenarioId?: string;
  stepNumber?: number;
}

export interface RepositoryState {
  repositoryId: number;
  currentBranch: string;
  commits: GitCommit[];
  branches: GitBranch[];
  // The following fields will be populated progressively as the simulator expands
  workingDirectory?: { [filename: string]: string };
  stagingArea?: { [filename: string]: string };
  headRef?: string; // e.g., refs/heads/main or detached
  detachedHead?: boolean;
  remotes?: { [remoteName: string]: { [ref: string]: string } };
  workingDirectory: { [filename: string]: string };
  stagingArea: { [filename: string]: string };
  lastCommand?: string;
  lastCommandResult?: GitCommandResult;
}

export interface ExecuteResponse {
  result: GitCommandResult;
  stepCompleted?: boolean;
  nextStepNumber?: number;
  progress?: UserProgress;
  repositoryState?: RepositoryState;
  tutorMessage?: string;
}

export interface ScenarioStep {
  stepNumber: number;
  title: string;
  description: string;
  instructions: string;
  expectedCommands: string[];
  validationRules: ValidationRule[];
  hints: string[];
  pointsReward: number;
}

export interface ValidationRule {
  type: 'COMMAND_EXECUTED' | 'FILE_EXISTS' | 'FILE_CONTENT' | 'BRANCH_EXISTS' | 'COMMIT_MESSAGE' | 'MERGE_COMPLETED';
  target: string;
  condition: string;
  errorMessage: string;
}

export interface GitGuidance {
  stepNumber: number;
  title: string;
  description: string;
  instructions: string;
  expectedCommands: string[];
  hints: string[];
  nextSteps: string[];
}

export interface CommandHistoryItem {
  id: string;
  command: string;
  output: string;
  error?: string;
  timestamp: Date;
  successful: boolean;
  executionTime?: number;
}

export interface AutocompleteCommand {
  command: string;
  description: string;
  category: 'basic' | 'branching' | 'merging' | 'remote' | 'advanced';
  options?: string[];
}

export interface GitStats {
  totalCommands: number;
  successfulCommands: number;
  failedCommands: number;
  averageExecutionTime: number;
  mostUsedCommands: { command: string; count: number }[];
  scenariosCompleted: number;
  totalPointsEarned: number;
  currentStreak: number;
  longestStreak: number;
} 