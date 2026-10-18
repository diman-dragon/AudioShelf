import { readStored, writeStored } from '../core/storage.js';

const KEY = 'audioshelf.normOn';

export const isNormOn = () => readStored(KEY) !== '0';

export function setNormOn(on) {
  writeStored(KEY, on ? '1' : '0');
  document.dispatchEvent(new Event('normchange'));
}
