import { readStored, writeStored } from '../core/storage.js';
import { getTrack } from '../player/player-state.js';

// Что настраиваем: «для всех» или «для трека». Если трек не выбран — только для всех.
const KEY = 'audioshelf.eq.scope';

export const trackAvailable = () => Boolean(getTrack());

export const getScope = () => (readStored(KEY) === 'track' && trackAvailable() ? 'track' : 'global');

export function setScope(scope) {
  writeStored(KEY, scope);
  document.dispatchEvent(new Event('eqchange'));
}

// id книги, чей эквалайзер сейчас правим (null — общий).
export const editingId = () => (getScope() === 'track' ? getTrack().id : null);
