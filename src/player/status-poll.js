import { App } from '@capacitor/app';
import { Player, Resume } from '../folder/plugins.js';
import { applyStatus, getTrack } from './player-state.js';
import { adoptSpot } from './progress-store.js';
import { refreshChapters } from './refresh-chapters.js';
import { initTicker } from './status-ticker.js';

let hold = false;
export const holdStatus = (on) => {
  hold = on;
};

const fromNative = (next) => {
  if (hold) return;
  applyStatus(next);
};

const syncNow = () => Player.state().then(fromNative).catch(() => {});

async function reconcile() {
  const saved = await Resume.saved().catch(() => null);
  const track = getTrack();
  const same = track && saved?.id === track.id && saved.count === track.files.length;
  if (same && adoptSpot(saved.id, { i: saved.index, p: saved.position, t: saved.t, x: track.expanded })) {
    document.dispatchEvent(new Event('playerchange'));
  }
}

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
