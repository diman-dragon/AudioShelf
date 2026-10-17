// Что изменилось в состоянии плеера: ничего, только время (позиция, остаток таймера сна) или что-то ещё.
// Время меняется каждые полсекунды, поэтому по нему обновляется только бегунок, а не весь экран.
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
