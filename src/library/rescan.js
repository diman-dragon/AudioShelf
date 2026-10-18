import { getFolder } from '../folder/folder-store.js';
import { finishScan, getStatus, isScanning, restoreLibrary, setProgress, setScanning } from './library-data.js';
import { cancelScan, scanFolder } from './scan.js';

let again = false;

export async function rescan() {
  const folder = getFolder();
  if (!folder) return restoreLibrary();
  if (isScanning()) {
    again = true;
    return cancelScan();
  }
  setScanning(true);
  let files = null;
  let failed = false;
  try {
    const result = await scanFolder(folder.uri, setProgress);
    if (!again && (!result.cancelled || getStatus() !== 'ok')) files = result.files;
  } catch {
    failed = !again;
  }
  finishScan(files, failed);
  if (again) {
    again = false;
    rescan();
  }
}
