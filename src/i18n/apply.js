import { t } from './i18n.js';

// Перерисовывает все привязанные тексты после смены языка.
export function applyTranslations(root = document) {
  root.querySelectorAll('[data-i18n]').forEach((node) => {
    const params = node.dataset.i18nParams;
    node.textContent = t(node.dataset.i18n, params ? JSON.parse(params) : undefined);
  });
}
