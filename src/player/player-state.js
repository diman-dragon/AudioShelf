import { App } from '@capacitor/app';
import { Player, Resume } from '../folder/plugins.js';
import { noteDuration } from './durations.js';
import { adoptSpot, loadLast, saveProgress } from './progress-store.js';
import { syncSleep } from './sleep-actions.js';

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
  document.dispatchEvent(new Event('trackchange'));
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
  syncSleep(next);
  if (wasLoaded && next.loaded && next.duration > 0) noteDuration(track.id, next.index, next.duration);
  const stopped = prev.playing && !next.playing;
  const sought = !next.playing && Math.abs(next.position - prev.position) > 500; // подвинули ползунок на паузе
  if (wasLoaded && (stopped || sought || (next.playing && ++ticks % 4 === 0))) remember(prev, next);
  if (JSON.stringify(prev) !== JSON.stringify(next)) document.dispatchEvent(new Event('playerchange'));
}

export const syncNow = () => Player.state().then(applyStatus).catch(() => {});

// Нативный плеер сам записывает место (каждые 10 с, при паузе, смене главы и закрытии): берём запись, если она новее нашей.
async function reconcile() {
  const saved = await Resume.saved().catch(() => null);
  if (saved?.id && adoptSpot(saved.id, { i: saved.index, p: saved.position, t: saved.t })) {
    document.dispatchEvent(new Event('playerchange'));
  }
}

// Изменения (шторка, наушники, звонок) приходят событиями. Опрос нужен только для бегунка: пока приложение
// видно, раз в 0,5 с при игре и раз в 2 с на паузе. В фоне и при выключенном экране не опрашиваем совсем.
export function initPlayer() {
  let active = true;
  let idle = 0;
  const wake = (on) => {
    active = on;
    if (on) {
      reconcile();
      syncNow();
    }
  };
  reconcile();
  syncNow();
  Player.addListener('state', applyStatus).catch(() => {});
  App.addListener('appStateChange', (s) => wake(s.isActive));
  document.addEventListener('visibilitychange', () => wake(!document.hidden));
  setInterval(() => {
    if (!active || (!status.playing && ++idle % 4)) return;
    syncNow();
  }, 500);
}
