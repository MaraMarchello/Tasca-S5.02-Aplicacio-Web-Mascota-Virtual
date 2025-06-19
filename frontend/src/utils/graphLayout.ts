import * as d3 from 'd3';
import { GitCommit, GitBranch } from '../types/git';
import { 
  VisualizationCommit, 
  VisualizationBranch, 
  MergeConnection, 
  GraphLayout, 
  GraphDimensions,
  GraphConfig 
} from '../types/visualization';

export const DEFAULT_GRAPH_CONFIG: GraphConfig = {
  commitRadius: 8,
  branchSpacing: 60,
  commitSpacing: 80,
  colors: {
    main: '#2563eb',      // blue-600
    develop: '#059669',   // emerald-600
    feature: '#7c3aed',   // violet-600
    hotfix: '#dc2626',    // red-600
    release: '#ea580c',   // orange-600
    default: '#6b7280'    // gray-500
  },
  animation: {
    duration: 800,
    easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
  },
  zoom: {
    min: 0.1,
    max: 3,
    step: 0.1
  }
};

export function getBranchColor(branchName: string, config: GraphConfig = DEFAULT_GRAPH_CONFIG): string {
  const lowerName = branchName.toLowerCase();
  
  if (lowerName === 'main' || lowerName === 'master') {
    return config.colors.main;
  } else if (lowerName === 'develop' || lowerName === 'dev') {
    return config.colors.develop;
  } else if (lowerName.startsWith('feature/') || lowerName.startsWith('feat/')) {
    return config.colors.feature;
  } else if (lowerName.startsWith('hotfix/') || lowerName.startsWith('fix/')) {
    return config.colors.hotfix;
  } else if (lowerName.startsWith('release/') || lowerName.startsWith('rel/')) {
    return config.colors.release;
  }
  
  return config.colors.default;
}

export function calculateCommitPositions(
  commits: GitCommit[],
  branches: GitBranch[],
  dimensions: GraphDimensions,
  config: GraphConfig = DEFAULT_GRAPH_CONFIG
): VisualizationCommit[] {
  if (commits.length === 0) return [];

  // Sort commits by timestamp
  const sortedCommits = [...commits].sort((a, b) => 
    new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  );

  // Create branch position mapping
  const branchPositions = new Map<string, number>();
  const sortedBranches = [...branches].sort((a, b) => {
    // Main/master branch always at top
    if (a.branchName === 'main' || a.branchName === 'master') return -1;
    if (b.branchName === 'main' || b.branchName === 'master') return 1;
    return a.branchName.localeCompare(b.branchName);
  });

  sortedBranches.forEach((branch, index) => {
    branchPositions.set(branch.branchName, index);
  });

  // Calculate time scale
  const timeExtent = d3.extent(sortedCommits, d => new Date(d.timestamp)) as [Date, Date];
  const xScale = d3.scaleTime()
    .domain(timeExtent)
    .range([dimensions.margin.left, dimensions.width - dimensions.margin.right]);

  // Calculate positions
  const visualCommits: VisualizationCommit[] = sortedCommits.map((commit) => {
    const branchIndex = branchPositions.get(commit.branchName) || 0;
    const x = xScale(new Date(commit.timestamp));
    const y = dimensions.margin.top + (branchIndex * config.branchSpacing) + config.commitRadius;
    
    return {
      ...commit,
      x,
      y,
      radius: config.commitRadius,
      color: getBranchColor(commit.branchName, config),
      selected: false,
      highlighted: false
    };
  });

  return visualCommits;
}

export function calculateBranchPaths(
  commits: VisualizationCommit[],
  branches: GitBranch[],
  config: GraphConfig = DEFAULT_GRAPH_CONFIG
): VisualizationBranch[] {
  const visualBranches: VisualizationBranch[] = branches.map(branch => {
    const branchCommits = commits.filter(c => c.branchName === branch.branchName);
    
    if (branchCommits.length === 0) {
      return {
        ...branch,
        color: getBranchColor(branch.branchName, config),
        path: '',
        commits: [],
        startX: 0,
        startY: 0,
        endX: 0,
        endY: 0
      };
    }

    // Sort commits by x position (time)
    const sortedCommits = branchCommits.sort((a, b) => a.x - b.x);
    const firstCommit = sortedCommits[0];
    const lastCommit = sortedCommits[sortedCommits.length - 1];

    // Create smooth path through commits
    const pathData = d3.line<VisualizationCommit>()
      .x(d => d.x)
      .y(d => d.y)
      .curve(d3.curveCatmullRom.alpha(0.5));

    const path = pathData(sortedCommits) || '';

    return {
      ...branch,
      color: getBranchColor(branch.branchName, config),
      path,
      commits: branchCommits,
      startX: firstCommit.x,
      startY: firstCommit.y,
      endX: lastCommit.x,
      endY: lastCommit.y
    };
  });

  return visualBranches;
}

export function calculateMergeConnections(
  commits: VisualizationCommit[],
  _config: GraphConfig = DEFAULT_GRAPH_CONFIG
): MergeConnection[] {
  const connections: MergeConnection[] = [];

  commits.forEach(commit => {
    if (commit.parentCommitId) {
      const parentCommit = commits.find(c => c.id === commit.parentCommitId);
      
      if (parentCommit && parentCommit.branchName !== commit.branchName) {
        // This is a merge or branch creation
        const connectionId = `${parentCommit.id}-${commit.id}`;
        
        // Create curved path for merge connection
        const midX = (parentCommit.x + commit.x) / 2;
        const controlX = midX;
        const controlY = Math.min(parentCommit.y, commit.y) - 20;

        const path = `M ${parentCommit.x} ${parentCommit.y} Q ${controlX} ${controlY} ${commit.x} ${commit.y}`;

        connections.push({
          id: connectionId,
          fromCommit: parentCommit,
          toCommit: commit,
          fromBranch: parentCommit.branchName,
          toBranch: commit.branchName,
          type: 'merge',
          animated: false,
          color: '#6b7280', // gray-500
          path
        });
      }
    }
  });

  return connections;
}

export function createGraphLayout(
  commits: GitCommit[],
  branches: GitBranch[],
  dimensions: GraphDimensions,
  config: GraphConfig = DEFAULT_GRAPH_CONFIG
): GraphLayout {
  const visualCommits = calculateCommitPositions(commits, branches, dimensions, config);
  const visualBranches = calculateBranchPaths(visualCommits, branches, config);
  const mergeConnections = calculateMergeConnections(visualCommits, config);

  const timeRange = commits.length > 0 ? {
    start: new Date(Math.min(...commits.map(c => new Date(c.timestamp).getTime()))),
    end: new Date(Math.max(...commits.map(c => new Date(c.timestamp).getTime())))
  } : {
    start: new Date(),
    end: new Date()
  };

  return {
    commits: visualCommits,
    branches: visualBranches,
    mergeConnections,
    dimensions,
    timeRange
  };
}

export function getCommitAtPosition(
  commits: VisualizationCommit[],
  x: number,
  y: number,
  threshold: number = 20
): VisualizationCommit | null {
  return commits.find(commit => {
    const distance = Math.sqrt(
      Math.pow(commit.x - x, 2) + Math.pow(commit.y - y, 2)
    );
    return distance <= threshold;
  }) || null;
}

export function getBranchAtPosition(
  branches: VisualizationBranch[],
  x: number,
  y: number,
  threshold: number = 10
): VisualizationBranch | null {
  // This is a simplified implementation
  // In a real scenario, you'd need to check if the point is near the branch path
  return branches.find(branch => {
    return branch.commits.some(commit => {
      const distance = Math.sqrt(
        Math.pow(commit.x - x, 2) + Math.pow(commit.y - y, 2)
      );
      return distance <= threshold;
    });
  }) || null;
}

export function animateCommitCreation(
  commit: VisualizationCommit,
  onUpdate: (commit: VisualizationCommit) => void,
  duration: number = 600
): void {
  const startRadius = 0;
  const endRadius = commit.radius;
  const startTime = Date.now();

  const animate = () => {
    const elapsed = Date.now() - startTime;
    const progress = Math.min(elapsed / duration, 1);
    
    // Easing function (ease-out-back)
    const easeOutBack = (t: number) => {
      const c1 = 1.70158;
      const c3 = c1 + 1;
      return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    };

    const easedProgress = easeOutBack(progress);
    const currentRadius = startRadius + (endRadius - startRadius) * easedProgress;

    onUpdate({
      ...commit,
      radius: currentRadius
    });

    if (progress < 1) {
      requestAnimationFrame(animate);
    }
  };

  requestAnimationFrame(animate);
}

export function animateMergeConnection(
  connection: MergeConnection,
  onUpdate: (connection: MergeConnection) => void,
  duration: number = 1000
): void {
  const startTime = Date.now();

  const animate = () => {
    const elapsed = Date.now() - startTime;
    const progress = Math.min(elapsed / duration, 1);

    onUpdate({
      ...connection,
      animated: true
    });

    if (progress < 1) {
      requestAnimationFrame(animate);
    } else {
      onUpdate({
        ...connection,
        animated: false
      });
    }
  };

  requestAnimationFrame(animate);
} 