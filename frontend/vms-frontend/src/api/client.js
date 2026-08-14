// Centralized API configuration for VMS Backend
export const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Helper function to construct full API endpoint URL
 * @param {string} path - API endpoint path (e.g. '/personnel')
 * @returns {string} Full URL
 */
export function getApiUrl(path) {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${cleanPath}`;
}
