import { writable } from 'svelte/store';

// Initialize state from localStorage if available
const storedUser = localStorage.getItem('vms_user') 
  ? JSON.parse(localStorage.getItem('vms_user')) 
  : null;
const storedToken = localStorage.getItem('vms_token') || null;

const initialState = {
  user: storedUser,
  token: storedToken,
  isAuthenticated: Boolean(storedToken),
  loading: false,
  error: null,
};

function createAuthStore() {
  const { subscribe, set, update } = writable(initialState);

  return {
    subscribe,

    /**
     * Set loading state
     */
    setLoading: (isLoading) => update(s => ({ ...s, loading: isLoading, error: null })),

    /**
     * Set error message
     */
    setError: (errorMessage) => update(s => ({ ...s, loading: false, error: errorMessage })),

    /**
     * Set authenticated user session
     * @param {Object} user - User details { username, role }
     * @param {string} token - JWT token
     */
    loginSuccess: (user, token) => {
      if (token) localStorage.setItem('vms_token', token);
      if (user) localStorage.setItem('vms_user', JSON.stringify(user));

      update(s => ({
        ...s,
        user,
        token,
        isAuthenticated: true,
        loading: false,
        error: null,
      }));
    },

    /**
     * Clear auth session and logout
     */
    logout: () => {
      localStorage.removeItem('vms_token');
      localStorage.removeItem('vms_user');
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: null,
      });
    },

    /**
     * Reset error state
     */
    clearError: () => update(s => ({ ...s, error: null })),
  };
}

export const authStore = createAuthStore();
