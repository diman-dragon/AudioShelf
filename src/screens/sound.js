import { el } from '../core/dom.js';
import { createLockBanner, lockable } from '../billing/lock.js';
import { createBoostCard } from '../sound/boost-card.js';
import { createEqPanel } from '../sound/eq-panel.js';
import { createNormCard } from '../sound/norm-card.js';

// «Звук»: усиление громкости (для всех), эквалайзер с пресетами и нормализация (Премиум).
export function renderSound() {
  const root = el('section', 'sound');
  root.append(createBoostCard().node, createLockBanner(), lockable(createEqPanel().node), lockable(createNormCard().node));
  return root;
}
