import { getDurations } from './durations.js';

export function bookProgress(id, count, index, position) {
  const known = getDurations(id);
  const values = Object.values(known);
  if (!values.length) return null;
  const avg = values.reduce((a, b) => a + b, 0) / values.length;
  let before = 0;
  let total = 0;
  for (let i = 0; i < count; i++) {
    const len = known[i] ?? avg;
    total += len;
    if (i < index) before += len;
  }
  const elapsed = before + position;
  return {
    percent: Math.min(100, Math.floor((elapsed / total) * 100)),
    left: Math.max(0, total - elapsed),
    exact: values.length >= count,
  };
}
