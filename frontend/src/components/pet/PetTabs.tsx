import React from 'react';

type TabType = 'pet' | 'room' | 'shop' | 'achievements';

interface PetTabsProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

const PetTabs: React.FC<PetTabsProps> = ({ activeTab, onTabChange }) => {
  const tabs = [
    { id: 'pet' as const, label: '🐾 Pet' },
    { id: 'room' as const, label: '🏠 Room' },
    { id: 'shop' as const, label: '🛍️ Shop' },
    { id: 'achievements' as const, label: '🏆 Achievements' },
  ];

  return (
    <div className="mb-8">
      <div className="border-b border-gray-200 dark:border-gray-700">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>
    </div>
  );
};

export default PetTabs; 