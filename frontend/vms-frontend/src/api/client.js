// Centralized API Base URL configuration for VMS Backend
export const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Generic fetch wrapper with JSON handling, Bearer token injection, and 401/403 error handling
 * @param {string} endpoint - Relative endpoint path (e.g. '/personnel')
 * @param {RequestInit} [options] - Standard fetch options
 * @returns {Promise<any>} Parsed response data
 */
export async function request(endpoint, options = {}) {
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${API_BASE_URL}${cleanEndpoint}`;

  // Read JWT token from localStorage
  const token = localStorage.getItem('vms_token');

  // Auth endpoints (/auth/login, /auth/register) must NEVER send Bearer tokens
  const isAuthEndpoint = cleanEndpoint.startsWith('/auth/') || options.skipAuth === true;

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(token && !isAuthEndpoint ? { 'Authorization': `Bearer ${token}` } : {}),
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
      // 401 Unauthorized: Session expired or invalid token
      if (response.status === 401) {
        // If 401 happens on an auth endpoint, pass through the backend error message
        if (isAuthEndpoint) {
          const loginFailError = new Error((data && data.message) || 'Kullanıcı adı veya şifre hatalı.');
          loginFailError.status = 401;
          loginFailError.data = data;
          throw loginFailError;
        }

        localStorage.removeItem('vms_token');
        localStorage.removeItem('vms_username');
        localStorage.removeItem('vms_role');

        const authError = new Error((data && data.message) || 'Oturum süreniz doldu veya geçersiz. Lütfen tekrar giriş yapın.');
        authError.status = 401;
        authError.data = data;
        
        // Dispatch custom event to notify App.svelte to force re-render login
        window.dispatchEvent(new CustomEvent('vms-unauthorized'));
        throw authError;
      }

      // 403 Forbidden: User authenticated but unauthorized for specific action
      if (response.status === 403) {
        const forbiddenError = new Error((data && data.message) || 'Bu işlem için yetkiniz bulunmamaktadır.');
        forbiddenError.status = 403;
        forbiddenError.data = data;
        throw forbiddenError;
      }

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
