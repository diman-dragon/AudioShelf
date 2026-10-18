export const mockSound = { setSound: async () => {} };

let cancelled = false;
let listeners = new Set();
const emit = (e) => listeners.forEach((fn) => fn(e));

export const mockLoudness = {
  async addListener(_name, fn) {
    listeners.add(fn);
    return { remove: async () => listeners.delete(fn) };
  },
  async start({ files }) {
    cancelled = false;
    (async () => {
      for (const f of files) {
        if (cancelled) break;
        emit({ type: 'start', name: f.name });
        await new Promise((r) => setTimeout(r, 700));
        if (cancelled) break;
        emit({ type: 'file', uri: f.uri, name: f.name, gain: -2.5 });
      }
      emit({ type: 'finish', status: cancelled ? 'cancelled' : 'done', failed: 0 });
    })();
    return { started: true };
  },
  async cancel() {
    cancelled = true;
  },
};
