import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import { MergeConnection } from '../../../types/visualization';

interface MergeVisualizationProps {
  connection: MergeConnection;
  isSelected?: boolean;
  isHighlighted?: boolean;
  onConnectionClick?: (connection: MergeConnection) => void;
  onConnectionHover?: (connection: MergeConnection | null) => void;
  showTooltip?: boolean;
  animated?: boolean;
}

const MergeVisualization: React.FC<MergeVisualizationProps> = ({
  connection,
  isSelected = false,
  isHighlighted = false,
  onConnectionClick,
  onConnectionHover,
  showTooltip = true,
  animated = true
}) => {
  const pathRef = useRef<SVGPathElement>(null);
  const arrowRef = useRef<SVGPolygonElement>(null);
  const [isHovered, setIsHovered] = useState(false);
  const [animationProgress, setAnimationProgress] = useState(0);

  useEffect(() => {
    if (!pathRef.current || !animated) return;

    const path = pathRef.current;
    const length = path.getTotalLength();

    if (connection.animated) {
      // Animated drawing effect
      const startTime = Date.now();
      const duration = 1000;

      const animate = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // Easing function
        const easeInOutCubic = (t: number) => {
          return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        };

        const easedProgress = easeInOutCubic(progress);
        setAnimationProgress(easedProgress);

        // Animate the path drawing
        const offset = length * (1 - easedProgress);
        d3.select(path)
          .attr('stroke-dasharray', `${length} ${length}`)
          .attr('stroke-dashoffset', offset);

        if (progress < 1) {
          requestAnimationFrame(animate);
        } else {
          // Remove dash array after animation
          d3.select(path).attr('stroke-dasharray', null);
          setAnimationProgress(1);
        }
      };

      requestAnimationFrame(animate);
    } else {
      setAnimationProgress(1);
    }
  }, [connection.animated, animated]);

  const handleMouseEnter = () => {
    setIsHovered(true);
    onConnectionHover?.(connection);
  };

  const handleMouseLeave = () => {
    setIsHovered(false);
    onConnectionHover?.(null);
  };

  const handleClick = () => {
    onConnectionClick?.(connection);
  };

  const getConnectionIcon = (type: MergeConnection['type']): string => {
    switch (type) {
      case 'merge': return '🔀';
      case 'rebase': return '📐';
      case 'cherry-pick': return '🍒';
      default: return '🔗';
    }
  };

  const getConnectionColor = (type: MergeConnection['type']): string => {
    switch (type) {
      case 'merge': return '#10b981'; // emerald-500
      case 'rebase': return '#f59e0b'; // amber-500
      case 'cherry-pick': return '#ef4444'; // red-500
      default: return '#6b7280'; // gray-500
    }
  };

  // Calculate arrow position and rotation
  const midX = (connection.fromCommit.x + connection.toCommit.x) / 2;
  const midY = (connection.fromCommit.y + connection.toCommit.y) / 2;
  const angle = Math.atan2(
    connection.toCommit.y - connection.fromCommit.y,
    connection.toCommit.x - connection.fromCommit.x
  ) * (180 / Math.PI);

  const arrowSize = 8;
  const arrowPoints = [
    [0, -arrowSize / 2],
    [arrowSize, 0],
    [0, arrowSize / 2]
  ].map(([x, y]) => `${x},${y}`).join(' ');

  const strokeWidth = isSelected ? 4 : isHighlighted ? 3 : 2;
  const opacity = animationProgress * (isHovered ? 0.9 : 0.7);

  return (
    <>
      {/* Merge Connection */}
      <g className="merge-connection">
        {/* Background line for better hover area */}
        <path
          d={connection.path}
          fill="none"
          stroke="transparent"
          strokeWidth={strokeWidth * 3}
          className="cursor-pointer"
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          onClick={handleClick}
        />

        {/* Selection/highlight background */}
        {(isSelected || isHighlighted) && (
          <path
            d={connection.path}
            fill="none"
            stroke={isSelected ? '#3b82f6' : '#f59e0b'}
            strokeWidth={strokeWidth + 2}
            opacity={0.4}
            strokeDasharray={isHighlighted && !isSelected ? '6,3' : undefined}
          />
        )}

        {/* Main connection path */}
        <path
          ref={pathRef}
          d={connection.path}
          fill="none"
          stroke={getConnectionColor(connection.type)}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeLinejoin="round"
          opacity={opacity}
          className={`
            transition-all duration-200
            ${isHovered ? 'drop-shadow-md' : ''}
          `}
          style={{
            filter: isHovered ? 'brightness(1.2)' : undefined
          }}
        />

        {/* Arrow indicator */}
        {animationProgress > 0.5 && (
          <polygon
            ref={arrowRef}
            points={arrowPoints}
            fill={getConnectionColor(connection.type)}
            opacity={opacity}
            transform={`translate(${midX}, ${midY}) rotate(${angle})`}
            className="transition-all duration-200"
          />
        )}

        {/* Connection type indicator */}
        {animationProgress > 0.7 && (
          <g className="connection-indicator">
            <circle
              cx={midX}
              cy={midY - 20}
              r={12}
              fill="white"
              stroke={getConnectionColor(connection.type)}
              strokeWidth={2}
              opacity={opacity}
            />
            <text
              x={midX}
              y={midY - 16}
              textAnchor="middle"
              className="text-xs pointer-events-none"
              fontSize="10"
              opacity={opacity}
            >
              {getConnectionIcon(connection.type)}
            </text>
          </g>
        )}

        {/* Animated flow particles (for active animations) */}
        {connection.animated && animationProgress < 1 && (
          <g className="flow-particles">
            {[0.3, 0.6, 0.9].map((offset, index) => {
              const progress = (animationProgress + offset) % 1;
              const pathElement = pathRef.current;
              
              if (!pathElement) return null;
              
              const length = pathElement.getTotalLength();
              const point = pathElement.getPointAtLength(length * progress);
              
              return (
                <circle
                  key={index}
                  cx={point.x}
                  cy={point.y}
                  r={3}
                  fill={getConnectionColor(connection.type)}
                  opacity={0.8}
                  className="animate-pulse"
                />
              );
            })}
          </g>
        )}
      </g>

      {/* Tooltip */}
      {showTooltip && isHovered && (
        <foreignObject
          x={midX - 100}
          y={midY - 80}
          width="200"
          height="60"
          className="pointer-events-none"
        >
          <div className="bg-gray-900 dark:bg-gray-800 text-white text-sm rounded-lg p-3 shadow-lg border border-gray-700">
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <span className="text-base">{getConnectionIcon(connection.type)}</span>
                <span className="font-medium capitalize">{connection.type}</span>
              </div>
              <div className="text-xs text-gray-400">
                <div>{connection.fromBranch} → {connection.toBranch}</div>
                <div className="mt-1">
                  <span className="font-mono">{connection.fromCommit.commitHash.substring(0, 7)}</span>
                  {' → '}
                  <span className="font-mono">{connection.toCommit.commitHash.substring(0, 7)}</span>
                </div>
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

export default MergeVisualization; 