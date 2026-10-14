import { Player } from '../folder/plugins.js';
import { progressState, resumeAt, saveLast, saveProgress, saveSpeed } from './progress-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded, setTrack } from './player-state.js';
import { rewindTarget } from './rewind.js';
import { getSkip } from './skip-pref.js';

export function playBook(book) {
  const { i, p } = resumeAt(book.id);
  if (progressState(book.id) === 'new') saveProgress(book.id, { i: 0, p: 1, d: 0 }); // «начато»: значок книги сразу меняется
  setTrack(book);
  saveLast(book);
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
  else {
    const back = rewindTarget(s.position); // после паузы — на 5 с раньше
    if (back !== s.position) await Player.seek({ position: back });
  }
  applyStatus(await Player.play());
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
  if (isLoaded()) applyStatus(await Player.setSpeed({ speed }));
}
