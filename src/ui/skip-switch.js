import { SKIP_OPTIONS, getSkip, setSkip } from '../player/skip-pref.js';
import { createSegmented } from './segmented.js';

const items = SKIP_OPTIONS.map((n) => ({ id: n, label: String(n) }));

// Переключатель «сколько секунд перематывать»; kind — 'back' или 'forward'.
export const createSkipSwitch = (kind) => createSegmented(items, () => getSkip()[kind], (n) => setSkip(kind, n));
