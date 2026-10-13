import { App } from '@capacitor/app';
import { Player } from '../folder/plugins.js';
import { loadLast, savePos } from './progress-store.js';

// Что играет и в каком состоянии. Позицию спрашиваем у нативного плеера по таймеру.
const EMPTY = { loaded: false, uri: '', playing: false, loading: false, ended: false, position: 0, duration: 0, speed: 1 };

let track = loadLast();
let status = EMPTY;
let ticks = 0;

export const getTrack = () => track;
export const getStatus = () => status;
export const isLoaded = () => Boolean(track) && status.loaded && status.uri === track.uri;

export function setTrack(file) {
  track = file;
  document.dispatchEvent(new Event('playerchange'));
}

export function applyStatus(next) {
  const prev = status;
  const wasLoaded = isLoaded();
  status = next;
  // Позицию запоминаем каждые ~2 с, при паузе и когда плеер закрыли из шторки.
  if (wasLoaded && ((prev.playing && !next.playing) || ++ticks % 4 === 0)) {
    const at = next.loaded ? next : prev; // плеер закрыли — берём последнее известное
    savePos(track.uri, next.ended ? at.duration : at.position, at.duration);
  }
  if (JSON.stringify(prev) !== JSON.stringify(next)) document.dispatchEvent(new Event('playerchange'));
}

export const syncNow = () => Player.state().then(applyStatus).catch(() => {});

// Состояние меняется и снаружи (шторка, наушники), поэтому спрашиваем плеер постоянно.
export function initPlayer() {
  syncNow();
  setInterval(syncNow, 500);
  App.addListener('appStateChange', (s) => s.isActive && syncNow());
}
