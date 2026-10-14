// Заглушка плеера для браузера (npm run dev): время идёт по часам, звука нет.
const CHAPTER = 600000; // каждая глава 10 минут
const s = { items: [], index: 0, playing: false, position: 0, speed: 1, at: 0 };

function advance() {
  if (!s.playing) return;
  const now = Date.now();
  s.position += (now - s.at) * s.speed;
  s.at = now;
  while (s.position >= CHAPTER && s.index < s.items.length - 1) {
    s.position -= CHAPTER;
    s.index++;
  }
  if (s.position >= CHAPTER) {
    s.position = CHAPTER;
    s.playing = false;
  }
}

function snap() {
  advance();
  const loaded = s.items.length > 0;
  return {
    loaded,
    uri: loaded ? s.items[s.index].uri : '',
    index: s.index,
    count: s.items.length,
    playing: s.playing,
    loading: false,
    ended: loaded && !s.playing && s.index === s.items.length - 1 && s.position >= CHAPTER,
    position: s.position,
    duration: loaded ? CHAPTER : 0,
    speed: s.speed,
  };
}

const act = (change) => async (args = {}) => {
  advance();
  change(args);
  s.at = Date.now();
  return snap();
};

export const mockPlayer = {
  load: act(({ items, index = 0, position = 0, autoplay = false }) =>
    Object.assign(s, { items, index, position, playing: autoplay })),
  play: act(() => (s.playing = true)),
  pause: act(() => (s.playing = false)),
  seek: act(({ position }) => (s.position = position)),
  next: act(() => Object.assign(s, { index: Math.min(s.items.length - 1, s.index + 1), position: 0 })),
  previous: act(() => Object.assign(s, { index: Math.max(0, s.index - 1), position: 0 })),
  skipTo: act(({ index }) => Object.assign(s, { index, position: 0 })),
  setSpeed: act(({ speed }) => (s.speed = speed)),
  stop: act(() => Object.assign(s, { items: [], index: 0, playing: false, position: 0 })),
  state: async () => snap(),
};
