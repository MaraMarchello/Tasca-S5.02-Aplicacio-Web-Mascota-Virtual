import { GitRepository, ExecuteResponse } from '../types/git';

/**
 * Service for managing demo Git repositories and executing real Git commands
 */
class DemoGitService {
  private demoRepository: GitRepository | null = null;
  private readonly API_BASE = '/api/v1/git';

  /**
   * Creates or gets a demo repository for real Git execution
   */
  async getOrCreateDemoRepository(): Promise<GitRepository> {
    if (this.demoRepository) {
      return this.demoRepository;
    }

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('Authentication required. Please login to use the demo.');
      }

      const response = await fetch(`${this.API_BASE}/repository/create-demo`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Session expired. Please refresh the page and login again.');
        } else if (response.status === 403) {
          throw new Error('Permission denied. Demo mode requires authentication.');
        } else {
          throw new Error(`Failed to create demo repository: ${response.status}`);
        }
      }

      const repository: GitRepository = await response.json();
      this.demoRepository = repository;
      console.log('Demo repository created:', repository);
      return repository;

    } catch (error) {
      console.error('Error creating demo repository:', error);
      throw error;
    }
  }

  /**
   * Executes a real Git command in the demo repository
   */
  async executeCommand(command: string): Promise<ExecuteResponse> {
    try {
      const repository = await this.getOrCreateDemoRepository();
      const token = localStorage.getItem('token');
      
      if (!token) {
        throw new Error('Authentication required');
      }

      const response = await fetch(`${this.API_BASE}/repository/${repository.id}/execute`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          command,
          scenarioId: null, // Demo mode doesn't need scenario validation
          stepNumber: null
        })
      });

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Session expired. Please refresh the page and login again.');
        } else if (response.status === 403) {
          throw new Error('Command not allowed for security reasons.');
        } else if (response.status === 404) {
          throw new Error('Demo repository not found. Creating a new one...');
        } else {
          throw new Error(`Command execution failed: ${response.status}`);
        }
      }

      const executeResponse = await response.json();
      console.log('Command executed:', command, executeResponse);
      return executeResponse;

    } catch (error) {
      console.error('Error executing command:', error);
      throw error;
    }
  }

  /**
   * Gets the current repository state
   */
  async getRepositoryState() {
    try {
      const repository = await this.getOrCreateDemoRepository();
      const token = localStorage.getItem('token');
      
      if (!token) {
        throw new Error('Authentication required');
      }

      const response = await fetch(`${this.API_BASE}/repository/${repository.id}/state`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to get repository state: ${response.status}`);
      }

      return await response.json();

    } catch (error) {
      console.error('Error getting repository state:', error);
      throw error;
    }
  }

  /**
   * Resets the demo repository by creating a new one
   */
  async resetDemoRepository(): Promise<GitRepository> {
    this.demoRepository = null;
    return await this.getOrCreateDemoRepository();
  }

  /**
   * Checks if real Git execution is enabled on the backend
   */
  async isRealGitEnabled(): Promise<boolean> {
    try {
      // Try to create a demo repository to test if real Git is enabled
      await this.getOrCreateDemoRepository();
      return true;
    } catch (error) {
      console.warn('Real Git execution may not be enabled:', error);
      return false;
    }
  }

  /**
   * Gets the current demo repository without creating a new one
   */
  getCurrentRepository(): GitRepository | null {
    return this.demoRepository;
  }
}

// Export a singleton instance
export const demoGitService = new DemoGitService();
export default demoGitService;
