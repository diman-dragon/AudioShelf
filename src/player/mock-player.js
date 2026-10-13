// Заглушка плеера для браузера (npm run dev): время идёт по часам, звука нет.
const s = { loaded: false, uri: '', playing: false, position: 0, duration: 3600000, speed: 1, at: 0 };

function snap() {
  if (s.playing) {
    const now = Date.now();
    s.position = Math.min(s.duration, s.position + (now - s.at) * s.speed);
    s.at = now;
    if (s.position >= s.duration) s.playing = false;
  }
  return { ...s, loading: false, ended: s.loaded && s.position >= s.duration };
}

export const mockPlayer = {
  async load({ uri, position = 0, autoplay = false }) {
    Object.assign(s, { loaded: true, uri, position, playing: autoplay, at: Date.now() });
    return snap();
  },
  async play() {
    snap();
    s.playing = true;
    s.at = Date.now();
    return snap();
  },
  async pause() {
    snap();
    s.playing = false;
    return snap();
  },
  async seek({ position }) {
    snap();
    s.position = position;
    return snap();
  },
  async setSpeed({ speed }) {
    snap();
    s.speed = speed;
    return snap();
  },
  async stop() {
    Object.assign(s, { loaded: false, uri: '', playing: false, position: 0 });
    return snap();
  },
  async state() {
    return snap();
  },
};
