import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import { VisualizationCommit, CommitTooltip } from '../../../types/visualization';

interface CommitNodeProps {
  commit: VisualizationCommit;
  isSelected?: boolean;
  isHighlighted?: boolean;
  onCommitClick?: (commit: VisualizationCommit) => void;
  onCommitHover?: (commit: VisualizationCommit | null) => void;
  showTooltip?: boolean;
  animated?: boolean;
}

const CommitNode: React.FC<CommitNodeProps> = ({
  commit,
  isSelected = false,
  isHighlighted = false,
  onCommitClick,
  onCommitHover,
  showTooltip = true,
  animated = true
}) => {
  const nodeRef = useRef<SVGCircleElement>(null);
  const [isHovered, setIsHovered] = useState(false);
  const [tooltip, setTooltip] = useState<CommitTooltip | null>(null);

  const effectiveRadius = isSelected ? commit.radius * 1.5 : 
                         isHighlighted ? commit.radius * 1.2 : 
                         commit.radius;

  useEffect(() => {
    if (!nodeRef.current || !animated) return;

    const node = d3.select(nodeRef.current);
    
    // Animate radius change
    node.transition()
      .duration(200)
      .ease(d3.easeBackOut.overshoot(1.7))
      .attr('r', effectiveRadius);

  }, [effectiveRadius, animated]);

  const handleMouseEnter = (event: React.MouseEvent<SVGCircleElement>) => {
    setIsHovered(true);
    onCommitHover?.(commit);

    if (showTooltip) {
      const rect = event.currentTarget.getBoundingClientRect();
      setTooltip({
        commit,
        x: rect.left + rect.width / 2,
        y: rect.top - 10,
        visible: true
      });
    }
  };

  const handleMouseLeave = () => {
    setIsHovered(false);
    onCommitHover?.(null);
    setTooltip(null);
  };

  const handleClick = () => {
    onCommitClick?.(commit);
  };

  const formatTimestamp = (timestamp: string): string => {
    return new Date(timestamp).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const truncateMessage = (message: string, maxLength: number = 50): string => {
    return message.length > maxLength ? `${message.substring(0, maxLength)}...` : message;
  };

  return (
    <>
      {/* Commit Node */}
      <g className="commit-node">
        {/* Outer ring for selection/highlight */}
        {(isSelected || isHighlighted) && (
          <circle
            cx={commit.x}
            cy={commit.y}
            r={effectiveRadius + 3}
            fill="none"
            stroke={isSelected ? '#3b82f6' : '#f59e0b'}
            strokeWidth={2}
            strokeDasharray={isHighlighted && !isSelected ? '3,3' : undefined}
            opacity={0.8}
          />
        )}

        {/* Main commit circle */}
        <circle
          ref={nodeRef}
          cx={commit.x}
          cy={commit.y}
          r={effectiveRadius}
          fill={commit.color}
          stroke="#ffffff"
          strokeWidth={2}
          className={`
            cursor-pointer transition-all duration-200
            ${isHovered ? 'drop-shadow-lg' : 'drop-shadow'}
            hover:stroke-gray-300
          `}
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          onClick={handleClick}
        />

        {/* Commit hash text */}
        <text
          x={commit.x}
          y={commit.y + effectiveRadius + 15}
          textAnchor="middle"
          className="text-xs font-mono fill-gray-600 dark:fill-gray-400 pointer-events-none"
          fontSize="10"
        >
          {commit.commitHash.substring(0, 7)}
        </text>

        {/* Hover overlay for better interaction */}
        <circle
          cx={commit.x}
          cy={commit.y}
          r={Math.max(effectiveRadius + 5, 15)}
          fill="transparent"
          className="cursor-pointer"
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          onClick={handleClick}
        />
      </g>

      {/* Tooltip */}
      {tooltip && (
        <foreignObject
          x={tooltip.x - 150}
          y={tooltip.y - 80}
          width="300"
          height="80"
          className="pointer-events-none"
        >
          <div className="bg-gray-900 dark:bg-gray-800 text-white text-sm rounded-lg p-3 shadow-lg border border-gray-700 max-w-xs">
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-gray-300">
                  {commit.commitHash.substring(0, 7)}
                </span>
                <span className="text-xs text-gray-400">
                  {formatTimestamp(commit.timestamp)}
                </span>
              </div>
              <div className="font-medium">
                {truncateMessage(commit.message)}
              </div>
              <div className="flex items-center justify-between text-xs text-gray-400">
                <span>{commit.author}</span>
                <span className="px-2 py-1 bg-gray-700 rounded text-xs">
                  {commit.branchName}
                </span>
              </div>
            </div>
            {/* Tooltip arrow */}
            <div className="absolute bottom-0 left-1/2 transform translate-y-full -translate-x-1/2">
              <div className="w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900 dark:border-t-gray-800"></div>
            </div>
          </div>
        </foreignObject>
      )}
    </>
  );
};

export default CommitNode; 