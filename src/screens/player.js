import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createCover } from '../ui/player-cover.js';
import { createProgress } from '../ui/player-progress.js';
import { createControls } from '../ui/player-controls.js';

function createTitleBlock() {
  const box = el('div', 'player-info');
  box.append(
    bindText(el('div', 'player-title'), 'player.idleTitle'),
    bindText(el('div', 'player-subtitle'), 'player.idleHint'),
  );
  return box;
}

export function renderPlayer() {
  const root = el('section', 'player');
  root.append(createCover(), createTitleBlock(), createProgress(), createControls());
  return root;
}
