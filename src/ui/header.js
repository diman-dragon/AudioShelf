import { el } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';

export function createHeader() {
  const node = el('header', 'header');
  const title = el('h1', 'header-title');
  node.append(title);
  return {
    node,
    setTitle: (key) => bindText(title, key),
  };
}
