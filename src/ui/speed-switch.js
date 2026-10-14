import { setSpeed } from '../player/player-actions.js';
import { loadSpeed } from '../player/progress-store.js';
import { SPEEDS, fmtSpeed } from '../player/speeds.js';
import { createSegmented } from './segmented.js';

const items = SPEEDS.map((v) => ({ id: v, label: fmtSpeed(v) }));

// Скорость воспроизведения; меняется сразу и у играющего.
export const createSpeedSwitch = () => createSegmented(items, loadSpeed, setSpeed);
