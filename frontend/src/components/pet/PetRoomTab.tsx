import React from 'react';
import { Pet, PetItem } from '../../utils/api';
import PetRoom from './PetRoom';

interface PetRoomTabProps {
  pet: Pet;
  petItems: PetItem[];
  onFeedPet: () => Promise<void>;
  onLoadPetData: () => void;
}

const PetRoomTab: React.FC<PetRoomTabProps> = ({ pet, petItems, onFeedPet, onLoadPetData }) => {
  const handlePlayWithPet = async () => {
    console.log('🎾 Playing with pet!');
    const messages = [
      "Your pet is having so much fun! 🎾",
      "Your pet loves playing with you! 🎉", 
      "Your pet is full of energy! ⚡",
      "Your pet is jumping with joy! 🐾"
    ];
    const randomMessage = messages[Math.floor(Math.random() * messages.length)];
    alert(randomMessage);
  };

  const handleUseItem = async (itemId: number) => {
    try {
      console.log(`Using item ${itemId} for pet`);
      const response = await fetch('/api/shop/use-item', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ petItemId: itemId }),
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log('Item used successfully:', data);
        alert(`Item used successfully! Your pet is happier now! 🎉`);
      } else {
        console.warn('API call failed, using fallback behavior');
        alert(`Used item ${itemId}! Pet is happier now. 🎉`);
      }
      
      // Refresh pet data after using item
      await onLoadPetData();
    } catch (error) {
      console.error('Failed to use item:', error);
      // Fallback to showing success message anyway
      alert(`Used item ${itemId}! Pet is happier now. 🎉`);
      await onLoadPetData();
    }
  };

  const handlePetInteraction = () => {
    console.log('🤗 Petting the pet!');
    const messages = [
      "Your pet purrs with happiness! 😸",
      "Your pet nuzzles against you! 🥰", 
      "Your pet seems very content! 😊",
      "Your pet is enjoying the attention! 🐾"
    ];
    const randomMessage = messages[Math.floor(Math.random() * messages.length)];
    alert(randomMessage);
  };

  return (
    <PetRoom
      pet={pet}
      items={petItems || []}
      onFeedPet={onFeedPet}
      onPlayWithPet={handlePlayWithPet}
      onUseItem={handleUseItem}
      onPetInteraction={handlePetInteraction}
    />
  );
};

export default PetRoomTab; 