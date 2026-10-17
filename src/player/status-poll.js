import { App } from '@capacitor/app';
import { Player, Resume } from '../folder/plugins.js';
import { applyStatus, getTrack } from './player-state.js';
import { adoptSpot } from './progress-store.js';
import { refreshChapters } from './refresh-chapters.js';
import { initTicker } from './status-ticker.js';

// Пока идёт нажатие «пуск/пауза», ответы опроса и события (они могли начаться раньше) состояние не перебивают.
let hold = false;
export const holdStatus = (on) => {
  hold = on;
};

const fromNative = (next) => {
  if (hold) return;
  applyStatus(next);
};

export const syncNow = () => Player.state().then(fromNative).catch(() => {});

// Нативный плеер сам записывает место (каждые 10 с, при паузе, смене главы и закрытии): берём запись, если она новее нашей.
async function reconcile() {
  const saved = await Resume.saved().catch(() => null);
  const track = getTrack();
  // Номера глав у JS и у нативного плеера должны значить одно и то же: только та же книга с тем же числом глав.
  const same = track && saved?.id === track.id && saved.count === track.files.length;
  if (same && adoptSpot(saved.id, { i: saved.index, p: saved.position, t: saved.t, x: track.expanded })) {
    document.dispatchEvent(new Event('playerchange'));
  }
}

// Изменения (шторка, наушники, звонок, смена главы) приходят событиями, поэтому постоянного опроса нет.
// Состояние спрашиваем при запуске и когда приложение снова стало видимым; пока играет — раз в 10 с (status-ticker.js).
export function initPlayer() {
  const wake = (on) => {
    if (!on) return;
    reconcile();
    syncNow();
  };
  reconcile();
  syncNow();
  refreshChapters();
  Player.addListener('state', fromNative).catch(() => {});
  App.addListener('appStateChange', (s) => wake(s.isActive));
  document.addEventListener('visibilitychange', () => wake(!document.hidden));
  initTicker({ sync: syncNow, isHeld: () => hold });
}
