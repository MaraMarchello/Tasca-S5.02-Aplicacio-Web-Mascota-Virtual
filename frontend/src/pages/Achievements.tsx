import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const AchievementsPage: React.FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // Redirect to pet page with achievements tab
    navigate('/pet?tab=achievements', { replace: true });
  }, [navigate]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
        <p className="mt-4 text-gray-600">Redirecting to achievements...</p>
      </div>
    </div>
  );
};

export default AchievementsPage; 