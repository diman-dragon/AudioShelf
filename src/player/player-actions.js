import { Player } from '../folder/plugins.js';
import { authorOf, titleOf } from '../library/file-info.js';
import { loadPos, loadSpeed, progressState, savePos, saveLast, saveSpeed } from './progress-store.js';
import { applyStatus, getStatus, getTrack, isLoaded, setTrack } from './player-state.js';
import { nextSpeed } from './speeds.js';

const SKIP_BACK = 15000;
const SKIP_FORWARD = 30000;

// Загружает текущий файл в нативный плеер с позиции position.
async function load(position, autoplay) {
  const track = getTrack();
  const state = await Player.load({ uri: track.uri, title: titleOf(track), artist: authorOf(track), position, autoplay });
  applyStatus(await Player.setSpeed({ speed: loadSpeed() }).catch(() => state));
}

export function playFile(file) {
  if (progressState(file.uri) === 'new') savePos(file.uri, 1, 0); // «начато» — значок книги сразу меняется
  setTrack(file);
  saveLast(file);
  return load(loadPos(file.uri), true).catch(() => {});
}

export async function togglePlay() {
  if (!getTrack()) return;
  if (!isLoaded()) return load(loadPos(getTrack().uri), true);
  const s = getStatus();
  if (s.playing) return applyStatus(await Player.pause());
  if (s.ended) await Player.seek({ position: 0 });
  applyStatus(await Player.play());
}

export async function seekTo(ms) {
  if (!getTrack()) return;
  if (!isLoaded()) return load(ms, false);
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
