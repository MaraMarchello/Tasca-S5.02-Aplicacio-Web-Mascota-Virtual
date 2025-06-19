import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import { VisualizationBranch, BranchTooltip } from '../../../types/visualization';

interface BranchLineProps {
  branch: VisualizationBranch;
  isSelected?: boolean;
  isHighlighted?: boolean;
  onBranchClick?: (branch: VisualizationBranch) => void;
  onBranchHover?: (branch: VisualizationBranch | null) => void;
  showTooltip?: boolean;
  animated?: boolean;
  strokeWidth?: number;
}

const BranchLine: React.FC<BranchLineProps> = ({
  branch,
  isSelected = false,
  isHighlighted = false,
  onBranchClick,
  onBranchHover,
  showTooltip = true,
  animated = true,
  strokeWidth = 3
}) => {
  const pathRef = useRef<SVGPathElement>(null);
  const [isHovered, setIsHovered] = useState(false);
  const [tooltip, setTooltip] = useState<BranchTooltip | null>(null);
  const [, setPathLength] = useState(0);

  const effectiveStrokeWidth = isSelected ? strokeWidth * 2 : 
                              isHighlighted ? strokeWidth * 1.5 : 
                              strokeWidth;

  useEffect(() => {
    if (!pathRef.current) return;

    const path = pathRef.current;
    const length = path.getTotalLength();
    setPathLength(length);

    if (animated) {
      // Initial draw animation
      d3.select(path)
        .attr('stroke-dasharray', `${length} ${length}`)
        .attr('stroke-dashoffset', length)
        .transition()
        .duration(1000)
        .ease(d3.easeLinear)
        .attr('stroke-dashoffset', 0)
        .on('end', () => {
          // Remove dash array after animation
          d3.select(path).attr('stroke-dasharray', null);
        });
    }
  }, [branch.path, animated]);

  useEffect(() => {
    if (!pathRef.current || !animated) return;

    const path = d3.select(pathRef.current);
    
    // Animate stroke width change
    path.transition()
      .duration(200)
      .ease(d3.easeBackOut)
      .attr('stroke-width', effectiveStrokeWidth);

  }, [effectiveStrokeWidth, animated]);

  const handleMouseEnter = (event: React.MouseEvent<SVGPathElement>) => {
    setIsHovered(true);
    onBranchHover?.(branch);

    if (showTooltip) {
      const rect = event.currentTarget.getBoundingClientRect();
      const svgRect = event.currentTarget.closest('svg')?.getBoundingClientRect();
      
      if (svgRect) {
        setTooltip({
          branch,
          x: rect.left + rect.width / 2 - svgRect.left,
          y: rect.top - svgRect.top - 10,
          visible: true
        });
      }
    }
  };

  const handleMouseLeave = () => {
    setIsHovered(false);
    onBranchHover?.(null);
    setTooltip(null);
  };

  const handleClick = () => {
    onBranchClick?.(branch);
  };

  const getBranchTypeIcon = (branchName: string): string => {
    const lowerName = branchName.toLowerCase();
    
    if (lowerName === 'main' || lowerName === 'master') {
      return '🏠';
    } else if (lowerName === 'develop' || lowerName === 'dev') {
      return '🚧';
    } else if (lowerName.startsWith('feature/') || lowerName.startsWith('feat/')) {
      return '✨';
    } else if (lowerName.startsWith('hotfix/') || lowerName.startsWith('fix/')) {
      return '🚨';
    } else if (lowerName.startsWith('release/') || lowerName.startsWith('rel/')) {
      return '🚀';
    }
    
    return '🌿';
  };

  const formatBranchName = (name: string): string => {
    // Remove common prefixes for display
    return name.replace(/^(feature|feat|hotfix|fix|release|rel)\//, '');
  };

  if (!branch.path) {
    return null;
  }

  return (
    <>
      {/* Branch Line */}
      <g className="branch-line">
        {/* Background line for better hover area */}
        <path
          d={branch.path}
          fill="none"
          stroke="transparent"
          strokeWidth={Math.max(effectiveStrokeWidth * 3, 10)}
          className="cursor-pointer"
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          onClick={handleClick}
        />

        {/* Selection/highlight background */}
        {(isSelected || isHighlighted) && (
          <path
            d={branch.path}
            fill="none"
            stroke={isSelected ? '#3b82f6' : '#f59e0b'}
            strokeWidth={effectiveStrokeWidth + 4}
            opacity={0.3}
            strokeDasharray={isHighlighted && !isSelected ? '8,4' : undefined}
          />
        )}

        {/* Main branch path */}
        <path
          ref={pathRef}
          d={branch.path}
          fill="none"
          stroke={branch.color}
          strokeWidth={effectiveStrokeWidth}
          strokeLinecap="round"
          strokeLinejoin="round"
          className={`
            transition-all duration-200
            ${isHovered ? 'drop-shadow-md' : ''}
          `}
          style={{
            filter: isHovered ? 'brightness(1.1)' : undefined
          }}
        />

        {/* Branch label at the end */}
        {branch.commits.length > 0 && (
          <g className="branch-label">
            <text
              x={branch.endX + 10}
              y={branch.endY + 4}
              className="text-xs font-medium fill-gray-700 dark:fill-gray-300 pointer-events-none"
              fontSize="11"
            >
              {formatBranchName(branch.branchName)}
            </text>
            
            {/* Branch type icon */}
            <text
              x={branch.endX + 10}
              y={branch.endY - 8}
              className="text-sm pointer-events-none"
              fontSize="12"
            >
              {getBranchTypeIcon(branch.branchName)}
            </text>
          </g>
        )}

        {/* Active branch indicator */}
        {branch.isActive && (
          <circle
            cx={branch.endX}
            cy={branch.endY}
            r={6}
            fill={branch.color}
            stroke="#ffffff"
            strokeWidth={2}
            className="animate-pulse"
          />
        )}
      </g>

      {/* Tooltip */}
      {tooltip && (
        <foreignObject
          x={tooltip.x - 100}
          y={tooltip.y - 60}
          width="200"
          height="60"
          className="pointer-events-none"
        >
          <div className="bg-gray-900 dark:bg-gray-800 text-white text-sm rounded-lg p-3 shadow-lg border border-gray-700">
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <span className="text-base">{getBranchTypeIcon(branch.branchName)}</span>
                <span className="font-medium">{branch.branchName}</span>
                {branch.isActive && (
                  <span className="px-2 py-1 bg-green-600 text-xs rounded">
                    ACTIVE
                  </span>
                )}
              </div>
              <div className="text-xs text-gray-400">
                {branch.commits.length} commit{branch.commits.length !== 1 ? 's' : ''}
                {branch.isMerged && (
                  <span className="ml-2 text-green-400">• Merged</span>
                )}
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

export default BranchLine; 