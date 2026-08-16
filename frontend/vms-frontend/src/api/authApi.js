import { request } from './client.js';

/**
 * Send login credentials to backend auth endpoint
 * @param {Object} credentials - LoginRequest (username, password)
 * @returns {Promise<Object>} LoginResponse (token, username, role)
 */
export async function loginUser(credentials) {
  return request('/auth/login', {
    method: 'POST',
    body: credentials,
  });
}

/**
 * Send registration request to backend auth endpoint
 * @param {Object} userData - RegisterRequest (username, password)
 * @returns {Promise<Object>} Response { message: "Kayıt başarılı." }
 */
export async function registerUser(userData) {
  return request('/auth/register', {
    method: 'POST',
    body: userData,
  });
}
