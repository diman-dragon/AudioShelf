import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { pickPreset } from './eq-actions.js';
import { PRESETS } from './presets.js';

// Пресеты кнопками-«таблетками»; выбранный подсвечен (при ручной подстройке подсветки нет).
export function createPresetChips() {
  const node = el('div', 'chips');
  const chips = new Map();
  PRESETS.forEach((preset) => {
    const chip = bindText(button('chip', () => pickPreset(preset.id)), `preset.${preset.id}`);
    node.append(chip);
    chips.set(preset.id, chip);
  });
  return {
    node,
    mark: (id) => chips.forEach((chip, key) => chip.classList.toggle('on', key === id)),
  };
}
