import { useState, useEffect, useRef } from 'react';
import { petApi, pointsApi, shopApi, Pet, PointBalance, PetItem } from '../utils/api';

// Local storage keys
const BALANCE_STORAGE_KEY = 'codemate_point_balance';
const BALANCE_TIMESTAMP_KEY = 'codemate_balance_timestamp';

// Helper function to normalize API response to frontend interface
const normalizeBalance = (apiResponse: any): PointBalance | null => {
  try {
    // Handle API response with currentPoints
    if (apiResponse && typeof apiResponse.currentPoints === 'number') {
      return {
        currentBalance: apiResponse.currentPoints,
        totalEarned: apiResponse.totalEarned || 0,
        totalSpent: apiResponse.totalSpent || 0
      };
    }
    
    // Handle already normalized balance
    if (apiResponse && typeof apiResponse.currentBalance === 'number') {
      return {
        currentBalance: apiResponse.currentBalance,
        totalEarned: apiResponse.totalEarned || 0,
        totalSpent: apiResponse.totalSpent || 0
      };
    }
    
    console.error('❌ Invalid balance structure:', apiResponse);
    return null;
  } catch (error) {
    console.error('❌ Error normalizing balance:', error);
    return null;
  }
};

// Helper functions for localStorage backup
const saveBalanceToStorage = (balance: PointBalance) => {
  try {
    localStorage.setItem(BALANCE_STORAGE_KEY, JSON.stringify(balance));
    localStorage.setItem(BALANCE_TIMESTAMP_KEY, Date.now().toString());
    console.log('💾 Balance saved to localStorage:', balance);
  } catch (error) {
    console.error('❌ Failed to save balance to localStorage:', error);
  }
};

const loadBalanceFromStorage = (): PointBalance | null => {
  try {
    const stored = localStorage.getItem(BALANCE_STORAGE_KEY);
    const timestamp = localStorage.getItem(BALANCE_TIMESTAMP_KEY);
    
    if (stored && timestamp) {
      const balance = JSON.parse(stored);
      const age = Date.now() - parseInt(timestamp);
      
      // Only use stored balance if it's less than 5 minutes old
      if (age < 5 * 60 * 1000 && balance.currentBalance >= 0) {
        console.log('💾 Loaded balance from localStorage:', balance);
        return balance;
      }
    }
  } catch (error) {
    console.error('❌ Failed to load balance from localStorage:', error);
  }
  return null;
};

export const usePetData = () => {
  const [pet, setPet] = useState<Pet | null>(null);
  
  // Initialize with localStorage backup or defaults
  const [pointBalance, setPointBalance] = useState<PointBalance>(() => {
    const stored = loadBalanceFromStorage();
    return stored || { currentBalance: 0, totalEarned: 0, totalSpent: 0 };
  });
  
  const [petItems, setPetItems] = useState<PetItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dailyLoginChecked, setDailyLoginChecked] = useState(false);
  
  // Use refs to prevent multiple simultaneous API calls
  const loadingRef = useRef(false);
  const mountedRef = useRef(true);

  // Safe balance setter that always saves to localStorage
  const setPointBalanceSafe = (balance: PointBalance | ((prev: PointBalance) => PointBalance)) => {
    if (!mountedRef.current) return;
    
    setPointBalance(prev => {
      const newBalance = typeof balance === 'function' ? balance(prev) : balance;
      
      // Validate balance before setting
      if (newBalance && 
          typeof newBalance.currentBalance === 'number' && 
          newBalance.currentBalance >= 0 &&
          typeof newBalance.totalEarned === 'number' &&
          typeof newBalance.totalSpent === 'number') {
        console.log('✅ Valid balance accepted:', newBalance);
        saveBalanceToStorage(newBalance);
        return newBalance;
      }
      
      console.error('❌ Invalid balance rejected. Expected: {currentBalance: number, totalEarned: number, totalSpent: number}, got:', newBalance);
      return prev;
    });
  };

  const loadPetData = async () => {
    // Prevent multiple simultaneous loads
    if (loadingRef.current) {
      console.log('⏳ Load already in progress, skipping...');
      return;
    }
    
    loadingRef.current = true;
    setLoading(true);
    setError(null);
    
    try {
      console.log('🔄 Loading pet data...');
      
      const petResponse = await petApi.getUserPet().catch((err) => {
        console.error('❌ Pet API error:', err);
        return { success: false, data: null, error: err.message };
      });
      
      const balanceResponse = await pointsApi.getBalance().catch((err) => {
        console.error('❌ Balance API error:', err);
        return { success: false, data: null, error: err.message };
      });
      
      const itemsResponse = await shopApi.getUserItems().catch((err) => {
        console.error('❌ Items API error:', err);
        return { success: false, data: [], error: err.message };
      });

      console.log('🐾 Pet response:', petResponse);
      console.log('💰 Balance response:', balanceResponse);
      console.log('🎒 Items response:', itemsResponse);

      if (petResponse.success && petResponse.data) {
        console.log('✅ Pet loaded successfully:', petResponse.data);
        setPet(petResponse.data);
      } else {
        console.log('ℹ️ No pet found or pet API failed');
      }

      if (balanceResponse.success && balanceResponse.data) {
        console.log('✅ Balance loaded successfully:', balanceResponse.data);
        const normalizedBalance = normalizeBalance(balanceResponse.data);
        if (normalizedBalance) {
          setPointBalanceSafe(normalizedBalance);
        } else {
          console.error('❌ Failed to normalize balance response');
        }
      } else {
        console.log('⚠️ Balance API failed, preserving existing balance');
        // Do nothing - keep existing balance (including localStorage backup)
      }

      if (itemsResponse.success && itemsResponse.data) {
        console.log('✅ Items loaded successfully:', itemsResponse.data);
        setPetItems(itemsResponse.data);
      } else {
        console.log('⚠️ Items API failed');
        setPetItems([]);
      }
      
    } catch (error) {
      console.error('❌ Fatal error loading pet data:', error);
      setError(`Failed to load pet data: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      loadingRef.current = false;
      setLoading(false);
      console.log('✅ Pet data loading complete');
    }
  };

  const checkDailyLogin = async () => {
    if (dailyLoginChecked) return;
    
    try {
      console.log('🔄 Checking daily login...');
      const response = await pointsApi.checkDailyLogin();
      if (response.success && response.data && response.data.amount > 0) {
        console.log('🎉 Daily login bonus received:', response.data.amount);
        setPointBalanceSafe(prev => ({
          ...prev,
          currentBalance: prev.currentBalance + response.data!.amount,
          totalEarned: prev.totalEarned + response.data!.amount
        }));
      }
    } catch (error) {
      console.error('❌ Daily login check failed:', error);
    } finally {
      setDailyLoginChecked(true);
    }
  };

  const handlePetCreated = (newPet: Pet) => {
    console.log('🎉 New pet created:', newPet);
    setPet(newPet);
    loadPetData();
  };

  const handlePointsUpdate = (updatedBalance: PointBalance | any) => {
    console.log('💰 Points updated:', updatedBalance);
    const normalizedBalance = normalizeBalance(updatedBalance);
    if (normalizedBalance) {
      setPointBalanceSafe(normalizedBalance);
    } else {
      console.error('❌ Failed to normalize balance update');
    }
  };

  const handleFeedPet = async () => {
    if (!pet) return;
    
    try {
      const response = await petApi.feedPet();
      if (response.success && response.data) {
        setPet(response.data);
        console.log('🍽️ Pet fed successfully');
      }
    } catch (error) {
      console.error('❌ Failed to feed pet:', error);
    }
  };

  const handleAwardTestPoints = async () => {
    try {
      await pointsApi.awardTestPoints(100);
      console.log('✅ Test points awarded');
      loadPetData(); // Refresh to get updated balance
    } catch (error) {
      console.error('❌ Failed to award test points:', error);
    }
  };

  const handleDeletePet = async () => {
    if (!pet) {
      console.warn('⚠️ No pet to delete');
      return;
    }
    
    try {
      console.log('🗑️ Deleting pet:', pet.name);
      const response = await petApi.deletePet();
      
      if (response.success) {
        console.log('✅ Pet deleted successfully');
        // Reset pet state
        setPet(null);
        // Reload data to ensure consistency
        loadPetData();
      } else {
        console.error('❌ Failed to delete pet:', response.message);
        throw new Error(response.message || 'Failed to delete pet');
      }
    } catch (error) {
      console.error('❌ Error deleting pet:', error);
      throw error; // Re-throw so calling component can handle the error
    }
  };

  useEffect(() => {
    mountedRef.current = true;
    loadPetData();
    checkDailyLogin();
    
    return () => {
      mountedRef.current = false;
    };
  }, []);

  return {
    pet,
    setPet,
    pointBalance,
    petItems,
    loading,
    error,
    loadPetData,
    checkDailyLogin,
    handlePetCreated,
    handlePointsUpdate,
    handleFeedPet,
    handleAwardTestPoints,
    handleDeletePet
  };
}; 