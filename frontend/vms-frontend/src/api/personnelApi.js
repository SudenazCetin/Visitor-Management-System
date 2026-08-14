import { request } from './client.js';

/**
 * Fetch all personnel or filter by department
 * @param {string} [department] - Optional department filter
 * @returns {Promise<Array>} List of personnel
 */
export async function getAllPersonnel(department) {
  const query = department ? `?department=${encodeURIComponent(department)}` : '';
  return request(`/personnel${query}`);
}

/**
 * Fetch a single personnel by ID
 * @param {number|string} id - Personnel ID
 * @returns {Promise<Object>} Personnel details
 */
export async function getPersonnelById(id) {
  return request(`/personnel/${id}`);
}

/**
 * Create new personnel
 * @param {Object} personnelData - PersonnelRequest (fullName, department, title, email)
 * @returns {Promise<Object>} Created PersonnelResponse
 */
export async function createPersonnel(personnelData) {
  return request('/personnel', {
    method: 'POST',
    body: personnelData,
  });
}

/**
 * Update existing personnel
 * @param {number|string} id - Personnel ID
 * @param {Object} personnelData - PersonnelRequest
 * @returns {Promise<Object>} Updated PersonnelResponse
 */
export async function updatePersonnel(id, personnelData) {
  return request(`/personnel/${id}`, {
    method: 'PUT',
    body: personnelData,
  });
}

/**
 * Delete personnel by ID
 * @param {number|string} id - Personnel ID
 * @returns {Promise<null>}
 */
export async function deletePersonnel(id) {
  return request(`/personnel/${id}`, {
    method: 'DELETE',
  });
}
