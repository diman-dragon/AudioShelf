// «Умная» перемотка: после остановки продолжаем чуть раньше, чтобы вспомнить, на чём остановились.
export const REWIND_MS = 5000;
const MIN_PAUSE = 3000; // короче пауза — не откатываем (случайное нажатие)

let pausedAt = 0;

export const notePause = () => {
  pausedAt = Date.now();
};

// Откуда продолжить после паузы из этого места.
export function rewindTarget(position) {
  const long = pausedAt > 0 && Date.now() - pausedAt >= MIN_PAUSE;
  pausedAt = 0;
  return long ? Math.max(0, position - REWIND_MS) : position;
}
