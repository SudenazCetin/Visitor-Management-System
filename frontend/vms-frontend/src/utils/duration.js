/**
 * Safely parses Date from ISO string or Jackson array [year, month, day, hour, minute, second]
 * @param {string|Array} timeVal 
 * @returns {Date|null}
 */
export function parseDateTime(timeVal) {
  if (!timeVal) return null;

  if (Array.isArray(timeVal)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = timeVal;
    return new Date(year, month - 1, day, hour, minute, second);
  }

  const parsed = new Date(timeVal);
  return isNaN(parsed.getTime()) ? null : parsed;
}

/**
 * Calculates human-readable stay duration for a visitor.
 * If visitor is inside, calculates dynamic live duration up to currentTime.
 * If visitor checked out, calculates static duration between entryTime and exitTime.
 * 
 * Format rules:
 * - 30 seconds -> "0 dk"
 * - 5 minutes -> "5 dk"
 * - 65 minutes -> "1 sa 5 dk"
 * - 125 minutes -> "2 sa 5 dk"
 * 
 * @param {string|Array} entryTimeVal 
 * @param {string|Array} exitTimeVal 
 * @param {boolean} isInside 
 * @param {Date} [currentTime] 
 * @returns {string} Formatted duration string
 */
export function calculateLiveDuration(entryTimeVal, exitTimeVal, isInside, currentTime = new Date()) {
  const entryDate = parseDateTime(entryTimeVal);
  if (!entryDate) return '-';

  let endDate;
  if (isInside) {
    endDate = currentTime || new Date();
  } else if (exitTimeVal) {
    endDate = parseDateTime(exitTimeVal);
  } else {
    endDate = currentTime || new Date();
  }

  if (!endDate) return '-';

  const diffMs = Math.max(0, endDate.getTime() - entryDate.getTime());
  const totalMinutes = Math.floor(diffMs / (1000 * 60));

  const hours = Math.floor(totalMinutes / 60);
  const mins = totalMinutes % 60;

  if (hours === 0) {
    return `${mins} dk`;
  }
  if (mins === 0) {
    return `${hours} sa`;
  }
  return `${hours} sa ${mins} dk`;
}

/**
 * Calculates digital stay duration (HH:MM:SS) for a visitor.
 * If visitor is inside, calculates live digital duration up to currentTime.
 * If visitor checked out, calculates static digital duration between entryTime and exitTime.
 * 
 * Format: HH:MM:SS (e.g. 00:00:05, 00:04:23, 01:02:10)
 * 
 * @param {string|Array} entryTimeVal 
 * @param {string|Array} exitTimeVal 
 * @param {boolean} isInside 
 * @param {Date} [currentTime] 
 * @returns {string} Formatted HH:MM:SS duration string
 */
export function calculateDigitalDuration(entryTimeVal, exitTimeVal, isInside, currentTime = new Date()) {
  const entryDate = parseDateTime(entryTimeVal);
  if (!entryDate) return '00:00:00';

  let endDate;
  if (isInside) {
    endDate = currentTime || new Date();
  } else if (exitTimeVal) {
    endDate = parseDateTime(exitTimeVal);
  } else {
    endDate = currentTime || new Date();
  }

  if (!endDate) return '00:00:00';

  const diffMs = Math.max(0, endDate.getTime() - entryDate.getTime());
  const totalSeconds = Math.floor(diffMs / 1000);

  const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(totalSeconds % 60)).padStart(2, '0');

  return `${hours}:${minutes}:${seconds}`;
}

/**
 * Formats entry or exit time into HH:MM or DD.MM.YYYY HH:MM
 * @param {string|Array} timeVal 
 * @param {boolean} [includeDate=false] 
 * @returns {string}
 */
export function formatDateTimeStr(timeVal, includeDate = false) {
  const date = parseDateTime(timeVal);
  if (!date) return '-';

  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  if (!includeDate) {
    return `${hours}:${minutes}`;
  }

  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();

  return `${day}.${month}.${year} ${hours}:${minutes}`;
}
