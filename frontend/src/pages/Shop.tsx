import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const ShopPage: React.FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // Redirect to Pet page with shop tab
    console.log('🛍️ Redirecting to Pet page shop tab...');
    navigate('/pet?tab=shop', { replace: true });
  }, [navigate]);

  // Show loading while redirecting
  return (
    <div className="flex items-center justify-center min-h-[50vh]">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
        <p className="mt-4 text-gray-600">Redirecting to shop...</p>
      </div>
    </div>
  );
};

export default ShopPage; 