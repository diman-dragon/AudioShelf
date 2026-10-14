import { el } from '../core/dom.js';
import { createEqPanel } from '../sound/eq-panel.js';
import { createNormCard } from '../sound/norm-card.js';

// «Звук»: эквалайзер с пресетами и нормализация громкости.
export function renderSound() {
  const root = el('section', 'sound');
  root.append(createEqPanel().node, createNormCard().node);
  return root;
}
