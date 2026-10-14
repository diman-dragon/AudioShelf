import { el } from '../core/dom.js';
import { createKindTabs } from './kind-tabs.js';
import { createSearch } from './search-box.js';
import { createSortButton } from './sort-button.js';

// Под панелью кнопок: тип, поиск и сортировка.
export function createLibFilter() {
  const node = el('div', 'lib-filter');
  const row = el('div', 'lib-search-row');
  row.append(createSearch(), createSortButton());
  node.append(createKindTabs(), row);
  return node;
}
