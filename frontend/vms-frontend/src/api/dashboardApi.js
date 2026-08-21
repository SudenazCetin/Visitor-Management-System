import { request } from './client.js';

/**
 * Fetch real admin dashboard summary & analytics (ADMIN only) with optional range parameter ('today', '7d', '30d', 'all')
 */
export async function getAdminDashboardSummary(range = '30d') {
  const query = range ? `?range=${encodeURIComponent(range)}` : '';
  return request(`/dashboard/admin-summary${query}`);
}
