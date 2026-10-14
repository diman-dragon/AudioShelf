import { progressState } from '../player/progress-store.js';
import { kindOf } from './file-info.js';
import discEmoji from '../assets/emoji/1f4bf.svg';
import closedBook from '../assets/emoji/1f4d5.svg';
import openBook from '../assets/emoji/1f4d6.svg';

// Эмодзи вместо обложки. Книга: не открывали — закрытая (корешок слева),
// начата — открытая, дослушана — закрытая, но корешком вправо. Альбом — диск.
export function placeholderFor(book) {
  if (kindOf(book) === 'album') return { src: discEmoji, flip: false };
  const state = progressState(book.id);
  return { src: state === 'started' ? openBook : closedBook, flip: state === 'done' };
}

export function applyPlaceholder(img, book) {
  const { src, flip } = placeholderFor(book);
  if (img.getAttribute('src') !== src) img.src = src;
  img.classList.toggle('flip', flip);
}
