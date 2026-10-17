// Состояние плеера через elapsed мс после последнего ответа плеера: идёт позиция (с учётом скорости) и остаток таймера сна.
// Считается без обращения к нативной части; настоящее состояние подтягивается сверкой и событиями.
export function project(status, elapsed) {
  if (!status.playing || !status.loaded) return status;
  const position = Math.min(status.duration || Infinity, status.position + elapsed * (status.speed || 1));
  const sleepLeft = status.sleepLeft > 0 ? Math.max(0, status.sleepLeft - elapsed) : status.sleepLeft;
  return { ...status, position, sleepLeft };
}

// Дошли до конца главы или таймера: пора спросить плеер, что с ним на самом деле.
export const needsSync = (status, projected) =>
  (status.duration > 0 && projected.position >= status.duration) || (status.sleepLeft > 0 && projected.sleepLeft <= 0);
