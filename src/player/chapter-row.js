import { el, button } from '../core/dom.js';
import { splitName } from '../library/format.js';
import { fmtTime } from './time.js';

// Строка списка: «3. Название» и длительность, если уже известна; играющая выделена.
export function createChapterRow(file, index, { current, duration, onPick }) {
  const row = button(current ? 'crow cur' : 'crow', () => onPick(index));
  row.append(el('span', 'cname', `${index + 1}. ${splitName(file.name)[0]}`), el('span', 'cdur', duration ? fmtTime(duration) : ''));
  return row;
}
