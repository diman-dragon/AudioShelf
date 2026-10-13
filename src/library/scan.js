import { Folder } from '../folder/plugins.js';

// Сканирует папку с подпапками. onProgress({ files, folders }) вызывается по ходу обхода.
export async function scanFolder(tree, onProgress) {
  const listener = await Folder.addListener('scan', onProgress);
  try {
    return await Folder.scan({ tree });
  } finally {
    listener.remove();
  }
}

export const cancelScan = () => Folder.cancelScan().catch(() => {});
