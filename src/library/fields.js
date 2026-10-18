import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

export function textInput(value = '', placeholder = '') {
  const input = el('input');
  Object.assign(input, { type: 'text', value, placeholder, autocomplete: 'off' });
  return input;
}

export function createField(labelKey, hintKey, control, hintParams) {
  const node = el('label', 'field');
  const label = bindText(el('span', 'field-label'), labelKey);
  node.append(label, control, bindText(el('span', 'field-hint'), hintKey, hintParams));
  return { node, label };
}
