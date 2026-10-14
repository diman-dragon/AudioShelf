import { el } from '../core/dom.js';
import { createSegmented } from '../ui/segmented.js';
import { getScope, setScope, trackAvailable } from './eq-scope.js';

const items = [
  { id: 'global', labelKey: 'sound.global' },
  { id: 'track', labelKey: 'sound.track' },
];

// «Для всех / Для трека». Если трек не выбран, вторая кнопка неактивна.
export function createScopeSwitch() {
  const node = el('div', 'scope');
  const paint = () => {
    const seg = createSegmented(items, getScope, setScope);
    seg.children[1].disabled = !trackAvailable();
    node.replaceChildren(seg);
  };
  paint();
  return { node, paint };
}
