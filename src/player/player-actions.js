import { Player } from '../folder/plugins.js';
import { progressState, resumeAt, saveProgress } from './progress-store.js';
import { saveLast, saveSpeed } from './last-book.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded, setTrack } from './player-state.js';
import { holdStatus } from './status-poll.js';
import { refreshChapters } from './refresh-chapters.js';
import { getSkip } from './skip-pref.js';

export async function playBook(book) {
  if (progressState(book.id) === 'new') saveProgress(book.id, { i: 0, p: 1, d: 0 });
  setTrack(book);
  saveLast(book);
  await refreshChapters(book);
  const { i, p } = resumeAt(book.id);
  return load(i, p, true).catch(() => {});
}

let toggling = false;

export async function togglePlay() {
  if (!getTrack() || toggling) return;
  toggling = true;
  holdStatus(true);
  try {
    if (!isLoaded()) {
      const { i, p } = resumeAt(getTrack().id);
      await load(i, p, true);
      return;
    }
    applyStatus({ ...getStatus(), playing: !getStatus().playing });
    applyStatus(await Player.toggle());
  } finally {
    holdStatus(false);
    toggling = false;
  }
}

export async function seekTo(ms) {
  if (!getTrack()) return;
  if (!isLoaded()) return load(resumeAt(getTrack().id).i, ms, false);
  applyStatus(await Player.seek({ position: ms }));
}

export function skip(forward) {
  const { position, duration } = getStatus();
  const { back, forward: ahead } = getSkip();
  const target = position + (forward ? ahead : -back) * 1000;
  return seekTo(Math.round(Math.max(0, duration ? Math.min(duration, target) : target)));
}

export async function setSpeed(speed) {
  saveSpeed(speed);
  document.dispatchEvent(new Event('speedchange'));
  if (isLoaded()) applyStatus(await Player.setSpeed({ speed }));
}
