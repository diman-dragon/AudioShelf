import { el } from '../core/dom.js';
import { createEmptyState } from '../ui/empty-state.js';
import libraryArt from '../assets/art/library.webp';

export function renderLibrary() {
  const root = el('section', 'library');
  root.append(createEmptyState(libraryArt, 'library.emptyTitle', 'library.emptyHint'));
  return root;
}
