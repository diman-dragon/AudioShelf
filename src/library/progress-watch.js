import { paintProgress } from './item.js';

// Во время прослушивания полоски в библиотеке обновляются на месте, без перерисовки списка.
export function initProgressWatch() {
  document.addEventListener('progresschange', () => document.querySelectorAll('.fprog').forEach(paintProgress));
}
