import { el } from '../core/dom.js';
import { createLockBanner, lockable } from '../billing/lock.js';
import { createEqPanel } from '../sound/eq-panel.js';
import { createNormCard } from '../sound/norm-card.js';

// «Звук»: эквалайзер с пресетами и нормализация громкости (Премиум).
export function renderSound() {
  const root = el('section', 'sound');
  root.append(createLockBanner(), lockable(createEqPanel().node), lockable(createNormCard().node));
  return root;
}
