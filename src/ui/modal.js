import { el } from '../core/dom.js';

let node = null;

export const isModalOpen = () => node !== null;

export function closeModal() {
  node?.remove();
  node = null;
}

export function openModal(title, content) {
  closeModal();
  const box = el('div', 'modal-box');
  box.append(el('h2', 'modal-title', title), ...content);
  node = el('div', 'modal');
  node.append(box);
  node.addEventListener('click', (e) => {
    if (e.target === node) closeModal();
  });
  document.body.append(node);
}
