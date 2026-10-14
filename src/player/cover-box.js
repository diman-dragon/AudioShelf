import { el, image } from '../core/dom.js';
import { loadCover } from '../library/cover-queue.js';
import { applyPlaceholder } from '../library/placeholder.js';

// Большая обложка; если её нет — крупное эмодзи (книжка или диск, зависит от прогресса).
export function createCover() {
  const node = el('div', 'pcover');
  const ph = image('ph');
  const cover = image('pv');
  cover.onload = () => cover.classList.add('ready');
  node.append(ph, cover);
  return {
    node,
    refresh: (book) => applyPlaceholder(ph, book),
    show(book) {
      cover.classList.remove('ready');
      cover.removeAttribute('src');
      loadCover(book.files[0], cover);
    },
  };
}
