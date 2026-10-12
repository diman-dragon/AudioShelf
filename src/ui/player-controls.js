import { el } from '../core/dom.js';

const BUTTONS = [
  { emoji: '⏮️', label: 'Предыдущая' },
  { emoji: '⏪', label: 'Назад 15 секунд' },
  { emoji: '▶️', label: 'Играть', main: true },
  { emoji: '⏩', label: 'Вперёд 30 секунд' },
  { emoji: '⏭️', label: 'Следующая' },
];

// Кнопки управления. Пока без логики — только каркас.
export function createControls() {
  const row = el('div', 'controls');
  BUTTONS.forEach(({ emoji, label, main }) => {
    const btn = el('button', main ? 'ctrl ctrl-main' : 'ctrl', emoji);
    btn.type = 'button';
    btn.setAttribute('aria-label', label);
    row.append(btn);
  });
  return row;
}
