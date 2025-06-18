import React from 'react';
import Layout from '../components/layout/Layout';
import { AIHelper as AIHelperComponent } from '../components/ai';

const AIHelper: React.FC = () => {
  return (
    <Layout maxWidth="full">
      <div className="h-[calc(100vh-8rem)] p-4">
        <AIHelperComponent className="h-full" />
      </div>
    </Layout>
  );
};

export default AIHelper; 