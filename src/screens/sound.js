import { el, image } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import soundEmoji from '../assets/emoji/1f50a.svg';

// «Звук»: сюда лягут настройки звука воспроизведения. Пока пусто.
export function renderSound() {
  const root = el('section', 'sound');
  const note = el('div', 'empty');
  note.append(image('em', soundEmoji), bindText(el('div', 'empty-title'), 'sound.title'), bindText(el('div', 'muted'), 'sound.hint'));
  root.append(note);
  return root;
}
