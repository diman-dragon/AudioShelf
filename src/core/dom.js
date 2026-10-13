// Мини-хелперы для создания DOM-элементов.
export function el(tag, className = '', text = '') {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function button(className = '', onClick) {
  const node = el('button', className);
  node.type = 'button';
  if (onClick) node.addEventListener('click', onClick);
  return node;
}

export function image(className, src) {
  const node = el('img', className);
  node.alt = '';
  node.draggable = false;
  if (src) node.src = src;
  return node;
}
