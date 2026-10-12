import { el } from '../core/dom.js';
import { createArt } from './art.js';

export function createEmptyState(art, title, hint) {
  const box = el('div', 'empty');
  box.append(
    createArt(art, 220, 'empty-art'),
    el('div', 'empty-title', title),
    el('div', 'empty-hint', hint),
  );
  return box;
}
