import { createSegmented } from '../ui/segmented.js';
import { getFilter, setFilter } from './filter-state.js';

const items = [
  { id: 'all', labelKey: 'filter.all' },
  { id: 'book', labelKey: 'filter.books' },
  { id: 'album', labelKey: 'filter.albums' },
];

// Переключатель «Все / Книги / Альбомы».
export const createKindTabs = () => createSegmented(items, () => getFilter().kind, (kind) => setFilter({ kind }));
