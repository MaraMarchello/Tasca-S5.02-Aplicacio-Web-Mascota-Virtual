import React, { useState, useEffect } from 'react';
import { api } from '../utils/api';

interface User {
  id: number;
  name: string;
  email: string;
  roles: { name: string }[];
  createdAt: string;
  enabled: boolean;
}

interface Pet {
  id: number;
  name: string;
  type: string;
  happiness: number;
  totalPointsEarned: number;
  lastFed: string;
  createdAt: string;
  user: {
    id: number;
    name: string;
    email: string;
  };
}

interface AdminStats {
  totalUsers: number;
  totalPets: number;
  totalPointsAwarded: number;
  activeUsers: number;
  happyPets: number;
  recentSignups: number;
}

interface PointTransaction {
  id: number;
  userId: number;
  amount: number;
  type: string;
  source: string;
  description: string;
  createdAt: string;
  user: {
    name: string;
    email: string;
  };
}

const AdminDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [transactions, setTransactions] = useState<PointTransaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Point awarding form
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [pointAmount, setPointAmount] = useState<number>(0);
  const [pointDescription, setPointDescription] = useState<string>('');

  // Pet modification form
  const [selectedPet, setSelectedPet] = useState<Pet | null>(null);
  const [petHappiness, setPetHappiness] = useState<number>(50);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [statsRes, usersRes, petsRes, transactionsRes] = await Promise.all([
        api.get<{success: boolean, data: AdminStats}>('/admin/stats'),
        api.get<{success: boolean, data: User[]}>('/admin/users'),
        api.get<{success: boolean, data: Pet[]}>('/admin/pets'),
        api.get<{success: boolean, data: PointTransaction[]}>('/admin/transactions?limit=50')
      ]);

      setStats(statsRes.data);
      setUsers(usersRes.data);
      setPets(petsRes.data);
      setTransactions(transactionsRes.data);
    } catch (error: any) {
      setError('Failed to load admin data: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  const toggleUserStatus = async (userId: number, enabled: boolean) => {
    try {
      await api.put(`/admin/users/${userId}/status`, { enabled });
      await loadDashboardData();
    } catch (error: any) {
      setError('Failed to update user status: ' + (error.response?.data?.message || error.message));
    }
  };

  const deletePet = async (petId: number) => {
    if (!confirm('Are you sure you want to delete this pet? This action cannot be undone.')) {
      return;
    }

    try {
      await api.delete(`/admin/pets/${petId}`);
      await loadDashboardData();
    } catch (error: any) {
      setError('Failed to delete pet: ' + (error.response?.data?.message || error.message));
    }
  };

  const updatePetHappiness = async (petId: number, happiness: number) => {
    try {
      await api.put(`/admin/pets/${petId}`, { happiness });
      setSelectedPet(null);
      await loadDashboardData();
    } catch (error: any) {
      setError('Failed to update pet happiness: ' + (error.response?.data?.message || error.message));
    }
  };

  const awardPoints = async () => {
    if (!selectedUserId || pointAmount <= 0 || !pointDescription.trim()) {
      setError('Please fill in all fields for point awarding');
      return;
    }

    try {
      await api.post('/admin/points/award', {
        userId: selectedUserId,
        amount: pointAmount,
        description: pointDescription
      });
      
      setSelectedUserId(null);
      setPointAmount(0);
      setPointDescription('');
      await loadDashboardData();
    } catch (error: any) {
      setError('Failed to award points: ' + (error.response?.data?.message || error.message));
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString() + ' ' + new Date(dateString).toLocaleTimeString();
  };

  const getHappinessColor = (happiness: number) => {
    if (happiness >= 80) return 'text-green-600';
    if (happiness >= 50) return 'text-yellow-600';
    return 'text-red-600';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading admin dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
            <button
              onClick={loadDashboardData}
              className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
            >
              Refresh Data
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="bg-red-50 border border-red-200 rounded-md p-4">
            <p className="text-red-800">{error}</p>
            <button
              onClick={() => setError(null)}
              className="mt-2 text-red-600 hover:text-red-800 underline"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Tab Navigation */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            {[
              { id: 'dashboard', name: 'Dashboard' },
              { id: 'users', name: 'User Management' },
              { id: 'pets', name: 'Pet Management' },
              { id: 'points', name: 'Point Management' },
              { id: 'reports', name: 'Reports' }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        {/* Dashboard Tab */}
        {activeTab === 'dashboard' && stats && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-blue-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">U</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Total Users</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.totalUsers}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-green-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">P</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Total Pets</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.totalPets}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-yellow-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">$</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Points Awarded</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.totalPointsAwarded.toLocaleString()}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-purple-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">A</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Active Users</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.activeUsers}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-pink-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">😊</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Happy Pets</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.happyPets}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 bg-indigo-500 rounded-md flex items-center justify-center">
                        <span className="text-white font-bold">N</span>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">Recent Signups</dt>
                        <dd className="text-lg font-medium text-gray-900">{stats.recentSignups}</dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* User Management Tab */}
        {activeTab === 'users' && (
          <div className="bg-white shadow overflow-hidden sm:rounded-md">
            <div className="px-4 py-5 sm:px-6">
              <h3 className="text-lg leading-6 font-medium text-gray-900">User Management</h3>
              <p className="mt-1 max-w-2xl text-sm text-gray-500">
                Manage user accounts and their status
              </p>
            </div>
            <ul className="divide-y divide-gray-200">
              {users.map((user) => (
                <li key={user.id} className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <div className="flex-shrink-0">
                        <div className="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center">
                          <span className="text-gray-700 font-medium">
                            {user.name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                      </div>
                      <div className="ml-4">
                        <div className="flex items-center">
                          <p className="text-sm font-medium text-gray-900">{user.name}</p>
                          {user.roles.some(role => role.name === 'ADMIN') && (
                            <span className="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                              Admin
                            </span>
                          )}
                          <span className={`ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            user.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {user.enabled ? 'Active' : 'Disabled'}
                          </span>
                        </div>
                        <p className="text-sm text-gray-500">{user.email}</p>
                        <p className="text-xs text-gray-400">Joined: {formatDate(user.createdAt)}</p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-3">
                      <button
                        onClick={() => toggleUserStatus(user.id, !user.enabled)}
                        className={`px-3 py-1 rounded text-sm font-medium ${
                          user.enabled
                            ? 'bg-red-100 text-red-800 hover:bg-red-200'
                            : 'bg-green-100 text-green-800 hover:bg-green-200'
                        }`}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Pet Management Tab */}
        {activeTab === 'pets' && (
          <div className="bg-white shadow overflow-hidden sm:rounded-md">
            <div className="px-4 py-5 sm:px-6">
              <h3 className="text-lg leading-6 font-medium text-gray-900">Pet Management</h3>
              <p className="mt-1 max-w-2xl text-sm text-gray-500">
                Manage virtual pets and their properties
              </p>
            </div>
            <ul className="divide-y divide-gray-200">
              {pets.map((pet) => (
                <li key={pet.id} className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-medium text-gray-900">{pet.name}</p>
                          <p className="text-sm text-gray-500">
                            Owner: {pet.user.name} ({pet.user.email})
                          </p>
                          <p className="text-xs text-gray-400">
                            Type: {pet.type} | Created: {formatDate(pet.createdAt)}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className={`text-sm font-medium ${getHappinessColor(pet.happiness)}`}>
                            Happiness: {pet.happiness}%
                          </p>
                          <p className="text-sm text-gray-500">
                            Points Earned: {pet.totalPointsEarned.toLocaleString()}
                          </p>
                          <p className="text-xs text-gray-400">
                            Last Fed: {pet.lastFed ? formatDate(pet.lastFed) : 'Never'}
                          </p>
                        </div>
                      </div>
                    </div>
                    <div className="ml-4 flex items-center space-x-3">
                      <button
                        onClick={() => {
                          setSelectedPet(pet);
                          setPetHappiness(pet.happiness);
                        }}
                        className="px-3 py-1 bg-blue-100 text-blue-800 rounded text-sm font-medium hover:bg-blue-200"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => deletePet(pet.id)}
                        className="px-3 py-1 bg-red-100 text-red-800 rounded text-sm font-medium hover:bg-red-200"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>

            {/* Pet Edit Modal */}
            {selectedPet && (
              <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
                <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
                  <div className="mt-3">
                    <h3 className="text-lg font-medium text-gray-900 mb-4">
                      Edit Pet: {selectedPet.name}
                    </h3>
                    <div className="mb-4">
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Happiness Level
                      </label>
                      <div className="flex items-center space-x-3">
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={petHappiness}
                          onChange={(e) => setPetHappiness(parseInt(e.target.value))}
                          className="flex-1"
                        />
                        <span className="text-sm font-medium text-gray-900 w-12">
                          {petHappiness}%
                        </span>
                      </div>
                    </div>
                    <div className="flex justify-end space-x-3">
                      <button
                        onClick={() => setSelectedPet(null)}
                        className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => updatePetHappiness(selectedPet.id, petHappiness)}
                        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                      >
                        Update
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Point Management Tab */}
        {activeTab === 'points' && (
          <div className="space-y-6">
            {/* Point Awarding Interface */}
            <div className="bg-white shadow sm:rounded-lg">
              <div className="px-4 py-5 sm:p-6">
                <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">Award Points</h3>
                <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Select User
                    </label>
                    <select
                      value={selectedUserId || ''}
                      onChange={(e) => setSelectedUserId(e.target.value ? parseInt(e.target.value) : null)}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="">Choose a user...</option>
                      {users.map((user) => (
                        <option key={user.id} value={user.id}>
                          {user.name} ({user.email})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Point Amount
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={pointAmount || ''}
                      onChange={(e) => setPointAmount(parseInt(e.target.value) || 0)}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Enter points"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Description
                    </label>
                    <input
                      type="text"
                      value={pointDescription}
                      onChange={(e) => setPointDescription(e.target.value)}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Reason for awarding points"
                    />
                  </div>
                </div>
                <div className="mt-4">
                  <button
                    onClick={awardPoints}
                    disabled={!selectedUserId || pointAmount <= 0 || !pointDescription.trim()}
                    className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                  >
                    Award Points
                  </button>
                </div>
              </div>
            </div>

            {/* Recent Transactions */}
            <div className="bg-white shadow overflow-hidden sm:rounded-md">
              <div className="px-4 py-5 sm:px-6">
                <h3 className="text-lg leading-6 font-medium text-gray-900">Recent Point Transactions</h3>
              </div>
              <ul className="divide-y divide-gray-200">
                {transactions.map((transaction) => (
                  <li key={transaction.id} className="px-4 py-4 sm:px-6">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-gray-900">
                          {transaction.user.name}
                        </p>
                        <p className="text-sm text-gray-500">{transaction.description}</p>
                        <p className="text-xs text-gray-400">
                          {formatDate(transaction.createdAt)} | Source: {transaction.source}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className={`text-sm font-medium ${
                          transaction.type === 'EARNED' ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {transaction.type === 'EARNED' ? '+' : '-'}{transaction.amount} points
                        </p>
                        <p className="text-xs text-gray-400">{transaction.type}</p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}

        {/* Reports Tab */}
        {activeTab === 'reports' && (
          <div className="bg-white shadow sm:rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">System Reports</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium text-gray-900 mb-2">User Activity</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Total registered users: {users.length}</li>
                    <li>Active users: {users.filter(u => u.enabled).length}</li>
                    <li>Admin users: {users.filter(u => u.roles.some(r => r.name === 'ADMIN')).length}</li>
                  </ul>
                </div>
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium text-gray-900 mb-2">Pet Statistics</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Total pets: {pets.length}</li>
                    <li>Happy pets (&gt;80%): {pets.filter(p => p.happiness > 80).length}</li>
                    <li>Sad pets (&lt;30%): {pets.filter(p => p.happiness < 30).length}</li>
                  </ul>
                </div>
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium text-gray-900 mb-2">Point Economy</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Total transactions: {transactions.length}</li>
                    <li>Points earned: {transactions.filter(t => t.type === 'EARNED').reduce((sum, t) => sum + t.amount, 0).toLocaleString()}</li>
                    <li>Points spent: {transactions.filter(t => t.type === 'SPENT').reduce((sum, t) => sum + t.amount, 0).toLocaleString()}</li>
                  </ul>
                </div>
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium text-gray-900 mb-2">Recent Activity</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>New signups (7 days): {stats?.recentSignups || 0}</li>
                    <li>Recent transactions: {transactions.filter(t => new Date(t.createdAt) > new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)).length}</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard; 