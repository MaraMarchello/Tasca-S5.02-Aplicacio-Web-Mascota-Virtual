import React, { useEffect } from 'react';
import { clsx } from 'clsx';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  closeOnOverlayClick?: boolean;
  closeOnEscape?: boolean;
  showCloseButton?: boolean;
  className?: string;
}

const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  children,
  size = 'md',
  closeOnOverlayClick = true,
  closeOnEscape = true,
  showCloseButton = true,
  className,
}) => {
  useEffect(() => {
    if (closeOnEscape) {
      const handleEscape = (e: KeyboardEvent) => {
        if (e.key === 'Escape' && isOpen) {
          onClose();
        }
      };

      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [isOpen, onClose, closeOnEscape]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
    full: 'max-w-full mx-4',
  };

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget && closeOnOverlayClick) {
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div 
        className="flex min-h-screen items-center justify-center p-4"
        onClick={handleOverlayClick}
      >
        {/* Backdrop */}
        <div className="fixed inset-0 bg-black bg-opacity-50 transition-opacity animate-fade-in" />
        
        {/* Modal */}
        <div
          className={clsx(
            'relative w-full',
            sizeClasses[size],
            'bg-surface-light dark:bg-surface-dark',
            'rounded-2xl shadow-strong',
            'animate-slide-in',
            className
          )}
        >
          {showCloseButton && (
            <button
              onClick={onClose}
              className={clsx(
                'absolute top-4 right-4 z-10',
                'p-2 rounded-lg',
                'text-gray-400 hover:text-gray-600 dark:hover:text-gray-200',
                'hover:bg-gray-100 dark:hover:bg-gray-700',
                'transition-colors duration-200',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500'
              )}
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          )}
          
          {children}
        </div>
      </div>
    </div>
  );
};

interface ModalHeaderProps {
  children: React.ReactNode;
  className?: string;
}

export const ModalHeader: React.FC<ModalHeaderProps> = ({ children, className }) => (
  <div className={clsx('px-6 py-4 border-b border-border-light dark:border-border-dark', className)}>
    {children}
  </div>
);

interface ModalTitleProps {
  children: React.ReactNode;
  className?: string;
}

export const ModalTitle: React.FC<ModalTitleProps> = ({ children, className }) => (
  <h2 className={clsx(
    'text-xl font-semibold text-text-light dark:text-text-dark',
    className
  )}>
    {children}
  </h2>
);

interface ModalContentProps {
  children: React.ReactNode;
  className?: string;
}

export const ModalContent: React.FC<ModalContentProps> = ({ children, className }) => (
  <div className={clsx('px-6 py-4', className)}>
    {children}
  </div>
);

interface ModalFooterProps {
  children: React.ReactNode;
  className?: string;
}

export const ModalFooter: React.FC<ModalFooterProps> = ({ children, className }) => (
  <div className={clsx(
    'px-6 py-4 border-t border-border-light dark:border-border-dark',
    'flex items-center justify-end space-x-3',
    className
  )}>
    {children}
  </div>
);

export default Modal; 