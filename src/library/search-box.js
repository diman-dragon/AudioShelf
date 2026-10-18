import { el } from '../core/dom.js';
import { t } from '../i18n/i18n.js';
import { setFilter } from './filter-state.js';

const DELAY = 150;

export function createSearch() {
  const input = el('input', 'lib-search');
  Object.assign(input, { type: 'search', autocomplete: 'off', enterKeyHint: 'search' });
  const label = () => {
    input.placeholder = t('lib.search');
    input.setAttribute('aria-label', t('lib.search'));
  };
  let timer = 0;
  input.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => setFilter({ query: input.value }), DELAY);
  });
  document.addEventListener('langchange', label);
  label();
  return input;
}
