import { request } from './client.js';

function buildQuery(startDate, endDate) {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  const str = params.toString();
  return str ? `?${str}` : '';
}

/**
 * Fetch top summary KPI metrics with optional date range filter
 */
export async function getSummary(startDate, endDate) {
  return request(`/reports/summary${buildQuery(startDate, endDate)}`);
}

/**
 * Fetch 7-day or date range visitor traffic statistics
 */
export async function getWeeklyReport(startDate, endDate) {
  return request(`/reports/weekly${buildQuery(startDate, endDate)}`);
}

/**
 * Fetch top 5 most visited personnel with optional date range filter
 */
export async function getTopPersonnel(startDate, endDate) {
  return request(`/reports/top-personnel${buildQuery(startDate, endDate)}`);
}

/**
 * Fetch visitor distribution grouped by department with optional date range filter
 */
export async function getDepartmentReport(startDate, endDate) {
  return request(`/reports/by-department${buildQuery(startDate, endDate)}`);
}
