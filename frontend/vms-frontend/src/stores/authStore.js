import { writable } from 'svelte/store';

// Initialize auth state from localStorage if available
const storedToken = localStorage.getItem('vms_token') || null;
const storedUsername = localStorage.getItem('vms_username') || null;
const storedRole = localStorage.getItem('vms_role') || null;

const storedUser = (storedUsername && storedRole) 
  ? { username: storedUsername, role: storedRole } 
  : null;

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

    setLoading: (isLoading) => update(s => ({ ...s, loading: isLoading, error: null })),

    setError: (errorMessage) => update(s => ({ ...s, loading: false, error: errorMessage })),

    /**
     * Store authentication session details
     * @param {string} token - JWT token string
     * @param {string} username - User login name
     * @param {string} role - System role ('ADMIN' | 'RECEPTIONIST')
     */
    loginSuccess: (token, username, role) => {
      localStorage.setItem('vms_token', token);
      localStorage.setItem('vms_username', username);
      localStorage.setItem('vms_role', role);

      const user = { username, role };

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
      localStorage.removeItem('vms_username');
      localStorage.removeItem('vms_role');
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: null,
      });
    },

    clearError: () => update(s => ({ ...s, error: null })),
  };
}

export const authStore = createAuthStore();

// Listen to global 401 unauthorized event dispatched by client.js
if (typeof window !== 'undefined') {
  window.addEventListener('vms-unauthorized', () => {
    authStore.logout();
  });
}
