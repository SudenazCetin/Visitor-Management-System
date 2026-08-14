import { request } from './client.js';

/**
 * Fetch all visitors history
 * @returns {Promise<Array>} List of visitors
 */
export async function getAllVisitors() {
  return request('/visitors');
}

/**
 * Fetch currently active visitors inside the building
 * @returns {Promise<Array>} List of active visitors
 */
export async function getActiveVisitors() {
  return request('/visitors/active');
}

/**
 * Fetch visitor details by ID
 * @param {number|string} id - Visitor ID
 * @returns {Promise<Object>} Visitor details
 */
export async function getVisitorById(id) {
  return request(`/visitors/${id}`);
}

/**
 * Fetch visitors hosted by a specific personnel
 * @param {number|string} hostId - Host personnel ID
 * @returns {Promise<Array>} List of visitors for the host
 */
export async function getVisitorsByHostId(hostId) {
  return request(`/visitors/host/${hostId}`);
}

/**
 * Perform visitor check-in
 * @param {Object} checkInData - VisitorCheckInRequest (fullName, hostId)
 * @returns {Promise<Object>} Created VisitorResponse
 */
export async function checkInVisitor(checkInData) {
  return request('/visitors/check-in', {
    method: 'POST',
    body: checkInData,
  });
}

/**
 * Perform visitor check-out
 * @param {number|string} id - Visitor ID
 * @returns {Promise<Object>} Updated VisitorResponse with exitTime
 */
export async function checkOutVisitor(id) {
  return request(`/visitors/${id}/check-out`, {
    method: 'PUT',
  });
}
