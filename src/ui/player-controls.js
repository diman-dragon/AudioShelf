import { el } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';

const BUTTONS = [
  { emoji: '⏮️', key: 'player.prev' },
  { emoji: '⏪', key: 'player.back', params: { n: 15 } },
  { emoji: '▶️', key: 'player.play', main: true },
  { emoji: '⏩', key: 'player.forward', params: { n: 30 } },
  { emoji: '⏭️', key: 'player.next' },
];

// Кнопки управления. Пока без логики — только каркас.
export function createControls() {
  const row = el('div', 'controls');
  BUTTONS.forEach(({ emoji, key, params, main }) => {
    const btn = el('button', main ? 'ctrl ctrl-main' : 'ctrl', emoji);
    btn.type = 'button';
    row.append(bindAria(btn, key, params));
  });
  return row;
}
