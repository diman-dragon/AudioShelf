import { paintProgress } from './item.js';

// Во время прослушивания полоски в библиотеке обновляются на месте, без перерисовки списка:
// сохранилась одна книга — перекрашиваем только её полоску.
export function initProgressWatch() {
  document.addEventListener('progresschange', (e) => {
    const id = e.detail;
    const bars = id ? [...document.querySelectorAll('.fprog')].filter((b) => b.dataset.id === id) : document.querySelectorAll('.fprog');
    bars.forEach(paintProgress);
  });
}
