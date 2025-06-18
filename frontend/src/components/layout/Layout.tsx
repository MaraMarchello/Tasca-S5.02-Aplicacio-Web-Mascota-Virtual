import React, { useState } from 'react';
import { clsx } from 'clsx';
import NavBar from './NavBar';
import Sidebar from './Sidebar';
import Breadcrumb from './Breadcrumb';

interface LayoutProps {
  children: React.ReactNode;
  className?: string;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '7xl' | 'full';
  padding?: boolean;
  showSidebar?: boolean;
  showBreadcrumb?: boolean;
  breadcrumbItems?: Array<{
    label: string;
    path?: string;
    icon?: React.ReactNode;
  }>;
  user?: {
    id: number;
    name: string;
    email: string;
    authorities: string[];
  } | null;
}

const Layout: React.FC<LayoutProps> = ({
  children,
  className,
  maxWidth = '7xl',
  padding = true,
  showSidebar = true,
  showBreadcrumb = true,
  breadcrumbItems,
  user,
}) => {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  const toggleSidebar = () => {
    setIsSidebarCollapsed(!isSidebarCollapsed);
  };
  const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '7xl': 'max-w-7xl',
    full: 'max-w-full',
  };

  return (
    <div className="min-h-screen bg-background-light dark:bg-background-dark transition-colors duration-200">
      <NavBar 
        user={user} 
        onSidebarToggle={showSidebar ? toggleSidebar : undefined}
      />
      
      {/* Sidebar */}
      {showSidebar && user && (
        <Sidebar 
          isCollapsed={isSidebarCollapsed}
          onToggle={toggleSidebar}
          user={user}
        />
      )}
      
      {/* Main Content */}
      <main className={clsx(
        'pt-16 transition-all duration-300 ease-in-out',
        showSidebar && user ? (
          isSidebarCollapsed ? 'lg:ml-16' : 'lg:ml-64'
        ) : ''
      )}>
        <div className={clsx(
          'mx-auto',
          maxWidthClasses[maxWidth],
          padding && 'px-4 sm:px-6 lg:px-8 py-8',
          className
        )}>
          {/* Breadcrumb */}
          {showBreadcrumb && user && (
            <div className="mb-6">
              <Breadcrumb items={breadcrumbItems} />
            </div>
          )}
          
          {children}
        </div>
      </main>
    </div>
  );
};

export default Layout; 