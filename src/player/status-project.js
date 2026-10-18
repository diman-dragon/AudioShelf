export function project(status, elapsed) {
  if (!status.playing || !status.loaded) return status;
  const position = Math.min(status.duration || Infinity, status.position + elapsed * (status.speed || 1));
  const sleepLeft = status.sleepLeft > 0 ? Math.max(0, status.sleepLeft - elapsed) : status.sleepLeft;
  return { ...status, position, sleepLeft };
}

export const needsSync = (status, projected) =>
  (status.duration > 0 && projected.position >= status.duration) || (status.sleepLeft > 0 && projected.sleepLeft <= 0);
