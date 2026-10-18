const ROW = 'row\u0001';
const KV = 'kv';

const safe = (fn, fallback) => {
  try {
    return fn(localStorage);
  } catch {
    return fallback;
  }
};

const nameOf = (s, k) => (s === KV ? k : `${ROW}${s}\u0001${k}`);

export const browserLoad = () =>
  safe((st) => {
    const rows = [];
    Object.keys(st).forEach((name) => {
      if (name.startsWith(ROW)) rows.push([...name.slice(ROW.length).split('\u0001'), st.getItem(name)]);
      else if (name.startsWith('audioshelf.')) rows.push([KV, name, st.getItem(name)]);
    });
    return rows;
  }, []);

export const browserApply = (ops) =>
  safe((st) => {
    ops.forEach(([o, s, k, v]) => {
      if (o === 'p') st.setItem(nameOf(s, k), v);
      else if (o === 'd') st.removeItem(nameOf(s, k));
      else Object.keys(st).filter((n) => n.startsWith(`${ROW}${s}\u0001`)).forEach((n) => st.removeItem(n));
    });
  }, null);
