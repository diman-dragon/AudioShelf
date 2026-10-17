import { el } from '../core/dom.js';
import { getScreen, onNavigate } from '../app/router.js';
import { bindText } from '../i18n/bind.js';
import { energyStatusKey } from '../player/energy-status.js';
import { isOffloadOn, setOffloadOn } from '../player/energy-pref.js';
import { createSegmented } from './segmented.js';

const items = [
  { id: 'on', labelKey: 'energy.on' },
  { id: 'off', labelKey: 'energy.off' },
];
const SETTLE_MS = 400; // нативная часть узнаёт о смене звука чуть позже JS

// Переключатель «аппаратное воспроизведение» с пояснением и строкой состояния (работает ли оно сейчас на этом телефоне).
export function createEnergySwitch() {
  const node = el('div', 'energy');
  const status = el('div', 'note energy-status');
  status.hidden = true;
  node.append(
    createSegmented(items, () => (isOffloadOn() ? 'on' : 'off'), (id) => setOffloadOn(id === 'on').then(refresh)),
    bindText(el('div', 'note'), 'energy.hint'),
    status,
  );

  let timer = 0;
  async function refresh() {
    const key = await energyStatusKey();
    status.hidden = !key;
    if (key) bindText(status, key);
  }
  const later = () => {
    clearTimeout(timer);
    if (getScreen() === 'settings') timer = setTimeout(refresh, SETTLE_MS); // только пока открыты настройки
  };

  ['eqchange', 'normchange', 'boostchange', 'premiumchange'].forEach((name) => document.addEventListener(name, later));
  onNavigate((id) => id === 'settings' && later());
  refresh();
  return node;
}
