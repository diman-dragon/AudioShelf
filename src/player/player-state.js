import { App } from '@capacitor/app';
import { Player } from '../folder/plugins.js';
import { loadLast, saveProgress } from './progress-store.js';

// Что играет (книга) и в каком состоянии. Состояние спрашиваем у нативного плеера по таймеру.
const EMPTY = { loaded: false, uri: '', index: 0, count: 0, playing: false, loading: false, ended: false, position: 0, duration: 0, speed: 1 };
const END_GAP = 3000; // до конца последней главы меньше 3 с — считаем дослушанной

let track = loadLast()?.files ? loadLast() : null;
let status = EMPTY;
let ticks = 0;

export const getTrack = () => track;
export const getStatus = () => status;
export const isLoaded = () => Boolean(track) && status.loaded && track.files.some((f) => f.uri === status.uri);

export function setTrack(book) {
  track = book;
  document.dispatchEvent(new Event('playerchange'));
}

// Прогресс книги запоминаем каждые ~2 с, при паузе и когда плеер закрыли из шторки.
function remember(prev, next) {
  const at = next.loaded ? next : prev; // плеер закрыли — берём последнее известное
  const last = at.index === track.files.length - 1;
  const done = next.ended || (last && at.duration > 0 && at.position >= at.duration - END_GAP);
  saveProgress(track.id, { i: at.index, p: next.ended ? 0 : at.position, d: at.duration, done });
}

export function applyStatus(next) {
  const prev = status;
  const wasLoaded = isLoaded();
  status = next;
  if (wasLoaded && ((prev.playing && !next.playing) || ++ticks % 4 === 0)) remember(prev, next);
  if (JSON.stringify(prev) !== JSON.stringify(next)) document.dispatchEvent(new Event('playerchange'));
}

export const syncNow = () => Player.state().then(applyStatus).catch(() => {});

// Состояние меняется и снаружи (шторка, наушники), поэтому спрашиваем плеер постоянно.
export function initPlayer() {
  syncNow();
  setInterval(syncNow, 500);
  App.addListener('appStateChange', (s) => s.isActive && syncNow());
}
