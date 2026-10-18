import { readStored, removeStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.folder';

export function getFolder() {
  try {
    return JSON.parse(readStored(KEY));
  } catch {
    return null;
  }
}

export function setFolder(folder) {
  writeStored(KEY, JSON.stringify(folder));
  document.dispatchEvent(new Event('folderchange'));
}

export function clearFolder() {
  removeStored(KEY);
  document.dispatchEvent(new Event('folderchange'));
}
