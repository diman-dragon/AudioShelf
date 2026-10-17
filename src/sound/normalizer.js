import { isPremium } from '../billing/entitlement.js';
import { Loudness } from '../folder/plugins.js';
import { getFiles } from '../library/library-data.js';
import { hasGain, saveGain } from './gain-store.js';

// Нормализация в фоне: нативная часть измеряет файлы по одному, а мы сохраняем поправки и показываем ход работы.
// status: idle | running | done | cancelled | battery (остановлено, чтобы не посадить слабую батарею)
let state = { status: 'idle', done: 0, total: 0, failed: 0, current: '' };
let listener = null;
const FINISH = { cancelled: 'cancelled', battery: 'battery', unplugged: 'idle' }; // unplugged: автозапуск ждёт зарядку

export const getNorm = () => state;
export const pendingFiles = () => getFiles().filter((f) => !hasGain(f.uri));

let timer = 0;

const set = (patch) => {
  clearTimeout(timer);
  timer = 0;
  state = { ...state, ...patch };
  document.dispatchEvent(new Event('normchange'));
};

// Ход работы (очередной файл готов): состояние обновляем сразу, а экраны и плеер уведомляем не чаще раза в секунду.
const step = (patch) => {
  state = { ...state, ...patch };
  if (!timer) timer = setTimeout(() => set({}), 1000);
};

function onEvent(e) {
  if (e.type === 'start') return step({ current: e.name });
  if (e.type === 'file') {
    // Файл, который не удалось измерить, тоже запоминаем (поправка 0): иначе его пробовали бы заново при каждом запуске.
    if (e.gain == null) {
      saveGain(e.uri, 0);
      return step({ done: state.done + 1, failed: state.failed + 1 });
    }
    saveGain(e.uri, e.gain);
    return step({ done: state.done + 1 });
  }
  listener?.remove();
  listener = null;
  set({ status: FINISH[e.status] ?? 'done', failed: e.failed, current: '' });
}

// auto — запуск без участия человека: работает только пока телефон на зарядке.
export async function startNormalize({ auto = false } = {}) {
  if (state.status === 'running' || !isPremium()) return;
  const todo = pendingFiles();
  if (!todo.length) return set({ status: 'done', done: 0, total: 0, failed: 0, current: '' });
  set({ status: 'running', done: 0, total: todo.length, failed: 0, current: '', paused: false });
  listener = await Loudness.addListener('loudness', onEvent);
  try {
    const res = await Loudness.start({ files: todo.map((f) => ({ uri: f.uri, name: f.name })), onlyCharging: auto });
    if (res && res.started === false) onEvent({ type: 'finish', status: res.reason === 'unplugged' ? 'unplugged' : 'done', failed: 0 });
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
