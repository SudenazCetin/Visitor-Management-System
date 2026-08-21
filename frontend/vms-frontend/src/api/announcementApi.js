import { authStore } from '../stores/authStore.js';
import { get } from 'svelte/store';

const API_BASE = 'http://localhost:8080/api/announcements';

export async function sendAnnouncement(announcementData) {
  const { token } = get(authStore);

  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(announcementData)
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Duyuru gönderilirken bir hata oluştu.');
  }

  return data;
}
