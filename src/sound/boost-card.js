import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createSegmented } from '../ui/segmented.js';
import { BOOSTS, getBoost, setBoost } from './boost-pref.js';

const items = BOOSTS.map((p) => ({ id: p, label: `${p}%` }));

// «Усиление громкости»: 100% — как в файле; больше — программное усиление с мягким ограничителем пиков.
export function createBoostCard() {
  const node = el('div', 'boostcard');
  node.append(
    bindText(el('h2', 'sound-h'), 'boost.title'),
    bindText(el('p', 'note'), 'boost.hint'),
    createSegmented(items, getBoost, setBoost),
    bindText(el('p', 'note boost-warn'), 'boost.warn'),
  );
  return { node };
}
