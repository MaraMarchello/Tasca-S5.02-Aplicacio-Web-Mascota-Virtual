import React from 'react';
import { useSearchParams } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import PetCreation from '../components/PetCreation';
import Shop from '../components/Shop';
import Achievements from '../components/Achievements';
import { PetHeader, PetTabs, PetInfo, PetRoomTab } from '../components/pet';
import { usePetData } from '../hooks/usePetData';

const PetPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const {
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
  } = usePetData();

  const [activeTab, setActiveTab] = React.useState<'pet' | 'room' | 'shop' | 'achievements'>(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam && ['pet', 'room', 'shop', 'achievements'].includes(tabParam)) {
      return tabParam as 'pet' | 'room' | 'shop' | 'achievements';
    }
    return 'pet';
  });

  const handleTabChange = (tab: 'pet' | 'room' | 'shop' | 'achievements') => {
    console.log('📱 Tab changed to:', tab);
    setActiveTab(tab);
    setSearchParams({ tab });
  };

  // Error state
  if (error) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="text-center space-y-4 max-w-2xl">
            <div className="text-red-500 text-6xl">⚠️</div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Something went wrong</h2>
            <p className="text-gray-600 dark:text-gray-400">{error}</p>
            <button
              onClick={() => {
                loadPetData();
              }}
              className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
            >
              Try Again
            </button>
          </div>
        </div>
      </Layout>
    );
  }

  // Loading state
  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading your pet...</p>
            <p className="text-sm text-gray-500 dark:text-gray-500">
              If this takes too long, try refreshing the page
            </p>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <PetHeader 
          pet={pet} 
          pointBalance={pointBalance} 
          onAwardTestPoints={handleAwardTestPoints} 
        />

        {!pet ? (
          /* Pet Creation */
          <div className="max-w-md mx-auto">
            <PetCreation onPetCreated={handlePetCreated} />
          </div>
        ) : (
          /* Main Pet Interface */
          <div>
            {/* Navigation Tabs */}
            <PetTabs activeTab={activeTab} onTabChange={handleTabChange} />

            {/* Tab Content */}
            <div>
              {activeTab === 'pet' && (
                <PetInfo
                  pet={pet}
                  pointBalance={pointBalance}
                  petItems={petItems}
                  onPetUpdate={setPet}
                  onFeedPet={handleFeedPet}
                  onAwardTestPoints={handleAwardTestPoints}
                  onLoadPetData={loadPetData}
                  onCheckDailyLogin={checkDailyLogin}
                  onDeletePet={handleDeletePet}
                />
              )}
              
              {activeTab === 'room' && (
                <PetRoomTab
                  pet={pet}
                  petItems={petItems}
                  onFeedPet={handleFeedPet}
                  onLoadPetData={loadPetData}
                />
              )}
              
              {activeTab === 'shop' && (
                <Shop
                  pointBalance={pointBalance}
                  onPointsUpdate={handlePointsUpdate}
                />
              )}
              
              {activeTab === 'achievements' && (
                <Achievements />
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default PetPage; 