import { GitCommit, GitBranch } from './git';

export interface VisualizationCommit extends GitCommit {
  x: number;
  y: number;
  radius: number;
  color: string;
  selected?: boolean;
  highlighted?: boolean;
}

export interface VisualizationBranch extends GitBranch {
  color: string;
  path: string; // SVG path for the branch line
  commits: VisualizationCommit[];
  startX: number;
  startY: number;
  endX: number;
  endY: number;
}

export interface MergeConnection {
  id: string;
  fromCommit: VisualizationCommit;
  toCommit: VisualizationCommit;
  fromBranch: string;
  toBranch: string;
  type: 'merge' | 'rebase' | 'cherry-pick';
  animated?: boolean;
  color: string;
  path: string; // SVG path for the connection
}

export interface GraphDimensions {
  width: number;
  height: number;
  margin: {
    top: number;
    right: number;
    bottom: number;
    left: number;
  };
}

export interface ZoomState {
  scale: number;
  translateX: number;
  translateY: number;
}

export interface GraphLayout {
  commits: VisualizationCommit[];
  branches: VisualizationBranch[];
  mergeConnections: MergeConnection[];
  dimensions: GraphDimensions;
  timeRange: {
    start: Date;
    end: Date;
  };
}

export interface GraphInteraction {
  hoveredCommit?: VisualizationCommit | null;
  selectedCommit?: VisualizationCommit;
  hoveredBranch?: VisualizationBranch | null;
  selectedBranch?: VisualizationBranch;
  dragState?: {
    isDragging: boolean;
    startX: number;
    startY: number;
  };
}

export interface AnimationState {
  isAnimating: boolean;
  currentAnimation?: {
    type: 'merge' | 'rebase' | 'commit' | 'branch-creation';
    progress: number; // 0-1
    duration: number;
    startTime: number;
  };
}

export interface GraphConfig {
  commitRadius: number;
  branchSpacing: number;
  commitSpacing: number;
  colors: {
    main: string;
    develop: string;
    feature: string;
    hotfix: string;
    release: string;
    default: string;
  };
  animation: {
    duration: number;
    easing: string;
  };
  zoom: {
    min: number;
    max: number;
    step: number;
  };
}

export interface CommitTooltip {
  commit: VisualizationCommit;
  x: number;
  y: number;
  visible: boolean;
}

export interface BranchTooltip {
  branch: VisualizationBranch;
  x: number;
  y: number;
  visible: boolean;
} 