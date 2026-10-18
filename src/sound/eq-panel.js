import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { titleOf } from '../library/file-info.js';
import { getTrack } from '../player/player-state.js';
import { createEqBands } from './eq-bands.js';
import { hasOwnEq, resetBands, shownEq, useGlobalEq } from './eq-actions.js';
import { editingId, trackAvailable } from './eq-scope.js';
import { createPresetChips } from './preset-chips.js';
import { createScopeSwitch } from './scope-switch.js';

export function createEqPanel() {
  const node = el('div', 'eqpanel');
  const scope = createScopeSwitch();
  const note = el('p', 'note');
  const chips = createPresetChips();
  const bands = createEqBands();
  const actions = el('div', 'eq-actions');
  const reset = bindText(button('btn ghost', resetBands), 'sound.reset');
  const own = bindText(button('btn ghost', useGlobalEq), 'sound.useGlobal');
  actions.append(reset, own);
  node.append(
    bindText(el('h2', 'sound-h'), 'sound.eq'),
    scope.node,
    note,
    bindText(el('h3', 'sound-sub'), 'sound.presets'),
    chips.node,
    bindText(el('h3', 'sound-sub'), 'sound.bands'),
    bands.node,
    actions,
  );

  const paint = () => {
    const eq = shownEq();
    scope.paint();
    bands.update(eq.bands);
    chips.mark(eq.preset);
    note.textContent = editingId()
      ? t('sound.editingTrack', { t: titleOf(getTrack()) })
      : t(trackAvailable() ? 'sound.editingGlobal' : 'sound.noTrack');
    own.hidden = !hasOwnEq();
  };
  ['eqchange', 'trackchange', 'langchange', 'librarychange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return { node };
}
