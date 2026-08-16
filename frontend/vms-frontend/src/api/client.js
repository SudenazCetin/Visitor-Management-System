// Centralized API Base URL configuration for VMS Backend
export const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Generic fetch wrapper with JSON handling, Bearer token injection, and user-friendly error translations
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
      let friendlyMessage = (data && (data.message || data.details)) || null;

      // Handle specific HTTP Status Codes with user-friendly Turkish messages
      if (response.status === 401) {
        if (isAuthEndpoint) {
          const loginFailError = new Error(friendlyMessage || 'Kullanıcı adı veya şifre hatalı.');
          loginFailError.status = 401;
          loginFailError.data = data;
          throw loginFailError;
        }

        localStorage.removeItem('vms_token');
        localStorage.removeItem('vms_username');
        localStorage.removeItem('vms_role');

        const authError = new Error('Oturumunuz sona erdi. Lütfen tekrar giriş yapın.');
        authError.status = 401;
        authError.data = data;

        window.dispatchEvent(new CustomEvent('vms-unauthorized'));
        throw authError;
      }

      if (response.status === 403) {
        const forbiddenError = new Error('Bu işlem için yetkiniz bulunmuyor.');
        forbiddenError.status = 403;
        forbiddenError.data = data;
        throw forbiddenError;
      }

      if (response.status === 404) {
        friendlyMessage = friendlyMessage || 'İstenen kayıt bulunamadı.';
      } else if (response.status === 409) {
        friendlyMessage = friendlyMessage || 'Bu kayıt zaten mevcut.';
      } else if (response.status === 400) {
        friendlyMessage = friendlyMessage || 'Girilen bilgileri kontrol edin.';
      } else if (response.status >= 500) {
        friendlyMessage = 'Sunucu tarafında bir hata oluştu.';
      }

      const error = new Error(friendlyMessage || `HTTP ${response.status}`);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (err) {
    if (err.status !== undefined) {
      throw err;
    }
    const networkError = new Error('Sunucuya bağlanılamadı. Lütfen backend uygulamasının çalıştığından emin olun.');
    networkError.status = 0;
    throw networkError;
  }
}
