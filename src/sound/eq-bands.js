import { el } from '../core/dom.js';
import { LABELS } from './bands.js';
import { createBandSlider } from './band-slider.js';
import { setBand } from './eq-actions.js';

export function createEqBands() {
  const node = el('div', 'bands');
  const sliders = LABELS.map((label, i) => createBandSlider(label, (db) => setBand(i, db)));
  node.append(...sliders.map((s) => s.node));
  return {
    node,
    update: (bands) => sliders.forEach((s, i) => s.set(bands[i])),
  };
}
