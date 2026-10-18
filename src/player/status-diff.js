const TIME = new Set(['position', 'sleepLeft']);

export function diffKind(prev, next) {
  let kind = 'none';
  new Set([...Object.keys(prev), ...Object.keys(next)]).forEach((key) => {
    if (prev[key] === next[key]) return;
    if (TIME.has(key)) kind = kind === 'none' ? 'tick' : kind;
    else kind = 'change';
  });
  return kind;
}
