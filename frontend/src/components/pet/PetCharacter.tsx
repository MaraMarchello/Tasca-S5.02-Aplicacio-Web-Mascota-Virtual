import React, { useState } from 'react';
import { getPetImage, getPetEmoji } from '../../utils/petUtils';

interface PetCharacterProps {
  petType: 'CAT' | 'DOG' | 'BIRD' | 'FISH' | 'DUKE_JAVA' | 'COFFEE_BEAN' | 'CODEMATE_MASCOT';
  happiness: number;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  skin?: string;
  accessories?: string[];
  isIdle?: boolean;
  emotion?: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry';
  onClick?: () => void;
  className?: string;
  customImage?: string;
}

const PetCharacter: React.FC<PetCharacterProps> = ({
  petType,
  happiness,
  size = 'md',
  onClick,
  className = '',
  customImage
}) => {
  const [imageError, setImageError] = useState(false);
  
  // Size configurations
  const sizeClasses = {
    sm: 'w-16 h-16 text-2xl',
    md: 'w-24 h-24 text-4xl',
    lg: 'w-32 h-32 text-6xl',
    xl: 'w-48 h-48 text-8xl'
  };

  // Get pet emoji for fallback
  const getPetEmojiForDisplay = () => {
    return getPetEmoji(petType);
  };

  // Get appropriate image for special pet types
  const getSpecialPetImage = () => {
    if (customImage && !imageError) {
      return customImage;
    }

    try {
      return getPetImage(petType, happiness);
    } catch (error) {
      console.error('Error getting pet image:', error);
      return null;
    }
  };

  const specialImage = getSpecialPetImage();

  // If we have a special image, render it
  if (specialImage && !imageError) {
    return (
      <div 
        className={`relative flex items-center justify-center cursor-pointer transition-transform hover:scale-105 ${sizeClasses[size]} ${className}`}
        onClick={onClick}
      >
        <img 
          src={specialImage}
          alt={`${petType} pet`}
          className="w-full h-full object-contain rounded-lg"
          onError={() => setImageError(true)}
        />
        {/* Happiness indicator */}
        <div className="absolute -bottom-1 -right-1 text-sm bg-white rounded-full w-6 h-6 flex items-center justify-center shadow-sm">
          {happiness >= 80 ? '😊' : happiness >= 60 ? '🙂' : happiness >= 40 ? '😐' : happiness >= 20 ? '😟' : '😢'}
        </div>
      </div>
    );
  }

  // Fallback to emoji rendering
  return (
    <div 
      className={`relative flex items-center justify-center cursor-pointer transition-transform hover:scale-105 bg-gray-50 dark:bg-gray-800 rounded-xl border-2 border-gray-200 dark:border-gray-700 ${sizeClasses[size]} ${className}`}
      onClick={onClick}
      title={`${petType} - Happiness: ${happiness}%`}
    >
      <span className="select-none">
        {getPetEmojiForDisplay()}
      </span>
      
      {/* Happiness indicator */}
      <div className="absolute -bottom-1 -right-1 text-xs bg-white dark:bg-gray-800 rounded-full w-6 h-6 flex items-center justify-center shadow-sm border">
        {happiness >= 80 ? '😊' : happiness >= 60 ? '🙂' : happiness >= 40 ? '😐' : happiness >= 20 ? '😟' : '😢'}
      </div>
    </div>
  );
};

export default PetCharacter;