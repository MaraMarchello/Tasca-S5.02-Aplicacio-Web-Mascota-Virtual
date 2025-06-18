import React, { useState, useEffect } from 'react';
import { clsx } from 'clsx';

interface PetCharacterProps {
  petType: 'CAT' | 'DOG' | 'BIRD' | 'FISH';
  happiness: number;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  skin?: string;
  accessories?: string[];
  isIdle?: boolean;
  emotion?: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry';
  onClick?: () => void;
  className?: string;
}

const PetCharacter: React.FC<PetCharacterProps> = ({
  petType,
  happiness,
  size = 'md',
  skin = 'default',
  accessories = [],
  isIdle = true,
  emotion,
  onClick,
  className
}) => {
  const [currentAnimation, setCurrentAnimation] = useState<'idle' | 'bounce' | 'wiggle'>('idle');
  
  // Auto-determine emotion based on happiness if not provided
  const petEmotion = emotion || (() => {
    if (happiness >= 80) return 'happy';
    if (happiness >= 60) return 'excited';
    if (happiness >= 40) return 'happy';
    if (happiness >= 20) return 'sad';
    return 'hungry';
  })();

  // Size configurations
  const sizeClasses = {
    sm: 'w-16 h-16',
    md: 'w-24 h-24',
    lg: 'w-32 h-32',
    xl: 'w-48 h-48'
  };

  // Animation cycle for idle pets
  useEffect(() => {
    if (!isIdle) return;

    const animationCycle = () => {
      const animations: ('idle' | 'bounce' | 'wiggle')[] = ['idle', 'bounce', 'wiggle'];
      const randomAnimation = animations[Math.floor(Math.random() * animations.length)];
      setCurrentAnimation(randomAnimation);
      
      // Return to idle after animation
      setTimeout(() => setCurrentAnimation('idle'), 1000);
    };

    const interval = setInterval(animationCycle, 3000 + Math.random() * 2000);
    return () => clearInterval(interval);
  }, [isIdle]);

  // Cat SVG Component
  const CatCharacter = () => (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      {/* Cat Body */}
      <ellipse cx="50" cy="70" rx="20" ry="15" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Cat Head */}
      <circle cx="50" cy="40" r="18" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Cat Ears */}
      <polygon points="35,25 40,35 45,25" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      <polygon points="55,25 60,35 65,25" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      <polygon points="37,27 40,32 43,27" fill="#ff69b4"/>
      <polygon points="57,27 60,32 63,27" fill="#ff69b4"/>
      
      {/* Eyes */}
      <circle cx="44" cy="38" r="3" fill="#000"/>
      <circle cx="56" cy="38" r="3" fill="#000"/>
      {petEmotion === 'happy' && (
        <>
          <circle cx="45" cy="37" r="1" fill="#fff"/>
          <circle cx="57" cy="37" r="1" fill="#fff"/>
        </>
      )}
      {petEmotion === 'sad' && (
        <>
          <path d="M41 40 Q44 43 47 40" stroke="#333" strokeWidth="1" fill="none"/>
          <path d="M53 40 Q56 43 59 40" stroke="#333" strokeWidth="1" fill="none"/>
        </>
      )}
      
      {/* Nose */}
      <polygon points="48,42 52,42 50,45" fill="#ff69b4"/>
      
      {/* Mouth */}
      {petEmotion === 'happy' && (
        <path d="M46 47 Q50 50 54 47" stroke="#333" strokeWidth="1" fill="none"/>
      )}
      {petEmotion === 'sad' && (
        <path d="M46 49 Q50 46 54 49" stroke="#333" strokeWidth="1" fill="none"/>
      )}
      
      {/* Whiskers */}
      <line x1="30" y1="42" x2="40" y2="40" stroke="#333" strokeWidth="1"/>
      <line x1="30" y1="46" x2="40" y2="46" stroke="#333" strokeWidth="1"/>
      <line x1="60" y1="40" x2="70" y2="42" stroke="#333" strokeWidth="1"/>
      <line x1="60" y1="46" x2="70" y2="46" stroke="#333" strokeWidth="1"/>
      
      {/* Tail */}
      <path d="M70 65 Q80 50 75 35" stroke="#333" strokeWidth="3" fill="none" strokeLinecap="round"/>
      
      {/* Accessories */}
      {accessories.includes('bow') && (
        <g>
          <polygon points="42,20 48,15 54,20 48,25" fill="#ff1493"/>
          <circle cx="48" cy="20" r="2" fill="#ff69b4"/>
        </g>
      )}
    </svg>
  );

  // Dog SVG Component
  const DogCharacter = () => (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      {/* Dog Body */}
      <ellipse cx="50" cy="70" rx="22" ry="16" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Dog Head */}
      <ellipse cx="50" cy="40" rx="16" ry="18" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Dog Ears */}
      <ellipse cx="38" cy="30" rx="6" ry="12" fill="#8B4513" stroke="#333" strokeWidth="1"/>
      <ellipse cx="62" cy="30" rx="6" ry="12" fill="#8B4513" stroke="#333" strokeWidth="1"/>
      
      {/* Snout */}
      <ellipse cx="50" cy="48" rx="8" ry="6" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Eyes */}
      <circle cx="45" cy="36" r="3" fill="#000"/>
      <circle cx="55" cy="36" r="3" fill="#000"/>
      {petEmotion === 'happy' && (
        <>
          <circle cx="46" cy="35" r="1" fill="#fff"/>
          <circle cx="56" cy="35" r="1" fill="#fff"/>
        </>
      )}
      
      {/* Nose */}
      <circle cx="50" cy="46" r="2" fill="#000"/>
      
      {/* Mouth */}
      {petEmotion === 'happy' && (
        <path d="M44 52 Q50 56 56 52" stroke="#333" strokeWidth="2" fill="none"/>
      )}
      
      {/* Tongue (when happy) */}
      {petEmotion === 'happy' && (
        <ellipse cx="50" cy="54" rx="3" ry="6" fill="#ff69b4"/>
      )}
      
      {/* Tail */}
      <path d="M72 68 Q85 55 80 40" stroke="#333" strokeWidth="4" fill="none" strokeLinecap="round"/>
      
      {/* Spots */}
      {skin === 'spotted' && (
        <>
          <circle cx="40" cy="65" r="3" fill="#8B4513"/>
          <circle cx="60" cy="72" r="4" fill="#8B4513"/>
          <circle cx="45" cy="32" r="2" fill="#8B4513"/>
        </>
      )}
    </svg>
  );

  // Bird SVG Component
  const BirdCharacter = () => (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      {/* Bird Body */}
      <ellipse cx="50" cy="60" rx="18" ry="25" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Bird Head */}
      <circle cx="50" cy="35" r="15" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Beak */}
      <polygon points="50,30 45,25 50,20" fill="#FFA500" stroke="#333" strokeWidth="1"/>
      
      {/* Eyes */}
      <circle cx="46" cy="32" r="3" fill="#000"/>
      <circle cx="54" cy="32" r="3" fill="#000"/>
      <circle cx="47" cy="31" r="1" fill="#fff"/>
      <circle cx="55" cy="31" r="1" fill="#fff"/>
      
      {/* Wings */}
      <ellipse cx="35" cy="55" rx="8" ry="20" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      <ellipse cx="65" cy="55" rx="8" ry="20" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Tail Feathers */}
      <ellipse cx="50" cy="85" rx="12" ry="8" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Feet */}
      <line x1="45" y1="85" x2="42" y2="92" stroke="#FFA500" strokeWidth="2"/>
      <line x1="55" y1="85" x2="58" y2="92" stroke="#FFA500" strokeWidth="2"/>
      <line x1="42" y1="92" x2="38" y2="90" stroke="#FFA500" strokeWidth="2"/>
      <line x1="58" y1="92" x2="62" y2="90" stroke="#FFA500" strokeWidth="2"/>
    </svg>
  );

  // Fish SVG Component
  const FishCharacter = () => (
    <svg viewBox="0 0 100 100" className="w-full h-full">
      {/* Fish Body */}
      <ellipse cx="50" cy="50" rx="25" ry="15" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Fish Tail */}
      <polygon points="25,50 15,40 15,60" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Fish Fins */}
      <ellipse cx="50" cy="35" rx="8" ry="4" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      <ellipse cx="50" cy="65" rx="8" ry="4" fill={getSkinColor()} stroke="#333" strokeWidth="1"/>
      
      {/* Eyes */}
      <circle cx="60" cy="45" r="4" fill="#fff" stroke="#333" strokeWidth="1"/>
      <circle cx="60" cy="45" r="2" fill="#000"/>
      <circle cx="61" cy="44" r="1" fill="#fff"/>
      
      {/* Mouth */}
      <ellipse cx="70" cy="50" rx="3" ry="2" fill="none" stroke="#333" strokeWidth="1"/>
      
      {/* Scales */}
      {Array.from({ length: 6 }).map((_, i) => (
        <circle key={i} cx={45 + (i % 3) * 8} cy={45 + Math.floor(i / 3) * 8} r="2" fill="none" stroke="#333" strokeWidth="0.5"/>
      ))}
      
      {/* Bubbles */}
      <circle cx="75" cy="30" r="2" fill="#87CEEB" opacity="0.7"/>
      <circle cx="80" cy="25" r="1.5" fill="#87CEEB" opacity="0.7"/>
      <circle cx="85" cy="20" r="1" fill="#87CEEB" opacity="0.7"/>
    </svg>
  );

  const getSkinColor = () => {
    const skinColors = {
      default: petType === 'CAT' ? '#FFA500' : petType === 'DOG' ? '#D2691E' : petType === 'BIRD' ? '#FFD700' : '#FF6347',
      white: '#F5F5F5',
      black: '#2F2F2F',
      brown: '#8B4513',
      gray: '#808080',
      spotted: '#D2691E'
    };
    return skinColors[skin as keyof typeof skinColors] || skinColors.default;
  };

  const renderPetCharacter = () => {
    switch (petType) {
      case 'CAT': return <CatCharacter />;
      case 'DOG': return <DogCharacter />;
      case 'BIRD': return <BirdCharacter />;
      case 'FISH': return <FishCharacter />;
      default: return <CatCharacter />;
    }
  };

  return (
    <div
      className={clsx(
        'relative cursor-pointer transition-transform duration-300',
        sizeClasses[size],
        {
          'animate-bounce': currentAnimation === 'bounce',
          'animate-pulse': currentAnimation === 'wiggle',
          'hover:scale-110': onClick,
        },
        className
      )}
      onClick={onClick}
      style={{
        animation: currentAnimation === 'wiggle' ? 'wiggle 0.5s ease-in-out' : undefined,
      }}
    >
      {renderPetCharacter()}
      
      {/* Happiness indicator */}
      <div className="absolute -top-2 -right-2 w-4 h-4 rounded-full bg-white border-2 border-gray-300 flex items-center justify-center text-xs">
        {happiness >= 80 ? '😊' : happiness >= 60 ? '🙂' : happiness >= 40 ? '😐' : happiness >= 20 ? '😟' : '😢'}
      </div>
      
      {/* Achievement reaction */}
      {petEmotion === 'excited' && (
        <div className="absolute -top-6 left-1/2 transform -translate-x-1/2 animate-bounce">
          <span className="text-yellow-500 text-lg">⭐</span>
        </div>
      )}
    </div>
  );
};

export default PetCharacter; 