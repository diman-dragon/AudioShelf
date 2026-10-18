import { getTrack } from '../player/player-state.js';
import { applySound } from './apply-sound.js';

export function initSoundSync() {
  ['eqchange', 'normchange', 'boostchange', 'trackchange', 'premiumchange'].forEach((name) =>
    document.addEventListener(name, () => applySound(getTrack())),
  );
  applySound(getTrack());
}
