import { t } from './i18n.js';

// Привязывает текст элемента к ключу перевода: при смене языка его обновит apply.js.
export function bindText(node, key, params) {
  node.dataset.i18n = key;
  if (params) node.dataset.i18nParams = JSON.stringify(params);
  node.textContent = t(key, params);
  return node;
}
