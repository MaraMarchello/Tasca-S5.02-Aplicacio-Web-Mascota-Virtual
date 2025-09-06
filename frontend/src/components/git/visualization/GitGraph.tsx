import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as d3 from 'd3';
import { GitCommit, GitBranch } from '../../../types/git';
import { 
  VisualizationCommit, 
  VisualizationBranch, 
  MergeConnection,
  GraphLayout,
  GraphDimensions,
  GraphInteraction,
  ZoomState,
  GraphConfig
} from '../../../types/visualization';
import { 
  createGraphLayout, 
  getCommitAtPosition, 
  getBranchAtPosition,
  DEFAULT_GRAPH_CONFIG
} from '../../../utils/graphLayout';
import CommitNode from './CommitNode';
import BranchLine from './BranchLine';
import MergeVisualization from './MergeVisualization';

interface GitGraphProps {
  commits: GitCommit[];
  branches: GitBranch[];
  onCommitClick?: (commit: VisualizationCommit) => void;
  onBranchClick?: (branch: VisualizationBranch) => void;
  onMergeClick?: (connection: MergeConnection) => void;
  selectedCommit?: VisualizationCommit;
  selectedBranch?: VisualizationBranch;
  highlightedCommits?: string[];
  highlightedBranches?: string[];
  config?: Partial<GraphConfig>;
  responsive?: boolean;
  showMinimap?: boolean;
  className?: string;
}

const GitGraph: React.FC<GitGraphProps> = ({
  commits,
  branches,
  onCommitClick,
  onBranchClick,
  onMergeClick,
  selectedCommit,
  selectedBranch,
  highlightedCommits = [],
  highlightedBranches = [],
  config: userConfig,
  responsive = true,
  showMinimap = false,
  className = ''
}) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState<GraphDimensions>({
    width: 800,
    height: 600,
    margin: { top: 40, right: 100, bottom: 40, left: 40 }
  });
  const [zoomState, setZoomState] = useState<ZoomState>({
    scale: 1,
    translateX: 0,
    translateY: 0
  });
  const [interaction, setInteraction] = useState<GraphInteraction>({});
  const [layout, setLayout] = useState<GraphLayout | null>(null);

  const config = { ...DEFAULT_GRAPH_CONFIG, ...userConfig };

  // Handle responsive resizing
  useEffect(() => {
    if (!responsive || !containerRef.current) return;

    const resizeObserver = new ResizeObserver(entries => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        setDimensions(prev => ({
          ...prev,
          width: Math.max(width, 400),
          height: Math.max(height, 300)
        }));
      }
    });

    resizeObserver.observe(containerRef.current);
    return () => resizeObserver.disconnect();
  }, [responsive]);

  // Calculate layout when data changes
  useEffect(() => {
    if (!commits || commits.length === 0) {
      setLayout(null);
      return;
    }

    // Validate and filter out invalid commits/branches
    const validCommits = commits.filter(commit => 
      commit && 
      commit.branchName && 
      commit.commitHash && 
      commit.timestamp
    );
    
    const validBranches = branches.filter(branch => 
      branch && 
      branch.branchName && 
      typeof branch.id === 'number'
    );

    if (validCommits.length === 0) {
      console.warn('GitGraph: No valid commits found');
      setLayout(null);
      return;
    }

    try {
      const newLayout = createGraphLayout(validCommits, validBranches, dimensions, config);
      setLayout(newLayout);
    } catch (error) {
      console.error('GitGraph: Error creating layout:', error);
      setLayout(null);
    }
  }, [commits, branches, dimensions, config]);

  // Setup zoom and pan behavior
  useEffect(() => {
    if (!svgRef.current) return;

    const svg = d3.select(svgRef.current);
    const g = svg.select('.graph-content');

    const zoom = d3.zoom<SVGSVGElement, unknown>()
      .scaleExtent([config.zoom.min, config.zoom.max])
      .on('zoom', (event) => {
        const { transform } = event;
        setZoomState({
          scale: transform.k,
          translateX: transform.x,
          translateY: transform.y
        });
        g.attr('transform', transform.toString());
      });

    svg.call(zoom);

    // Initial zoom to fit content
    if (layout && layout.commits.length > 0) {
      const bounds = {
        minX: Math.min(...layout.commits.map(c => c.x)) - 50,
        maxX: Math.max(...layout.commits.map(c => c.x)) + 50,
        minY: Math.min(...layout.commits.map(c => c.y)) - 50,
        maxY: Math.max(...layout.commits.map(c => c.y)) + 50
      };

      const width = bounds.maxX - bounds.minX;
      const height = bounds.maxY - bounds.minY;
      const scale = Math.min(
        dimensions.width / width,
        dimensions.height / height,
        1
      ) * 0.9;

      const centerX = (bounds.minX + bounds.maxX) / 2;
      const centerY = (bounds.minY + bounds.maxY) / 2;
      const translateX = dimensions.width / 2 - centerX * scale;
      const translateY = dimensions.height / 2 - centerY * scale;

      svg.call(
        zoom.transform,
        d3.zoomIdentity.translate(translateX, translateY).scale(scale)
      );
    }

    return () => {
      svg.on('.zoom', null);
    };
  }, [layout, dimensions, config.zoom]);

  // Handle mouse interactions
  const handleSvgClick = useCallback((event: React.MouseEvent<SVGSVGElement>) => {
    if (!layout) return;

    const svg = svgRef.current;
    if (!svg) return;

    const rect = svg.getBoundingClientRect();
    const x = (event.clientX - rect.left - zoomState.translateX) / zoomState.scale;
    const y = (event.clientY - rect.top - zoomState.translateY) / zoomState.scale;

    // Check for commit clicks
    const clickedCommit = getCommitAtPosition(layout.commits, x, y);
    if (clickedCommit) {
      onCommitClick?.(clickedCommit);
      setInteraction(prev => ({ ...prev, selectedCommit: clickedCommit }));
      return;
    }

    // Check for branch clicks
    const clickedBranch = getBranchAtPosition(layout.branches, x, y);
    if (clickedBranch) {
      onBranchClick?.(clickedBranch);
      setInteraction(prev => ({ ...prev, selectedBranch: clickedBranch }));
      return;
    }

    // Clear selection if clicking empty space
    setInteraction(prev => ({ 
      ...prev, 
      selectedCommit: undefined, 
      selectedBranch: undefined 
    }));
  }, [layout, zoomState, onCommitClick, onBranchClick]);

  const handleCommitHover = useCallback((commit: VisualizationCommit | null) => {
    setInteraction(prev => ({ ...prev, hoveredCommit: commit }));
  }, []);

  const handleBranchHover = useCallback((branch: VisualizationBranch | null) => {
    setInteraction(prev => ({ ...prev, hoveredBranch: branch }));
  }, []);

  const handleMergeHover = useCallback((_connection: MergeConnection | null) => {
    // Could add merge connection hover state here
  }, []);

  // Zoom control functions
  const zoomIn = useCallback(() => {
    if (!svgRef.current) return;
    const svg = d3.select(svgRef.current);
    svg.transition().call(
      d3.zoom<SVGSVGElement, unknown>().scaleBy as any,
      1 + config.zoom.step
    );
  }, [config.zoom.step]);

  const zoomOut = useCallback(() => {
    if (!svgRef.current) return;
    const svg = d3.select(svgRef.current);
    svg.transition().call(
      d3.zoom<SVGSVGElement, unknown>().scaleBy as any,
      1 - config.zoom.step
    );
  }, [config.zoom.step]);

  const resetZoom = useCallback(() => {
    if (!svgRef.current || !layout) return;
    const svg = d3.select(svgRef.current);
    svg.transition().call(
      d3.zoom<SVGSVGElement, unknown>().transform as any,
      d3.zoomIdentity
    );
  }, [layout]);

  if (!layout) {
    return (
      <div className={`git-graph ${className}`} ref={containerRef}>
        <div className="flex items-center justify-center h-64 text-gray-500 dark:text-gray-400">
          <div className="text-center">
            <div className="text-4xl mb-4">🌳</div>
            <p>No commits to visualize</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`git-graph relative ${className}`} ref={containerRef}>
      {/* Zoom Controls */}
      <div className="absolute top-4 right-4 z-10 flex flex-col space-y-2">
        <button
          onClick={zoomIn}
          className="p-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          title="Zoom In"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
        </button>
        <button
          onClick={zoomOut}
          className="p-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          title="Zoom Out"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 12H6" />
          </svg>
        </button>
        <button
          onClick={resetZoom}
          className="p-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          title="Reset Zoom"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
      </div>

      {/* Graph Info */}
      <div className="absolute top-4 left-4 z-10 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg p-3 shadow-sm">
        <div className="text-sm text-gray-600 dark:text-gray-400">
          <div>{layout.commits.length} commits</div>
          <div>{layout.branches.length} branches</div>
          <div className="text-xs mt-1">
            Zoom: {Math.round(zoomState.scale * 100)}%
          </div>
        </div>
      </div>

      {/* Main SVG */}
      <svg
        ref={svgRef}
        width={dimensions.width}
        height={dimensions.height}
        className="w-full h-full border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-900"
        onClick={handleSvgClick}
      >
        <defs>
          {/* Gradient definitions for enhanced visuals */}
          <radialGradient id="commitGradient" cx="50%" cy="30%">
            <stop offset="0%" stopColor="rgba(255,255,255,0.3)" />
            <stop offset="100%" stopColor="rgba(0,0,0,0.1)" />
          </radialGradient>
          
          {/* Drop shadow filter */}
          <filter id="dropShadow" x="-50%" y="-50%" width="200%" height="200%">
            <feDropShadow dx="2" dy="2" stdDeviation="3" floodOpacity="0.3"/>
          </filter>
        </defs>

        <g className="graph-content">
          {/* Render merge connections first (behind everything) */}
          {layout.mergeConnections.map(connection => (
            <MergeVisualization
              key={connection.id}
              connection={connection}
              onConnectionClick={onMergeClick}
              onConnectionHover={handleMergeHover}
            />
          ))}

          {/* Render branch lines */}
          {layout.branches.map(branch => (
            <BranchLine
              key={branch.id}
              branch={branch}
              isSelected={selectedBranch?.id === branch.id || interaction.selectedBranch?.id === branch.id}
              isHighlighted={highlightedBranches.includes(branch.branchName)}
              onBranchClick={onBranchClick}
              onBranchHover={handleBranchHover}
            />
          ))}

          {/* Render commit nodes (on top) */}
          {layout.commits.map(commit => (
            <CommitNode
              key={commit.id}
              commit={commit}
              isSelected={selectedCommit?.id === commit.id || interaction.selectedCommit?.id === commit.id}
              isHighlighted={highlightedCommits.includes(commit.commitHash)}
              onCommitClick={onCommitClick}
              onCommitHover={handleCommitHover}
            />
          ))}
        </g>
      </svg>

      {/* Minimap (optional) */}
      {showMinimap && (
        <div className="absolute bottom-4 right-4 w-48 h-32 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm overflow-hidden">
          <svg width="100%" height="100%" className="bg-gray-50 dark:bg-gray-900">
            {/* Simplified minimap representation */}
            {layout.branches.map(branch => (
              <path
                key={`mini-${branch.id}`}
                d={branch.path}
                fill="none"
                stroke={branch.color}
                strokeWidth={1}
                transform="scale(0.2)"
                opacity={0.7}
              />
            ))}
            {layout.commits.map(commit => (
              <circle
                key={`mini-${commit.id}`}
                cx={commit.x * 0.2}
                cy={commit.y * 0.2}
                r={2}
                fill={commit.color}
              />
            ))}
          </svg>
        </div>
      )}
    </div>
  );
};

export default GitGraph; 