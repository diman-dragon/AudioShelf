import { progressState } from '../player/progress-store.js';
import { kindOf } from './file-info.js';
import discEmoji from '../assets/emoji/1f4bf.svg';
import closedBook from '../assets/emoji/1f4d5.svg';
import openBook from '../assets/emoji/1f4d6.svg';

// Эмодзи вместо обложки. Книга: не открывали — закрытая (корешок слева),
// начата — открытая, дослушана — закрытая, но корешком вправо. Альбом — диск.
export function placeholderFor(file) {
  if (kindOf(file) === 'album') return { src: discEmoji, flip: false };
  const state = progressState(file.uri);
  return { src: state === 'started' ? openBook : closedBook, flip: state === 'done' };
}

export function applyPlaceholder(img, file) {
  const { src, flip } = placeholderFor(file);
  if (img.getAttribute('src') !== src) img.src = src;
  img.classList.toggle('flip', flip);
}
