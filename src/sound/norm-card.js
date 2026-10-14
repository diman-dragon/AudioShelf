import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { getFileCount } from '../library/library-data.js';
import { createSegmented } from '../ui/segmented.js';
import { createRow } from '../ui/setting-row.js';
import { getNorm, pendingFiles, startNormalize, stopNormalize } from './normalizer.js';
import { statusText } from './norm-status.js';
import { isNormOn, setNormOn } from './norm-pref.js';

const SWITCH = [
  { id: 'on', labelKey: 'norm.on' },
  { id: 'off', labelKey: 'norm.off' },
];

// Нормализация громкости: кнопка с ходом работы, полоса «сколько файлов уже проанализировано», статус и переключатель.
export function createNormCard() {
  const node = el('div', 'normcard');
  const bar = el('div', 'norm-bar');
  const fill = el('div', 'norm-fill');
  bar.append(fill);
  const status = el('p', 'norm-status');
  const current = el('p', 'note');
  const run = button('btn', () => (getNorm().status === 'running' ? stopNormalize() : startNormalize()));
  node.append(
    bindText(el('h2', 'sound-h'), 'norm.title'),
    bindText(el('p', 'note'), 'norm.hint'),
    run,
    bar,
    status,
    current,
    createRow('norm.apply', createSegmented(SWITCH, () => (isNormOn() ? 'on' : 'off'), (id) => setNormOn(id === 'on'))),
  );

  const paint = () => {
    const state = getNorm();
    const total = getFileCount();
    const pending = pendingFiles().length;
    const running = state.status === 'running';
    run.textContent = running ? t('norm.stop', { n: state.done, m: state.total }) : t('norm.start');
    run.classList.toggle('ghost', running);
    run.disabled = !running && pending === 0;
    fill.style.width = `${total ? ((total - pending) / total) * 100 : 0}%`;
    bar.classList.toggle('busy', running);
    status.textContent = statusText(state, pending, total);
    current.textContent = running ? state.current : '';
    current.hidden = !running || !state.current;
  };
  ['normchange', 'librarychange', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return { node };
}
