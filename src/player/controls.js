import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { BACK_ICON, FORWARD_ICON, NEXT_ICON, PAUSE_ICON, PLAY_ICON, PREV_ICON } from './icons.js';
import { fmtSpeed } from './speeds.js';

function iconButton(className, icon, ariaKey, onClick) {
  const btn = button(className, onClick);
  btn.innerHTML = icon;
  return bindAria(btn, ariaKey);
}

// Кнопки: [глава −] [−15 с] [пуск/пауза] [+30 с] [глава +], ниже — скорость и «Главы».
export function createControls({ onPrev, onBack, onToggle, onForward, onNext, onSpeed, onChapters }) {
  const node = el('div', 'pcontrols');
  const prev = iconButton('pskip', PREV_ICON, 'player.prev', onPrev);
  const next = iconButton('pskip', NEXT_ICON, 'player.next', onNext);
  const toggle = iconButton('pplay', PLAY_ICON, 'player.play', onToggle);
  const main = el('div', 'prow');
  main.append(prev, iconButton('pskip', BACK_ICON, 'player.back', onBack), toggle, iconButton('pskip', FORWARD_ICON, 'player.forward', onForward), next);

  const speed = bindAria(button('pspeed', onSpeed), 'player.speed');
  const chapters = bindText(button('pspeed', onChapters), 'player.chapters');
  const extra = el('div', 'prow');
  extra.append(speed, chapters);
  node.append(main, extra);

  return {
    node,
    update({ playing, speed: value, multi }) {
      toggle.innerHTML = playing ? PAUSE_ICON : PLAY_ICON;
      bindAria(toggle, playing ? 'player.pause' : 'player.play');
      speed.textContent = fmtSpeed(value);
      [prev, next, chapters].forEach((node) => (node.hidden = !multi)); // главы бывают только у книги-папки
    },
  };
}
