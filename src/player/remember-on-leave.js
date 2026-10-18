import { onNavigate } from '../app/router.js';
import { getStatus, getTrack, isLoaded } from './player-state.js';
import { remember } from './remember.js';

function save() {
  const track = getTrack();
  const status = getStatus();
  if (track && status.playing && isLoaded()) remember(track, status, status);
}

export function initRememberOnLeave() {
  onNavigate((id) => id === 'library' && save());
  document.addEventListener('visibilitychange', () => document.hidden && save());
}
