// Круглая картинка нужного размера (px). Используется на экранах.
export function createArt(src, size, className = 'art') {
  const img = document.createElement('img');
  img.className = className;
  img.src = src;
  img.alt = '';
  img.draggable = false;
  img.style.width = `${size}px`;
  img.style.height = `${size}px`;
  return img;
}
