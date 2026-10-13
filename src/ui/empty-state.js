import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createArt } from './art.js';

export function createEmptyState(art, titleKey, hintKey) {
  const box = el('div', 'empty');
  box.append(
    createArt(art, 220, 'empty-art'),
    bindText(el('div', 'empty-title'), titleKey),
    bindText(el('div', 'empty-hint'), hintKey),
  );
  return box;
}
