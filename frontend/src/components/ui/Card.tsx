import React from 'react';
import { clsx } from 'clsx';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  variant?: 'default' | 'elevated' | 'outlined';
  padding?: 'none' | 'sm' | 'md' | 'lg' | 'xl';
  hover?: boolean;
  clickable?: boolean;
  onClick?: () => void;
}

const Card: React.FC<CardProps> = ({
  children,
  className,
  variant = 'default',
  padding = 'md',
  hover = false,
  clickable = false,
  onClick,
}) => {
  const baseClasses = [
    'bg-surface-light dark:bg-surface-dark',
    'border border-border-light dark:border-border-dark',
    'rounded-2xl',
    'transition-all duration-200',
  ];

  const variantClasses = {
    default: [],
    elevated: [
      'shadow-soft hover:shadow-medium',
    ],
    outlined: [
      'border-2',
    ],
  };

  const paddingClasses = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-6',
    xl: 'p-8',
  };

  const interactiveClasses = [
    hover && 'hover:shadow-medium hover:-translate-y-1',
    clickable && 'cursor-pointer hover:bg-gray-50 dark:hover:bg-slate-700',
  ].filter(Boolean);

  return (
    <div
      className={clsx(
        baseClasses,
        variantClasses[variant],
        paddingClasses[padding],
        interactiveClasses,
        className
      )}
      onClick={onClick}
    >
      {children}
    </div>
  );
};

interface CardHeaderProps {
  children: React.ReactNode;
  className?: string;
}

export const CardHeader: React.FC<CardHeaderProps> = ({ children, className }) => (
  <div className={clsx('mb-4', className)}>
    {children}
  </div>
);

interface CardTitleProps {
  children: React.ReactNode;
  className?: string;
  as?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const CardTitle: React.FC<CardTitleProps> = ({ 
  children, 
  className, 
  as: Component = 'h3' 
}) => (
  <Component className={clsx(
    'text-lg font-semibold text-text-light dark:text-text-dark',
    className
  )}>
    {children}
  </Component>
);

interface CardDescriptionProps {
  children: React.ReactNode;
  className?: string;
}

export const CardDescription: React.FC<CardDescriptionProps> = ({ children, className }) => (
  <p className={clsx(
    'text-sm text-gray-600 dark:text-gray-400',
    className
  )}>
    {children}
  </p>
);

interface CardContentProps {
  children: React.ReactNode;
  className?: string;
}

export const CardContent: React.FC<CardContentProps> = ({ children, className }) => (
  <div className={clsx('', className)}>
    {children}
  </div>
);

interface CardFooterProps {
  children: React.ReactNode;
  className?: string;
}

export const CardFooter: React.FC<CardFooterProps> = ({ children, className }) => (
  <div className={clsx('mt-4 pt-4 border-t border-border-light dark:border-border-dark', className)}>
    {children}
  </div>
);

export default Card; 