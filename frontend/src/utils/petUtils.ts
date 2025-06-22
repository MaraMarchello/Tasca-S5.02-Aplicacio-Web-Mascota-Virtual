/**
 * Utility functions for working with CodeMate pets
 */

// Image URLs for CodeMate mascot emotions
export const CODEMATE_IMAGES = {
  happy: "/images/CodeMate_happy.png",
  sad: "/images/CodeMate_sad.png",
  excited: "/images/CodeMate_happy.png", // Use happy for excited
  sleeping: "/images/CodeMate_sad.png", // Use sad for sleeping
  hungry: "/images/CodeMate_sad.png", // Use sad for hungry
} as const;

// Image URLs for Coffee Bean emotions
export const COFFEE_BEAN_IMAGES = {
  happy: "/images/CoffeeBean_happy.png",
  sad: "/images/CoffeeBean_sad.png",
  excited: "/images/CoffeeBean_happy.png", // Use happy for excited
  sleeping: "/images/CoffeeBean_sad.png", // Use sad for sleeping
  hungry: "/images/CoffeeBean_sad.png", // Use sad for hungry
} as const;

// Image URLs for Duke Java emotions
export const DUKE_JAVA_IMAGES = {
  happy: "/images/Duke_happy.png",
  sad: "/images/Duke_sad.png",
  excited: "/images/Duke_happy.png", // Use happy for excited
  sleeping: "/images/Duke_sad.png", // Use sad for sleeping
  hungry: "/images/Duke_sad.png", // Use sad for hungry
} as const;

export const CODEMATE_MASCOT_IMAGE_URL = CODEMATE_IMAGES.happy;

/**
 * Creates a default CodeMate mascot pet configuration
 */
export const createCodeMateMascotConfig = (overrides: Partial<{
  name: string;
  happiness: number;
  customImage: string;
  skin: string;
  accessories: string[];
}> = {}) => {
  return {
    type: 'CODEMATE_MASCOT' as const,
    name: 'CodeMate',
    happiness: 75,
    customImage: CODEMATE_MASCOT_IMAGE_URL,
    skin: 'default',
    accessories: [],
    ...overrides
  };
};

/**
 * Creates a default Coffee Bean pet configuration
 */
export const createCoffeeBeanConfig = (overrides: Partial<{
  name: string;
  happiness: number;
  customImage: string;
  skin: string;
  accessories: string[];
}> = {}) => {
  return {
    type: 'COFFEE_BEAN' as const,
    name: 'Espresso',
    happiness: 75,
    customImage: COFFEE_BEAN_IMAGES.happy,
    skin: 'default',
    accessories: [],
    ...overrides
  };
};

/**
 * Creates a default Duke Java pet configuration
 */
export const createDukeJavaConfig = (overrides: Partial<{
  name: string;
  happiness: number;
  customImage: string;
  skin: string;
  accessories: string[];
}> = {}) => {
  return {
    type: 'DUKE_JAVA' as const,
    name: 'Duke',
    happiness: 75,
    customImage: DUKE_JAVA_IMAGES.happy,
    skin: 'default',
    accessories: [],
    ...overrides
  };
};

/**
 * Checks if a pet is a CodeMate mascot
 */
export const isCodeMateMascot = (petType: string): boolean => {
  return petType === 'CODEMATE_MASCOT';
};

/**
 * Gets the appropriate image URL for a CodeMate mascot based on emotion
 */
export const getCodeMateImageForEmotion = (emotion: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'): string => {
  return CODEMATE_IMAGES[emotion] || CODEMATE_IMAGES.happy;
};

/**
 * Gets the appropriate image URL for a Coffee Bean pet based on emotion
 */
export const getCoffeeBeanImageForEmotion = (emotion: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'): string => {
  return COFFEE_BEAN_IMAGES[emotion] || COFFEE_BEAN_IMAGES.happy;
};

/**
 * Gets the appropriate image URL for a Duke Java pet based on emotion
 */
export const getDukeJavaImageForEmotion = (emotion: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'): string => {
  return DUKE_JAVA_IMAGES[emotion] || DUKE_JAVA_IMAGES.happy;
};

/**
 * Gets the appropriate image URL for any pet type based on emotion
 */
export const getImageForPetEmotion = (petType: PetType, emotion: 'happy' | 'sad' | 'excited' | 'sleeping' | 'hungry'): string | null => {
  switch (petType) {
    case 'CODEMATE_MASCOT':
      return getCodeMateImageForEmotion(emotion);
    case 'COFFEE_BEAN':
      return getCoffeeBeanImageForEmotion(emotion);
    case 'DUKE_JAVA':
      return getDukeJavaImageForEmotion(emotion);
    default:
      return null; // Use SVG for other pet types
  }
};

/**
 * Pet type definitions that include CodeMate mascot
 */
export type PetType = 'DUKE_JAVA' | 'COFFEE_BEAN' | 'CODEMATE_MASCOT' | string;

/**
 * Gets a display name for a pet type
 */
export const getPetTypeDisplayName = (petType: PetType): string => {
  switch (petType) {
    case 'CAT': return 'Cat';
    case 'DOG': return 'Dog';
    case 'BIRD': return 'Bird';
    case 'FISH': return 'Fish';
    case 'DUKE_JAVA': return 'Duke Java';
    case 'COFFEE_BEAN': return 'Coffee Bean';
    case 'CODEMATE_MASCOT': return 'CodeMate Mascot';
    default: return petType;
  }
};

/**
 * Gets an emoji for a pet type
 */
export const getPetTypeEmoji = (petType: PetType): string => {
  switch (petType) {
    case 'CAT': return '🐱';
    case 'DOG': return '🐶';
    case 'BIRD': return '🐦';
    case 'FISH': return '🐠';
    case 'DUKE_JAVA': return '☕';
    case 'COFFEE_BEAN': return '🫘';
    case 'CODEMATE_MASCOT': return '🤖';
    default: return '🐾';
  }
};

// Pet utility functions
export const getPetImage = (petType: PetType, happiness: number): string => {
  const emotion = happiness >= 50 ? 'happy' : 'sad';
  
  // Normalize the pet type to handle both enum and string formats
  const normalizedType = petType.toString().toUpperCase();
  
  console.log(`🐾 Getting pet image for type: "${petType}" (normalized: "${normalizedType}"), happiness: ${happiness}, emotion: ${emotion}`);
  
  if (normalizedType.includes('CODEMATE') || normalizedType.includes('MASCOT')) {
    const imagePath = `/images/CodeMate_${emotion}.png`;
    console.log(`🤖 CodeMate detected, using image: ${imagePath}`);
    return imagePath;
  } else if (normalizedType.includes('DUKE') || normalizedType.includes('JAVA')) {
    const imagePath = `/images/Duke_${emotion}.png`;
    console.log(`☕ Duke detected, using image: ${imagePath}`);
    return imagePath;
  } else if (normalizedType.includes('COFFEE') || normalizedType.includes('BEAN')) {
    const imagePath = `/images/CoffeeBean_${emotion}.png`;
    console.log(`🫘 Coffee Bean detected, using image: ${imagePath}`);
    return imagePath;
  }
  
  // Fallback to Duke if type is unknown
  const fallbackPath = `/images/Duke_${emotion}.png`;
  console.log(`❓ Unknown pet type, using fallback: ${fallbackPath}`);
  return fallbackPath;
};

export const getPetEmoji = (petType: PetType): string => {
  const normalizedType = petType.toString().toUpperCase();
  
  if (normalizedType.includes('CODEMATE') || normalizedType.includes('MASCOT')) {
    return '🤖';
  } else if (normalizedType.includes('DUKE') || normalizedType.includes('JAVA')) {
    return '☕';
  } else if (normalizedType.includes('COFFEE') || normalizedType.includes('BEAN')) {
    return '🫘';
  }
  
  return '🐾';
};

export const getPetDisplayName = (petType: PetType): string => {
  const normalizedType = petType.toString().toUpperCase();
  
  if (normalizedType.includes('CODEMATE') || normalizedType.includes('MASCOT')) {
    return 'CodeMate Mascot';
  } else if (normalizedType.includes('DUKE') || normalizedType.includes('JAVA')) {
    return 'Duke Java';
  } else if (normalizedType.includes('COFFEE') || normalizedType.includes('BEAN')) {
    return 'Coffee Bean';
  }
  
  return petType.toString().replace('_', ' ');
}; 