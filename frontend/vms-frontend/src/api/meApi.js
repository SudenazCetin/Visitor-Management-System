import { request } from './client.js';

export async function getMyProfile() {
  return await request('/me/profile');
}

export async function getMyVisitors() {
  return await request('/me/visitors');
}

export async function getMyActiveVisitors() {
  return await request('/me/visitors/active');
}

export async function getMyRecentVisitors(limit = 5) {
  return await request(`/me/visitors/recent?limit=${encodeURIComponent(limit)}`);
}

export async function getMyVisitorById(id) {
  return await request(`/me/visitors/${id}`);
}

export async function getMySummary() {
  return await request('/me/summary');
}

export async function changeMyPassword(data) {
  return await request('/me/change-password', {
    method: 'PUT',
    body: data
  });
}
