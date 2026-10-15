// Заглушка плеера для браузера (npm run dev): время идёт по часам, звука нет.
const CHAPTER = 600000; // каждая глава 10 минут
const s = { items: [], index: 0, playing: false, position: 0, speed: 1, at: 0, sleepLeft: 0, sleepEnd: false };

function advance() {
  if (!s.playing) return;
  const now = Date.now();
  if (s.sleepLeft > 0) {
    s.sleepLeft = Math.max(0, s.sleepLeft - (now - s.at));
    if (s.sleepLeft === 0) s.playing = false; // таймер сна сработал
  }
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
    sleepLeft: s.sleepLeft,
    sleepEnd: s.sleepEnd,
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
  setProfile: act(() => {}), // режимы и края файла — только на телефоне
  state: async () => snap(),
  addListener: async () => ({ remove: async () => {} }), // в браузере событий нет, остаётся опрос
};

// Таймер сна в браузере: тот же счёт по часам, пока играет.
export const mockSleep = {
  setSleep: act(({ minutes = 0, chapter = false }) => Object.assign(s, { sleepLeft: minutes * 60000, sleepEnd: chapter })),
};

// Нативная память места и настройка энергосбережения: в браузере их нет.
export const mockResume = { saved: async () => ({}) };
export const mockEnergy = { setOffload: async () => {} };
