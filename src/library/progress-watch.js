import { paintProgress } from './item.js';
import { applyPlaceholder } from './placeholder.js';

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
