import React, { useState, useEffect } from 'react';
import { shopApi, pointsApi, ItemTemplate, PetItem, PointBalance, Pet } from '../utils/api';

interface ShopProps {
  pointBalance: PointBalance;
  onPointsUpdate: (balance: PointBalance) => void;
  onPetUpdate?: (pet: Pet) => void;
}

const Shop: React.FC<ShopProps> = ({ pointBalance, onPointsUpdate, onPetUpdate }) => {
  const [items, setItems] = useState<ItemTemplate[]>([]);
  const [userItems, setUserItems] = useState<PetItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'shop' | 'inventory'>('shop');
  const [purchasing, setPurchasing] = useState<number | null>(null);
  const [using, setUsing] = useState<number | null>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [itemsResponse, userItemsResponse] = await Promise.all([
        shopApi.getItems(),
        shopApi.getUserItems()
      ]);

      if (itemsResponse.success && itemsResponse.data) {
        setItems(itemsResponse.data);
      }

      if (userItemsResponse.success && userItemsResponse.data) {
        setUserItems(userItemsResponse.data);
      }
    } catch (error) {
      console.error('Failed to load shop data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePurchase = async (itemId: number, price: number) => {
    if (pointBalance.currentBalance < price) {
      setMessage('Not enough points to purchase this item!');
      setTimeout(() => setMessage(''), 3000);
      return;
    }

    setPurchasing(itemId);
    setMessage('');

    try {
      const response = await shopApi.purchaseItem({
        itemTemplateId: itemId,
        quantity: 1
      });

      if (response.success) {
        setMessage('Item purchased successfully! 🎉');
        
        // Refresh data
        await loadData();
        const balanceResponse = await pointsApi.getBalance();
        if (balanceResponse.success && balanceResponse.data) {
          onPointsUpdate(balanceResponse.data);
        }
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Failed to purchase item');
    } finally {
      setPurchasing(null);
      setTimeout(() => setMessage(''), 3000);
    }
  };

  const handleUseItem = async (petItemId: number, itemType: string) => {
    setUsing(petItemId);
    setMessage('');

    try {
      if (itemType === 'FOOD') {
        const response = await shopApi.useItem({ petItemId });
        if (response.success && response.data && onPetUpdate) {
          onPetUpdate(response.data);
          setMessage('Your pet enjoyed the food! 🍽️');
        }
      } else if (itemType === 'ACCESSORY') {
        const item = userItems.find(item => item.id === petItemId);
        if (item?.isEquipped) {
          await shopApi.unequipItem(petItemId);
          setMessage('Item unequipped! 👕');
        } else {
          await shopApi.equipItem(petItemId);
          setMessage('Item equipped! ✨');
        }
      }

      // Refresh inventory
      await loadData();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Failed to use item');
    } finally {
      setUsing(null);
      setTimeout(() => setMessage(''), 3000);
    }
  };

  const getItemTypeIcon = (type: string) => {
    switch (type) {
      case 'FOOD': return '🍽️';
      case 'ACCESSORY': return '👔';
      default: return '📦';
    }
  };

  const getItemIcon = (name: string) => {
    const iconMap: { [key: string]: string } = {
      'Coffee Bean': '☕',
      'Energy Drink': '⚡',
      'Healthy Snack': '🥜',
      'Premium Food': '🍖',
      'Coding Hat': '🎩',
      'Lucky Charm': '🍀'
    };
    return iconMap[name] || '📦';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading shop...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md">
      {/* Header */}
      <div className="border-b border-gray-200">
        <div className="p-4">
          <h2 className="text-xl font-bold text-gray-800">Pet Shop</h2>
          <p className="text-sm text-gray-600">Current Points: <span className="font-semibold text-green-600">{pointBalance.currentBalance}</span></p>
        </div>
        
        {/* Tabs */}
        <div className="flex">
          <button
            onClick={() => setActiveTab('shop')}
            className={`flex-1 py-3 px-4 text-center font-medium transition-colors ${
              activeTab === 'shop'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            🛒 Shop
          </button>
          <button
            onClick={() => setActiveTab('inventory')}
            className={`flex-1 py-3 px-4 text-center font-medium transition-colors ${
              activeTab === 'inventory'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            🎒 Inventory ({userItems.length})
          </button>
        </div>
      </div>

      {/* Message */}
      {message && (
        <div className={`p-3 text-center text-sm ${
          message.includes('Failed') || message.includes('Not enough') 
            ? 'bg-red-50 text-red-600' 
            : 'bg-green-50 text-green-600'
        }`}>
          {message}
        </div>
      )}

      {/* Content */}
      <div className="p-4">
        {activeTab === 'shop' ? (
          <div className="grid gap-4">
            {items.map((item) => (
              <div key={item.id} className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <span className="text-2xl">{getItemIcon(item.name)}</span>
                    <div>
                      <h3 className="font-semibold text-gray-800">{item.name}</h3>
                      <p className="text-sm text-gray-600">{item.description}</p>
                      <div className="flex items-center space-x-2 mt-1">
                        <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                          {getItemTypeIcon(item.type)} {item.type}
                        </span>
                        {item.happinessBoost > 0 && (
                          <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                            +{item.happinessBoost} happiness
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-lg font-bold text-blue-600">{item.price} pts</div>
                    <button
                      onClick={() => handlePurchase(item.id, item.price)}
                      disabled={purchasing === item.id || pointBalance.currentBalance < item.price}
                      className={`mt-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                        purchasing === item.id
                          ? 'bg-gray-300 cursor-not-allowed'
                          : pointBalance.currentBalance < item.price
                          ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                          : 'bg-blue-500 hover:bg-blue-600 text-white'
                      }`}
                    >
                      {purchasing === item.id ? 'Buying...' : 'Buy'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-4">
            {userItems.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <p>🎒 Your inventory is empty</p>
                <p className="text-sm">Purchase items from the shop to get started!</p>
              </div>
            ) : (
              userItems.map((userItem) => (
                <div key={userItem.id} className="border border-gray-200 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <span className="text-2xl">{getItemIcon(userItem.itemTemplate.name)}</span>
                      <div>
                        <h3 className="font-semibold text-gray-800">{userItem.itemTemplate.name}</h3>
                        <p className="text-sm text-gray-600">{userItem.itemTemplate.description}</p>
                        <div className="flex items-center space-x-2 mt-1">
                          <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                            Qty: {userItem.quantity}
                          </span>
                          {userItem.isEquipped && (
                            <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                              ✨ Equipped
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => handleUseItem(userItem.id, userItem.itemTemplate.type)}
                      disabled={using === userItem.id}
                      className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                        using === userItem.id
                          ? 'bg-gray-300 cursor-not-allowed'
                          : userItem.itemTemplate.type === 'FOOD'
                          ? 'bg-green-500 hover:bg-green-600 text-white'
                          : userItem.isEquipped
                          ? 'bg-red-500 hover:bg-red-600 text-white'
                          : 'bg-blue-500 hover:bg-blue-600 text-white'
                      }`}
                    >
                      {using === userItem.id 
                        ? 'Using...' 
                        : userItem.itemTemplate.type === 'FOOD'
                        ? 'Feed Pet'
                        : userItem.isEquipped
                        ? 'Unequip'
                        : 'Equip'
                      }
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Shop; 