import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

// Кнопка выбора: крупная иконка + подпись (ключ перевода или готовый текст).
export function createChoiceButton(item, onPick) {
  const btn = el('button', 'choice-btn');
  btn.type = 'button';
  const name = el('span', 'choice-name');
  if (item.labelKey) bindText(name, item.labelKey);
  else name.textContent = item.label;
  btn.append(el('span', 'choice-icon', item.icon), name);
  btn.addEventListener('click', () => onPick(item.id));
  return btn;
}
