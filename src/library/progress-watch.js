import { paintProgress } from './item.js';
import { applyPlaceholder } from './placeholder.js';

// Книга сохранила прогресс (изменился процент или состояние «начата/дослушана»): в списке меняется только её строка —
// полоска и значок-эмодзи, без перерисовки списка.
export function initProgressWatch() {
  document.addEventListener('progresschange', (e) => {
    document.querySelectorAll('.fprog').forEach((bar) => {
      if (bar.dataset.id !== e.detail) return;
      paintProgress(bar);
      const item = bar.closest('.item');
      const ph = item?.querySelector('.ph');
      if (ph && item._book) applyPlaceholder(ph, item._book);
    });
  });
}
