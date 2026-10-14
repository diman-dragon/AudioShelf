import { Player } from '../folder/plugins.js';
import { loadSpeed, progressState, resumeAt, saveLast, saveProgress, saveSpeed } from './progress-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded, setTrack } from './player-state.js';
import { nextSpeed } from './speeds.js';

const SKIP_BACK = 15000;
const SKIP_FORWARD = 30000;

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
  applyStatus(await Player.play());
}

export async function seekTo(ms) {
  if (!getTrack()) return;
  if (!isLoaded()) return load(resumeAt(getTrack().id).i, ms, false);
  applyStatus(await Player.seek({ position: ms }));
}

export function skip(forward) {
  const { position, duration } = getStatus();
  const target = position + (forward ? SKIP_FORWARD : -SKIP_BACK);
  return seekTo(Math.max(0, duration ? Math.min(duration, target) : target));
}

export async function cycleSpeed() {
  const speed = nextSpeed(loadSpeed());
  saveSpeed(speed);
  if (isLoaded()) applyStatus(await Player.setSpeed({ speed }));
  else document.dispatchEvent(new Event('playerchange'));
}
