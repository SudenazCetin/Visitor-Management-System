import { request } from './client.js';

/**
 * Fetch all registered users
 * @returns {Promise<Array>} List of UserResponse (without passwords)
 */
export async function getAllUsers() {
  return request('/users');
}

/**
 * Fetch a single user by ID
 * @param {number|string} id - User ID
 * @returns {Promise<Object>} User details
 */
export async function getUserById(id) {
  return request(`/users/${id}`);
}

/**
 * Create a new user account
 * @param {Object} userData - UserRequest (username, password, role)
 * @returns {Promise<Object>} Created UserResponse
 */
export async function createUser(userData) {
  return request('/users', {
    method: 'POST',
    body: userData,
  });
}

/**
 * Update an existing user account
 * @param {number|string} id - User ID
 * @param {Object} userData - UserRequest
 * @returns {Promise<Object>} Updated UserResponse
 */
export async function updateUser(id, userData) {
  return request(`/users/${id}`, {
    method: 'PUT',
    body: userData,
  });
}

/**
 * Delete a user account by ID
 * @param {number|string} id - User ID
 * @returns {Promise<null>}
 */
export async function deleteUser(id) {
  return request(`/users/${id}`, {
    method: 'DELETE',
  });
}
