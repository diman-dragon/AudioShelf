import { FLAT_BANDS } from './bands.js';

// Пресеты: усиление по 10 полосам (дБ), от 31 Гц до 16 кГц. Название — ключ preset.<id>.
export const PRESETS = [
  { id: 'flat', bands: FLAT_BANDS },
  { id: 'voice', bands: [-6, -5, -3, -1, 1, 3, 4, 4, 2, 0] },
  { id: 'rock', bands: [5, 4, 3, 1, -1, -1, 1, 3, 4, 5] },
  { id: 'pop', bands: [-1, 1, 3, 4, 3, 0, -1, -1, 1, 2] },
  { id: 'jazz', bands: [3, 2, 1, 2, -1, -1, 0, 1, 2, 3] },
  { id: 'classical', bands: [3, 2, 1, 0, 0, 0, -1, -1, 2, 3] },
  { id: 'electronic', bands: [5, 4, 1, 0, -2, 2, 1, 3, 4, 5] },
  { id: 'bass', bands: [6, 5, 4, 2, 1, 0, 0, 0, 0, 0] },
  { id: 'treble', bands: [0, 0, 0, 0, 0, 1, 3, 4, 5, 6] },
];

export const presetBands = (id) => PRESETS.find((p) => p.id === id)?.bands ?? FLAT_BANDS;
