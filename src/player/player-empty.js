import { el, button, image } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { goHome } from '../app/router.js';
import playerEmoji from '../assets/emoji/1f3a7.svg';

export function createPlayerEmpty() {
  const node = el('div', 'empty');
  node.append(
    image('em', playerEmoji),
    bindText(el('div', 'empty-title'), 'player.empty'),
    bindText(el('div', 'muted'), 'player.emptyHint'),
    bindText(button('btn', goHome), 'player.toLibrary'),
  );
  return node;
}
