import { kindOf } from '../library/file-info.js';
import { applyProfile } from './apply-profile.js';
import { nextMode } from './mode-pref.js';
import { getTrack, isLoaded } from './player-state.js';

export async function cycleMode() {
  const book = getTrack();
  if (!book) return;
  nextMode(kindOf(book));
  if (isLoaded()) await applyProfile(book);
  document.dispatchEvent(new Event('modechange'));
}
