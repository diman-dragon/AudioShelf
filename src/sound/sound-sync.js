import { getTrack } from '../player/player-state.js';
import { applySound } from './apply-sound.js';

// Любое изменение настроек звука или смена трека — заново отправляем звук плееру.
export function initSoundSync() {
  ['eqchange', 'normchange', 'trackchange'].forEach((name) =>
    document.addEventListener(name, () => applySound(getTrack())),
  );
  applySound(getTrack());
}
