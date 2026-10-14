// Безопасная обёртка над localStorage: если он недоступен — просто не сохраняем.
export function readStored(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function writeStored(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* ignore */
  }
}

export function removeStored(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}
