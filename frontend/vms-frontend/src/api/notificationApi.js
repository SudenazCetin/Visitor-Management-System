import { request } from './client.js';

export async function getNotifications() {
  return await request('/notifications');
}

export async function getUnreadNotificationCount() {
  return await request('/notifications/unread-count');
}

export async function markNotificationAsRead(id) {
  return await request(`/notifications/${id}/read`, {
    method: 'PUT'
  });
}

export async function markAllNotificationsAsRead() {
  return await request('/notifications/read-all', {
    method: 'PUT'
  });
}

export async function deleteNotification(id) {
  return await request(`/notifications/${id}`, {
    method: 'DELETE'
  });
}

export async function deleteAllReadNotifications() {
  return await request('/notifications/read', {
    method: 'DELETE'
  });
}
