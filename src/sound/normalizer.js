import { isPremium } from '../billing/entitlement.js';
import { Loudness } from '../folder/plugins.js';
import { getFiles } from '../library/library-data.js';
import { flushGains, hasGain, saveGain } from './gain-store.js';

// Нормализация в фоне: нативная часть измеряет файлы по одному, а мы сохраняем поправки и показываем ход работы.
// status: idle | running | done | cancelled | battery (остановлено, чтобы не посадить слабую батарею)
let state = { status: 'idle', done: 0, total: 0, failed: 0, current: '' };
let listener = null;
const FINISH = { cancelled: 'cancelled', battery: 'battery' };

export const getNorm = () => state;
export const pendingFiles = () => getFiles().filter((f) => !hasGain(f.uri));

const set = (patch) => {
  state = { ...state, ...patch };
  document.dispatchEvent(new Event('normchange'));
};

function onEvent(e) {
  if (e.type === 'start') return set({ current: e.name });
  if (e.type === 'file') {
    // Файл, который не удалось измерить, тоже запоминаем (поправка 0): иначе его пробовали бы заново при каждом запуске.
    if (e.gain == null) {
      saveGain(e.uri, 0);
      return set({ done: state.done + 1, failed: state.failed + 1 });
    }
    saveGain(e.uri, e.gain);
    return set({ done: state.done + 1 });
  }
  flushGains();
  listener?.remove();
  listener = null;
  set({ status: FINISH[e.status] ?? 'done', failed: e.failed, current: '' });
}

export async function startNormalize() {
  if (state.status === 'running' || !isPremium()) return;
  const todo = pendingFiles();
  if (!todo.length) return set({ status: 'done', done: 0, total: 0, failed: 0, current: '' });
  set({ status: 'running', done: 0, total: todo.length, failed: 0, current: '', paused: false });
  listener = await Loudness.addListener('loudness', onEvent);
  try {
    await Loudness.start({ files: todo.map((f) => ({ uri: f.uri, name: f.name })) });
  } catch {
    onEvent({ type: 'finish', status: 'done', failed: todo.length });
  }
}

export const stopNormalize = () => Loudness.cancel().catch(() => {});

// Остановка не по просьбе человека (началось воспроизведение): автозапуск потом продолжит с оставшихся файлов.
export const pauseNormalize = () => {
  state = { ...state, paused: true };
  return stopNormalize();
};
