import { Player } from '../folder/plugins.js';
import { kindOf } from '../library/file-info.js';
import { getMode } from './mode-pref.js';
import { EDGES, nativeMode } from './modes.js';

export function applyProfile(book) {
  const kind = kindOf(book);
  return Player.setProfile({ ...nativeMode(getMode(kind)), ...EDGES[kind] }).catch(() => {});
}
