import { Sleep } from '../folder/plugins.js';

// Таймер сна: выкл, N минут воспроизведения или до конца главы. Считает нативный плеер, мы запоминаем выбор для подсветки.
export const SLEEP_OPTIONS = ['off', 15, 30, 45, 60, 'chapter'];

let choice = 'off';
let armedAt = 0;
const GRACE = 1500; // мс: пока нативный таймер заводится, состояние ещё старое

export const getSleepChoice = () => choice;

const notify = () => document.dispatchEvent(new Event('sleepchange'));

export function setSleepChoice(id) {
  choice = id;
  armedAt = Date.now();
  notify();
  return Sleep.setSleep({ minutes: typeof id === 'number' ? id : 0, chapter: id === 'chapter' }).catch(() => {});
}

// Таймер сработал (или плеер закрыли) — выбор сбрасывается.
export function syncSleep(status) {
  if (choice !== 'off' && Date.now() - armedAt > GRACE && !(status.sleepLeft > 0 || status.sleepEnd)) {
    choice = 'off';
    notify();
  }
}
