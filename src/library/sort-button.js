import { button } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { openSheet } from '../ui/sheet.js';
import { SORTS, getFilter, setFilter } from './filter-state.js';
import { SORT_ICON } from './icons.js';

// Кнопка «Сортировка»: нижнее меню, текущий вариант отмечен галочкой.
function openMenu() {
  const current = getFilter().sort;
  openSheet(t('lib.sort'), SORTS.map((id) => ({
    label: `${id === current ? '✓' : ' '} ${t(`sort.${id}`)}`,
    run: () => setFilter({ sort: id }),
  })));
}

export function createSortButton() {
  const btn = button('icon-btn', openMenu);
  btn.innerHTML = SORT_ICON;
  return bindAria(btn, 'lib.sort');
}
