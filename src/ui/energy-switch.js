import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { isOffloadOn, setOffloadOn } from '../player/energy-pref.js';
import { createSegmented } from './segmented.js';

const items = [
  { id: 'on', labelKey: 'energy.on' },
  { id: 'off', labelKey: 'energy.off' },
];

// Переключатель «аппаратное воспроизведение» с пояснением.
export function createEnergySwitch() {
  const node = el('div', 'energy');
  node.append(
    createSegmented(items, () => (isOffloadOn() ? 'on' : 'off'), (id) => setOffloadOn(id === 'on')),
    bindText(el('div', 'note'), 'energy.hint'),
  );
  return node;
}
