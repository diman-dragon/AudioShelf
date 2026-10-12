import { el } from '../core/dom.js';

// Узкая шапка с названием текущего экрана.
export function createHeader() {
  const node = el('header', 'header');
  const title = el('h1', 'header-title');
  node.append(title);
  return {
    node,
    setTitle: (text) => {
      title.textContent = text;
    },
  };
}
