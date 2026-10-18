import { bookProgress } from './book-progress.js';
import { saveProgress } from './progress-store.js';

const END_GAP = 3000;

export function remember(track, prev, next) {
  const at = next.loaded ? next : prev;
  const last = at.index === track.files.length - 1;
  const done = next.ended || (last && at.duration > 0 && at.position >= at.duration - END_GAP);
  const info = bookProgress(track.id, track.files.length, at.index, at.position);
  const pc = done ? 100 : info ? info.percent : 0;
  saveProgress(track.id, { i: at.index, p: next.ended ? 0 : at.position, done, x: track.expanded, pc });
}
