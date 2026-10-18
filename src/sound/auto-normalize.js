import { isPremium } from '../billing/entitlement.js';
import { getStatus as getLibraryStatus, isScanning } from '../library/library-data.js';
import { getStatus } from '../player/player-state.js';
import { isNormOn } from './norm-pref.js';
import { getNorm, pauseNormalize, pendingFiles, startNormalize } from './normalizer.js';

const RETRY = 10 * 60000;
const WAIT = 4000;
let timer = 0;
let blockedUntil = 0;
let retry = 0;

const ready = () =>
  isPremium() &&
  isNormOn() &&
  getLibraryStatus() === 'ok' &&
  !isScanning() &&
  !getStatus().playing &&
  Date.now() >= blockedUntil &&
  getNorm().status !== 'running' &&
  pendingFiles().length > 0;

function plan() {
  clearTimeout(timer);
  timer = setTimeout(() => {
    if (ready()) startNormalize({ auto: true });
  }, WAIT);
}

export function initAutoNormalize() {
  ['librarychange', 'premiumchange', 'normchange'].forEach((name) => document.addEventListener(name, plan));
  document.addEventListener('playerchange', () => {
    if (getStatus().playing && getNorm().status === 'running') pauseNormalize();
    plan();
  });
  document.addEventListener('normchange', () => {
    const { status, paused } = getNorm();
    if (status === 'idle' && !paused && pendingFiles().length) {
      blockedUntil = Date.now() + RETRY;
      clearTimeout(retry);
      retry = setTimeout(plan, RETRY + 500);
    }
    if ((status === 'cancelled' && !paused) || status === 'battery') blockedUntil = Number.MAX_SAFE_INTEGER;
    if (status === 'done' && pendingFiles().length) blockedUntil = Date.now() + 30 * 60000;
  });
  document.addEventListener('scanprogress', () => (blockedUntil = 0));
  plan();
}
