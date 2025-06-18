# CodeMate UI Design System

This directory contains the core UI components for the CodeMate application, following our design system guidelines.

## Components

### Button
A versatile button component with multiple variants and sizes.

```tsx
import { Button } from '../ui';

// Basic usage
<Button onClick={handleClick}>Click me</Button>

// With variants
<Button variant="primary">Primary</Button>
<Button variant="secondary">Secondary</Button>
<Button variant="outline">Outline</Button>
<Button variant="ghost">Ghost</Button>
<Button variant="cta">Call to Action</Button>

// With sizes
<Button size="sm">Small</Button>
<Button size="md">Medium</Button>
<Button size="lg">Large</Button>
<Button size="xl">Extra Large</Button>

// With icons
<Button leftIcon={<IconComponent />}>With Left Icon</Button>
<Button rightIcon={<IconComponent />}>With Right Icon</Button>

// Loading state
<Button isLoading>Loading...</Button>
```

### Card
A flexible card component for content containers.

```tsx
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../ui';

<Card variant="elevated" hover>
  <CardHeader>
    <CardTitle>Card Title</CardTitle>
    <CardDescription>Card description text</CardDescription>
  </CardHeader>
  <CardContent>
    <p>Card content goes here</p>
  </CardContent>
  <CardFooter>
    <Button>Action</Button>
  </CardFooter>
</Card>
```

### Modal
A modal dialog component with accessibility features.

```tsx
import { Modal, ModalHeader, ModalTitle, ModalContent, ModalFooter } from '../ui';

<Modal isOpen={isOpen} onClose={handleClose} size="lg">
  <ModalHeader>
    <ModalTitle>Modal Title</ModalTitle>
  </ModalHeader>
  <ModalContent>
    <p>Modal content</p>
  </ModalContent>
  <ModalFooter>
    <Button variant="outline" onClick={handleClose}>Cancel</Button>
    <Button onClick={handleSave}>Save</Button>
  </ModalFooter>
</Modal>
```

### Toast
Toast notifications are managed through the ToastContext.

```tsx
import { useToast } from '../../contexts/ToastContext';

const { showSuccess, showError, showWarning, showInfo } = useToast();

// Show different types of toasts
showSuccess('Operation completed successfully!');
showError('An error occurred');
showWarning('Please check your input');
showInfo('Here\'s some information');

// With custom options
showToast({
  type: 'success',
  title: 'Success!',
  message: 'Your changes have been saved.',
  duration: 3000,
  action: {
    label: 'Undo',
    onClick: handleUndo
  }
});
```

## Layout Components

### Layout
A wrapper component that provides consistent page structure with navigation.

```tsx
import Layout from '../layout/Layout';

<Layout user={user} maxWidth="7xl" padding>
  <div>Your page content</div>
</Layout>
```

### NavBar
Navigation bar with theme switcher and user menu (used within Layout).

## Theme System

The application supports light and dark themes through the ThemeContext.

```tsx
import { useTheme } from '../../contexts/ThemeContext';

const { theme, toggleTheme, setTheme } = useTheme();

// Toggle between light and dark
<button onClick={toggleTheme}>
  {theme === 'light' ? '🌙' : '☀️'}
</button>

// Set specific theme
<button onClick={() => setTheme('dark')}>Dark Mode</button>
```

## Color System

The design system uses semantic color tokens that automatically adapt to the current theme:

- `primary-*` - Primary brand colors (blue)
- `secondary-*` - Secondary colors (emerald/green)
- `error-*` - Error states (red)
- `background-light/dark` - Page backgrounds
- `surface-light/dark` - Card/panel backgrounds
- `border-light/dark` - Border colors
- `text-light/dark` - Text colors

## Best Practices

1. **Always use semantic color tokens** instead of hardcoded colors
2. **Prefer composition over customization** - use the provided component variants
3. **Test components in both light and dark modes**
4. **Use consistent spacing** with Tailwind's spacing scale
5. **Follow accessibility guidelines** - all components include proper focus states
6. **Use the Layout component** for consistent page structure

## Animation Classes

Custom animation utilities available:
- `animate-fade-in` - Fade in animation
- `animate-slide-in` - Slide in from top
- `animate-bounce-gentle` - Gentle bouncing animation
- `animate-pulse-slow` - Slow pulse animation
- `animate-spin-slow` - Slow spinning animation 