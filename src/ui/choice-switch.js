import { el } from '../core/dom.js';
import { createChoiceButton } from './choice-button.js';

// Переключатель «один из N»: по кнопке на вариант, выбранный подсвечен.
export function createChoiceSwitch(items, getCurrent, onPick) {
  const box = el('div', 'choice');
  const buttons = new Map();

  const mark = (id) => {
    buttons.forEach((btn, key) => {
      btn.classList.toggle('active', key === id);
      btn.setAttribute('aria-pressed', String(key === id));
    });
  };

  items.forEach((item) => {
    const btn = createChoiceButton(item, (id) => {
      onPick(id);
      mark(id);
    });
    box.append(btn);
    buttons.set(item.id, btn);
  });

  mark(getCurrent());
  return box;
}
