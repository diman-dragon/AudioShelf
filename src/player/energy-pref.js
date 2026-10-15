import { Energy } from '../folder/plugins.js';
import { readStored, writeStored } from '../core/storage.js';

// Аппаратное воспроизведение (экономит батарею): звук декодирует чип телефона, процессор спит.
// Включается само только пока не нужны эквалайзер и поправки громкости. По умолчанию выключено.
const KEY = 'audioshelf.offload';

export const isOffloadOn = () => readStored(KEY) === '1';

export const applyOffload = () => Energy.setOffload({ enabled: isOffloadOn() }).catch(() => {});

export function setOffloadOn(on) {
  writeStored(KEY, on ? '1' : '0');
  return applyOffload();
}
