// API utility functions for CodeMate
const API_BASE_URL = '/api';

interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface SignUpRequest {
  name: string;
  email: string;
  password: string;
}

interface User {
  id: number;
  name: string;
  email: string;
  authorities: string[];
}

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  name: string;
  email: string;
  roles: string[];
}

// Pet System Interfaces
interface Pet {
  id: number;
  name: string;
  type: string;
  happiness: number;
  totalPointsEarned: number;
  lastFed: string;
  createdAt: string;
  updatedAt: string;
}

interface ItemTemplate {
  id: number;
  name: string;
  description: string;
  price: number;
  type: string;
  happinessBoost: number;
  iconUrl?: string;
}

interface PetItem {
  id: number;
  itemTemplate: ItemTemplate;
  quantity: number;
  isEquipped: boolean;
  purchasedAt: string;
}

interface Achievement {
  id: number;
  name: string;
  description: string;
  iconUrl?: string;
  requiredValue: number;
  type: string;
}

interface UserAchievement {
  id: number;
  achievement: Achievement;
  currentProgress: number;
  isCompleted: boolean;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

interface PointTransaction {
  id: number;
  amount: number;
  type: string;
  source: string;
  description?: string;
  createdAt: string;
}

interface PointBalance {
  currentBalance: number;
  totalEarned: number;
  totalSpent: number;
}

interface CreatePetRequest {
  name: string;
  type: string;
}

interface UpdatePetNameRequest {
  name: string;
}

interface PurchaseItemRequest {
  itemTemplateId: number;
  quantity: number;
}

interface UseItemRequest {
  petItemId: number;
}

// Get auth token from localStorage
const getAuthToken = (): string | null => {
  return localStorage.getItem('token');
};

// Set auth token in localStorage
const setAuthToken = (token: string): void => {
  localStorage.setItem('token', token);
};

// Remove auth token from localStorage
const removeAuthToken = (): void => {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('userName');
  localStorage.removeItem('userEmail');
  localStorage.removeItem('userRoles');
  localStorage.removeItem('userRole');
};

// Generic API call function
const apiCall = async <T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> => {
  const token = getAuthToken();
  
  const defaultHeaders: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token) {
    defaultHeaders.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'An error occurred');
  }

  return response.json();
};

// Auth API calls
export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await apiCall<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    
    if (response.accessToken) {
      setAuthToken(response.accessToken);
      // Store user info in localStorage
      localStorage.setItem('userId', response.userId.toString());
      localStorage.setItem('userName', response.name);
      localStorage.setItem('userEmail', response.email);
      localStorage.setItem('userRoles', JSON.stringify(response.roles));
      // Set primary role (admin takes precedence)
      const primaryRole = response.roles.includes('ROLE_ADMIN') ? 'ADMIN' : 'USER';
      localStorage.setItem('userRole', primaryRole);
    }
    
    return response;
  },

  signup: async (userData: SignUpRequest): Promise<ApiResponse> => {
    return apiCall<ApiResponse>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
  },

  logout: async (): Promise<void> => {
    try {
      await apiCall('/auth/logout', {
        method: 'POST',
      });
    } finally {
      removeAuthToken();
    }
  },
};

// User API calls
export const userApi = {
  getCurrentUser: async (): Promise<User> => {
    return apiCall<User>('/user/me');
  },
};

// Pet API calls
export const petApi = {
  createPet: async (request: CreatePetRequest): Promise<ApiResponse<Pet>> => {
    return apiCall<ApiResponse<Pet>>('/pets', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  getUserPet: async (): Promise<ApiResponse<Pet>> => {
    return apiCall<ApiResponse<Pet>>('/pets/my-pet');
  },

  updatePetName: async (request: UpdatePetNameRequest): Promise<ApiResponse<Pet>> => {
    return apiCall<ApiResponse<Pet>>('/pets/my-pet/name', {
      method: 'PUT',
      body: JSON.stringify(request),
    });
  },

  feedPet: async (): Promise<ApiResponse<Pet>> => {
    return apiCall<ApiResponse<Pet>>('/pets/my-pet/feed', {
      method: 'POST',
    });
  },

  deletePet: async (): Promise<ApiResponse<any>> => {
    return apiCall<ApiResponse<any>>('/pets/my-pet', {
      method: 'DELETE',
    });
  },

  getPetStats: async (): Promise<ApiResponse<any>> => {
    return apiCall<ApiResponse<any>>('/pets/stats');
  },
};

// Shop API calls
export const shopApi = {
  getItems: async (): Promise<ApiResponse<ItemTemplate[]>> => {
    return apiCall<ApiResponse<ItemTemplate[]>>('/shop/items');
  },

  getUserItems: async (): Promise<ApiResponse<PetItem[]>> => {
    return apiCall<ApiResponse<PetItem[]>>('/shop/my-items');
  },

  purchaseItem: async (request: PurchaseItemRequest): Promise<ApiResponse<PetItem>> => {
    return apiCall<ApiResponse<PetItem>>('/shop/purchase', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  useItem: async (request: UseItemRequest): Promise<ApiResponse<Pet>> => {
    return apiCall<ApiResponse<Pet>>('/shop/use-item', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  equipItem: async (petItemId: number): Promise<ApiResponse<PetItem>> => {
    return apiCall<ApiResponse<PetItem>>(`/shop/equip/${petItemId}`, {
      method: 'POST',
    });
  },

  unequipItem: async (petItemId: number): Promise<ApiResponse<PetItem>> => {
    return apiCall<ApiResponse<PetItem>>(`/shop/unequip/${petItemId}`, {
      method: 'POST',
    });
  },
};

// Achievement API calls
export const achievementApi = {
  getUserAchievements: async (): Promise<ApiResponse<UserAchievement[]>> => {
    return apiCall<ApiResponse<UserAchievement[]>>('/achievements/my-achievements');
  },

  getAllAchievements: async (): Promise<ApiResponse<Achievement[]>> => {
    return apiCall<ApiResponse<Achievement[]>>('/achievements/all');
  },
};

// Points API calls
export const pointsApi = {
  getBalance: async (): Promise<ApiResponse<PointBalance>> => {
    return apiCall<ApiResponse<PointBalance>>('/points/balance');
  },

  getTransactions: async (page: number = 0, size: number = 20): Promise<ApiResponse<PointTransaction[]>> => {
    return apiCall<ApiResponse<PointTransaction[]>>(`/points/transactions?page=${page}&size=${size}`);
  },

  awardStackTracePoints: async (): Promise<ApiResponse<PointTransaction>> => {
    return apiCall<ApiResponse<PointTransaction>>('/points/award/stack-trace', {
      method: 'POST',
    });
  },

  awardAiChatPoints: async (): Promise<ApiResponse<PointTransaction>> => {
    return apiCall<ApiResponse<PointTransaction>>('/points/award/ai-chat', {
      method: 'POST',
    });
  },

  checkDailyLogin: async (): Promise<ApiResponse<PointTransaction | null>> => {
    return apiCall<ApiResponse<PointTransaction | null>>('/points/daily-login', {
      method: 'POST',
    });
  },

  // Debug/test function to manually award points
  awardTestPoints: async (amount: number = 100): Promise<ApiResponse<any>> => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      throw new Error('User not logged in');
    }
    return apiCall<ApiResponse<any>>('/admin/points/award', {
      method: 'POST',
      body: JSON.stringify({
        userId: parseInt(userId),
        amount: amount,
        description: `Test points award - ${amount} points`
      }),
    });
  },
};

// Export utility functions
export { getAuthToken, setAuthToken, removeAuthToken };
export type { Pet, ItemTemplate, PetItem, Achievement, UserAchievement, PointTransaction, PointBalance };

// Generic API for admin operations
export const api = {
  get: <T>(endpoint: string) => apiCall<T>(endpoint),
  post: <T>(endpoint: string, data?: any) => apiCall<T>(endpoint, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  }),
  put: <T>(endpoint: string, data?: any) => apiCall<T>(endpoint, {
    method: 'PUT', 
    body: data ? JSON.stringify(data) : undefined,
  }),
  delete: <T>(endpoint: string) => apiCall<T>(endpoint, {
    method: 'DELETE',
  }),
}; 