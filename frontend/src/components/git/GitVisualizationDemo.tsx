import React, { useState } from 'react';
import { Card } from '../ui';
import { GitGraph } from './visualization';
import { GitCommit, GitBranch } from '../../types/git';
import { VisualizationCommit, VisualizationBranch, MergeConnection } from '../../types/visualization';

const GitVisualizationDemo: React.FC = () => {
  const [selectedDemo, setSelectedDemo] = useState<'simple' | 'complex' | 'interactive'>('simple');
  const [selectedCommit, setSelectedCommit] = useState<VisualizationCommit | undefined>();
  const [selectedBranch, setSelectedBranch] = useState<VisualizationBranch | undefined>();
  const [highlightedCommits, setHighlightedCommits] = useState<string[]>([]);
  const [showMinimap, setShowMinimap] = useState(false);

  // Simple demo data
  const simpleCommits: GitCommit[] = [
    {
      id: 1,
      repositoryId: 1,
      commitHash: 'a1b2c3d4e5f6789012345678901234567890abcd',
      message: 'Initial commit',
      author: 'John Doe',
      authorEmail: 'john@example.com',
      timestamp: '2024-01-01T10:00:00Z',
      parentCommitId: undefined,
      branchName: 'main',
      fileChanges: { 'README.md': 'initial content' }
    },
    {
      id: 2,
      repositoryId: 1,
      commitHash: 'b2c3d4e5f67890123456789012345678901abcde',
      message: 'Add feature A',
      author: 'Jane Smith',
      authorEmail: 'jane@example.com',
      timestamp: '2024-01-02T11:00:00Z',
      parentCommitId: 1,
      branchName: 'main',
      fileChanges: { 'feature-a.js': 'feature a code' }
    },
    {
      id: 3,
      repositoryId: 1,
      commitHash: 'c3d4e5f678901234567890123456789012abcdef',
      message: 'Fix bug in feature A',
      author: 'John Doe',
      authorEmail: 'john@example.com',
      timestamp: '2024-01-03T12:00:00Z',
      parentCommitId: 2,
      branchName: 'main',
      fileChanges: { 'feature-a.js': 'fixed feature a code' }
    }
  ];

  const simpleBranches: GitBranch[] = [
    {
      id: 1,
      repositoryId: 1,
      branchName: 'main',
      headCommitId: 3,
      isActive: true,
      createdAt: '2024-01-01T10:00:00Z',
      mergedAt: undefined,
      isMerged: false
    }
  ];

  // Complex demo data with multiple branches and merges
  const complexCommits: GitCommit[] = [
    // Main branch
    {
      id: 1,
      repositoryId: 1,
      commitHash: 'a1b2c3d4e5f6789012345678901234567890abcd',
      message: 'Initial commit',
      author: 'John Doe',
      authorEmail: 'john@example.com',
      timestamp: '2024-01-01T10:00:00Z',
      parentCommitId: undefined,
      branchName: 'main',
      fileChanges: { 'README.md': 'initial content' }
    },
    {
      id: 2,
      repositoryId: 1,
      commitHash: 'b2c3d4e5f67890123456789012345678901abcde',
      message: 'Add project structure',
      author: 'John Doe',
      authorEmail: 'john@example.com',
      timestamp: '2024-01-02T10:00:00Z',
      parentCommitId: 1,
      branchName: 'main',
      fileChanges: { 'src/index.js': 'main entry point' }
    },
    // Feature branch
    {
      id: 3,
      repositoryId: 1,
      commitHash: 'c3d4e5f678901234567890123456789012abcdef',
      message: 'Start feature/user-auth',
      author: 'Jane Smith',
      authorEmail: 'jane@example.com',
      timestamp: '2024-01-03T09:00:00Z',
      parentCommitId: 2,
      branchName: 'feature/user-auth',
      fileChanges: { 'src/auth.js': 'auth skeleton' }
    },
    {
      id: 4,
      repositoryId: 1,
      commitHash: 'd4e5f67890123456789012345678901234abcdef0',
      message: 'Implement login functionality',
      author: 'Jane Smith',
      authorEmail: 'jane@example.com',
      timestamp: '2024-01-04T14:00:00Z',
      parentCommitId: 3,
      branchName: 'feature/user-auth',
      fileChanges: { 'src/auth.js': 'login implementation' }
    },
    // Continue main branch
    {
      id: 5,
      repositoryId: 1,
      commitHash: 'e5f678901234567890123456789012345abcdef01',
      message: 'Add database configuration',
      author: 'Bob Wilson',
      authorEmail: 'bob@example.com',
      timestamp: '2024-01-04T11:00:00Z',
      parentCommitId: 2,
      branchName: 'main',
      fileChanges: { 'config/database.js': 'db config' }
    },
    // Hotfix branch
    {
      id: 6,
      repositoryId: 1,
      commitHash: 'f67890123456789012345678901234abcdef0123',
      message: 'Hotfix: critical security patch',
      author: 'Alice Johnson',
      authorEmail: 'alice@example.com',
      timestamp: '2024-01-05T16:00:00Z',
      parentCommitId: 5,
      branchName: 'hotfix/security-patch',
      fileChanges: { 'src/security.js': 'security fix' }
    },
    // Merge hotfix back to main
    {
      id: 7,
      repositoryId: 1,
      commitHash: '67890123456789012345678901234abcdef01234',
      message: 'Merge hotfix/security-patch into main',
      author: 'Alice Johnson',
      authorEmail: 'alice@example.com',
      timestamp: '2024-01-05T17:00:00Z',
      parentCommitId: 6,
      branchName: 'main',
      fileChanges: {}
    },
    // Complete feature branch
    {
      id: 8,
      repositoryId: 1,
      commitHash: '7890123456789012345678901234abcdef012345',
      message: 'Add password validation',
      author: 'Jane Smith',
      authorEmail: 'jane@example.com',
      timestamp: '2024-01-06T10:00:00Z',
      parentCommitId: 4,
      branchName: 'feature/user-auth',
      fileChanges: { 'src/validation.js': 'password validation' }
    },
    // Merge feature into main
    {
      id: 9,
      repositoryId: 1,
      commitHash: '890123456789012345678901234abcdef0123456',
      message: 'Merge feature/user-auth into main',
      author: 'Jane Smith',
      authorEmail: 'jane@example.com',
      timestamp: '2024-01-07T15:00:00Z',
      parentCommitId: 8,
      branchName: 'main',
      fileChanges: {}
    }
  ];

  const complexBranches: GitBranch[] = [
    {
      id: 1,
      repositoryId: 1,
      branchName: 'main',
      headCommitId: 9,
      isActive: true,
      createdAt: '2024-01-01T10:00:00Z',
      mergedAt: undefined,
      isMerged: false
    },
    {
      id: 2,
      repositoryId: 1,
      branchName: 'feature/user-auth',
      headCommitId: 8,
      isActive: false,
      createdAt: '2024-01-03T09:00:00Z',
      mergedAt: '2024-01-07T15:00:00Z',
      isMerged: true
    },
    {
      id: 3,
      repositoryId: 1,
      branchName: 'hotfix/security-patch',
      headCommitId: 6,
      isActive: false,
      createdAt: '2024-01-05T16:00:00Z',
      mergedAt: '2024-01-05T17:00:00Z',
      isMerged: true
    }
  ];

  const currentCommits = selectedDemo === 'simple' ? simpleCommits : complexCommits;
  const currentBranches = selectedDemo === 'simple' ? simpleBranches : complexBranches;

  const handleCommitClick = (commit: VisualizationCommit) => {
    setSelectedCommit(commit);
    console.log('Commit clicked:', commit);
  };

  const handleBranchClick = (branch: VisualizationBranch) => {
    setSelectedBranch(branch);
    console.log('Branch clicked:', branch);
  };

  const handleMergeClick = (connection: MergeConnection) => {
    console.log('Merge connection clicked:', connection);
  };

  const highlightFeatureCommits = () => {
    const featureHashes = complexCommits
      .filter(c => c.branchName === 'feature/user-auth')
      .map(c => c.commitHash);
    setHighlightedCommits(featureHashes);
  };

  const clearHighlights = () => {
    setHighlightedCommits([]);
    setSelectedCommit(undefined);
    setSelectedBranch(undefined);
  };

  return (
    <div className="space-y-6">
      {/* Demo Controls */}
      <Card className="p-6">
        <div className="space-y-4">
          <div>
            <h2 className="text-2xl font-bold mb-2">🌳 Git Visualization Demo</h2>
            <p className="text-gray-600 dark:text-gray-400">
              Interactive Git graph visualization with D3.js - explore commits, branches, and merge operations
            </p>
          </div>

          {/* Demo Type Selection */}
          <div>
            <label className="block text-sm font-medium mb-2">Demo Type:</label>
            <div className="flex flex-wrap gap-2">
              <button
                className={`px-3 py-1 rounded text-sm ${
                  selectedDemo === 'simple' 
                    ? 'bg-blue-500 text-white' 
                    : 'bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600'
                }`}
                onClick={() => setSelectedDemo('simple')}
              >
                📝 Simple (Linear)
              </button>
              <button
                className={`px-3 py-1 rounded text-sm ${
                  selectedDemo === 'complex' 
                    ? 'bg-blue-500 text-white' 
                    : 'bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600'
                }`}
                onClick={() => setSelectedDemo('complex')}
              >
                🌿 Complex (Branches & Merges)
              </button>
              <button
                className={`px-3 py-1 rounded text-sm ${
                  selectedDemo === 'interactive' 
                    ? 'bg-blue-500 text-white' 
                    : 'bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600'
                }`}
                onClick={() => setSelectedDemo('interactive')}
              >
                🎮 Interactive Features
              </button>
            </div>
          </div>

          {/* Interactive Controls */}
          {selectedDemo === 'interactive' && (
            <div className="space-y-3 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
              <h4 className="font-medium text-blue-800 dark:text-blue-200">Interactive Controls:</h4>
              <div className="flex flex-wrap gap-2">
                <button
                  className="px-3 py-1 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
                  onClick={highlightFeatureCommits}
                >
                  🎯 Highlight Feature Branch
                </button>
                <button
                  className="px-3 py-1 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
                  onClick={clearHighlights}
                >
                  🧹 Clear Selection
                </button>
                <button
                  className="px-3 py-1 text-sm bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
                  onClick={() => setShowMinimap(!showMinimap)}
                >
                  {showMinimap ? '🗺️ Hide' : '🗺️ Show'} Minimap
                </button>
              </div>
            </div>
          )}
        </div>
      </Card>

      {/* Visualization */}
      <Card className="p-6">
        <div className="mb-4">
          <h3 className="text-xl font-semibold mb-2">
            {selectedDemo === 'simple' && '📝 Simple Linear History'}
            {selectedDemo === 'complex' && '🌿 Complex Git Workflow'}
            {selectedDemo === 'interactive' && '🎮 Interactive Exploration'}
          </h3>
          <p className="text-gray-600 dark:text-gray-400">
            {selectedDemo === 'simple' && 'Basic commit history on a single branch'}
            {selectedDemo === 'complex' && 'Multi-branch workflow with feature branches, hotfixes, and merges'}
            {selectedDemo === 'interactive' && 'Try clicking commits and branches, use zoom controls, and explore tooltips'}
          </p>
        </div>

        <div className="h-96 border border-gray-200 dark:border-gray-700 rounded-lg">
          <GitGraph
            commits={currentCommits}
            branches={currentBranches}
            onCommitClick={handleCommitClick}
            onBranchClick={handleBranchClick}
            onMergeClick={handleMergeClick}
            selectedCommit={selectedCommit}
            selectedBranch={selectedBranch}
            highlightedCommits={highlightedCommits}
            showMinimap={showMinimap}
            responsive={true}
            className="w-full h-full"
          />
        </div>
      </Card>

      {/* Selection Info */}
      {(selectedCommit || selectedBranch) && (
        <Card className="p-6">
          <h3 className="text-xl font-semibold mb-4">🔍 Selection Details</h3>
          
          {selectedCommit && (
            <div className="space-y-2 mb-4">
              <h4 className="font-medium text-blue-600 dark:text-blue-400">Selected Commit:</h4>
              <div className="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg font-mono text-sm">
                <div><strong>Hash:</strong> {selectedCommit.commitHash.substring(0, 7)}</div>
                <div><strong>Message:</strong> {selectedCommit.message}</div>
                <div><strong>Author:</strong> {selectedCommit.author}</div>
                <div><strong>Branch:</strong> {selectedCommit.branchName}</div>
                <div><strong>Date:</strong> {new Date(selectedCommit.timestamp).toLocaleString()}</div>
              </div>
            </div>
          )}
          
          {selectedBranch && (
            <div className="space-y-2">
              <h4 className="font-medium text-green-600 dark:text-green-400">Selected Branch:</h4>
              <div className="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg font-mono text-sm">
                <div><strong>Name:</strong> {selectedBranch.branchName}</div>
                <div><strong>Commits:</strong> {selectedBranch.commits.length}</div>
                <div><strong>Status:</strong> {selectedBranch.isActive ? 'Active' : 'Inactive'}</div>
                <div><strong>Merged:</strong> {selectedBranch.isMerged ? 'Yes' : 'No'}</div>
              </div>
            </div>
          )}
        </Card>
      )}


    </div>
  );
};

export default GitVisualizationDemo; 