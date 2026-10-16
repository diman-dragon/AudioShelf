import { isPremium } from '../billing/entitlement.js';
import { getStatus as getLibraryStatus, isScanning } from '../library/library-data.js';
import { getStatus } from '../player/player-state.js';
import { isNormOn } from './norm-pref.js';
import { getNorm, pauseNormalize, pendingFiles, startNormalize } from './normalizer.js';

// Нормализация идёт сама: новые файлы библиотеки измеряются в фоне, когда это ничему не мешает.
// Не мешает — значит: Премиум и «Применять» включены, библиотека не сканируется, ничего не играет, человек не нажимал «Остановить»,
// а батарею бережёт нативная часть (она сама останавливает анализ при экономии заряда и ниже 20%).
// Найденные поправки сохраняются по каждому файлу (gain-store), так что работа не повторяется.
const WAIT = 4000; // мс спокойствия перед запуском: библиотека и плеер успели «устояться»
let timer = 0;
let blockedUntil = 0;

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
    if (ready()) startNormalize();
  }, WAIT);
}

export function initAutoNormalize() {
  ['librarychange', 'premiumchange', 'normchange'].forEach((name) => document.addEventListener(name, plan));
  document.addEventListener('playerchange', () => {
    if (getStatus().playing && getNorm().status === 'running') pauseNormalize(); // музыку не трогаем
    plan();
  });
  // Нажали «Остановить» или батарея села: до следующего изменения библиотеки или запуска приложения не лезем.
  document.addEventListener('normchange', () => {
    const { status, paused } = getNorm();
    if ((status === 'cancelled' && !paused) || status === 'battery') blockedUntil = Number.MAX_SAFE_INTEGER;
    if (status === 'done' && pendingFiles().length) blockedUntil = Date.now() + 30 * 60000; // что-то не отчиталось: не крутимся по кругу
  });
  document.addEventListener('scanprogress', () => (blockedUntil = 0));
  plan();
}
