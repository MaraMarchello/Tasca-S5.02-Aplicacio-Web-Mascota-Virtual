import React from 'react';
import { clsx } from 'clsx';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'cta';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  children: React.ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  children,
  className,
  disabled,
  ...props
}) => {
  const baseClasses = [
    'inline-flex items-center justify-center font-medium transition-colors duration-200',
    'focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'rounded-xl',
  ];

  const variantClasses = {
    primary: [
      'bg-primary-500 text-white hover:bg-primary-600 active:bg-primary-700',
      'dark:bg-primary-400 dark:text-primary-950 dark:hover:bg-primary-300',
      'focus-visible:ring-primary-500 dark:focus-visible:ring-primary-400',
    ],
    secondary: [
      'bg-secondary-500 text-white hover:bg-secondary-600 active:bg-secondary-700',
      'dark:bg-secondary-400 dark:text-secondary-950 dark:hover:bg-secondary-300',
      'focus-visible:ring-secondary-500 dark:focus-visible:ring-secondary-400',
    ],
    outline: [
      'border-2 border-primary-500 text-primary-500 hover:bg-primary-50 active:bg-primary-100',
      'dark:border-primary-400 dark:text-primary-400 dark:hover:bg-primary-950 dark:active:bg-primary-900',
      'focus-visible:ring-primary-500 dark:focus-visible:ring-primary-400',
    ],
    ghost: [
      'text-primary-500 hover:bg-primary-50 active:bg-primary-100',
      'dark:text-primary-400 dark:hover:bg-primary-950 dark:active:bg-primary-900',
      'focus-visible:ring-primary-500 dark:focus-visible:ring-primary-400',
    ],
    cta: [
      'bg-gradient-to-r from-primary-500 to-secondary-500 text-white',
      'hover:from-primary-600 hover:to-secondary-600',
      'active:from-primary-700 active:to-secondary-700',
      'shadow-medium hover:shadow-strong',
      'focus-visible:ring-primary-500',
    ],
  };

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm h-8',
    md: 'px-4 py-2 text-sm h-10',
    lg: 'px-6 py-3 text-base h-12',
    xl: 'px-8 py-4 text-lg h-14',
  };

  const iconSizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-5 h-5',
    xl: 'w-6 h-6',
  };

  return (
    <button
      className={clsx(
        baseClasses,
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <>
          <svg
            className={clsx('animate-spin -ml-1 mr-2', iconSizeClasses[size])}
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          Loading...
        </>
      ) : (
        <>
          {leftIcon && (
            <span className={clsx('mr-2', iconSizeClasses[size])}>
              {leftIcon}
            </span>
          )}
          {children}
          {rightIcon && (
            <span className={clsx('ml-2', iconSizeClasses[size])}>
              {rightIcon}
            </span>
          )}
        </>
      )}
    </button>
  );
};

export default Button; 