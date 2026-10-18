import { Energy } from '../folder/plugins.js';
import { readStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.offload';

export const isOffloadOn = () => readStored(KEY) === '1';

export const applyOffload = () => Energy.setOffload({ enabled: isOffloadOn() }).catch(() => {});

export function setOffloadOn(on) {
  writeStored(KEY, on ? '1' : '0');
  return applyOffload();
}
