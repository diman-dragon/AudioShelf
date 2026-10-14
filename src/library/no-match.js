import { el, image } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import libraryEmoji from '../assets/emoji/1f4da.svg';

// Фильтр или поиск ничего не нашли.
export function createNoMatch() {
  const node = el('div', 'empty');
  node.append(image('em', libraryEmoji), bindText(el('div', 'empty-title'), 'lib.noMatch'), bindText(el('div', 'muted'), 'lib.noMatchHint'));
  return node;
}
