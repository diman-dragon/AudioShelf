import { Player } from '../folder/plugins.js';
import { progressState, resumeAt, saveLast, saveProgress, saveSpeed } from './progress-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded, refreshChapters, setTrack } from './player-state.js';
import { getSkip } from './skip-pref.js';

export async function playBook(book) {
  if (progressState(book.id) === 'new') saveProgress(book.id, { i: 0, p: 1, d: 0 }); // «начато»: значок книги сразу меняется
  setTrack(book);
  saveLast(book);
  await refreshChapters(book); // главы внутри файла (MKA, M4B, MP3): узнаём до запуска, чтобы начать с нужного места
  const { i, p } = resumeAt(book.id);
  return load(i, p, true).catch(() => {});
}

export async function togglePlay() {
  if (!getTrack()) return;
  if (!isLoaded()) {
    const { i, p } = resumeAt(getTrack().id);
    return load(i, p, true);
  }
  const s = getStatus();
  if (s.playing) return applyStatus(await Player.pause());
  if (s.ended) await Player.skipTo({ index: 0 });
  applyStatus(await Player.play()); // откат на 5 с после паузы делает нативный плеер
}

export async function seekTo(ms) {
  if (!getTrack()) return;
  if (!isLoaded()) return load(resumeAt(getTrack().id).i, ms, false);
  applyStatus(await Player.seek({ position: ms }));
}

// Перемотка на столько секунд, сколько выбрано в настройках.
export function skip(forward) {
  const { position, duration } = getStatus();
  const { back, forward: ahead } = getSkip();
  const target = position + (forward ? ahead : -back) * 1000;
  return seekTo(Math.round(Math.max(0, duration ? Math.min(duration, target) : target)));
}

// Скорость из настроек: запоминаем и сразу применяем к играющему.
export async function setSpeed(speed) {
  saveSpeed(speed);
  document.dispatchEvent(new Event('speedchange'));
  if (isLoaded()) applyStatus(await Player.setSpeed({ speed }));
}
