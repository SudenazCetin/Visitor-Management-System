// Centralized API Base URL configuration for VMS Backend
export const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Generic fetch wrapper with JSON handling and structured error responses
 * @param {string} endpoint - Relative endpoint path (e.g. '/personnel')
 * @param {RequestInit} [options] - Standard fetch options
 * @returns {Promise<any>} Parsed response data
 */
export async function request(endpoint, options = {}) {
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${API_BASE_URL}${cleanEndpoint}`;

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {}),
  };

  const config = {
    ...options,
    headers,
  };

  if (config.body && typeof config.body === 'object') {
    config.body = JSON.stringify(config.body);
  }

  try {
    const response = await fetch(url, config);

    // HTTP 204 No Content
    if (response.status === 204) {
      return null;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const error = new Error((data && (data.message || data.details)) || `HTTP ${response.status}`);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (err) {
    if (err.status !== undefined) {
      throw err;
    }
    const networkError = new Error('Sunucuya erişilemiyor. Lütfen backend uygulamasının çalıştığından emin olun.');
    networkError.status = 0;
    throw networkError;
  }
}
