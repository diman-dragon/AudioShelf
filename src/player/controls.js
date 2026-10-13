import { el, button } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { BACK_ICON, FORWARD_ICON, PAUSE_ICON, PLAY_ICON } from './icons.js';
import { fmtSpeed } from './speeds.js';

function iconButton(className, icon, ariaKey, onClick) {
  const btn = button(className, onClick);
  btn.innerHTML = icon;
  return bindAria(btn, ariaKey);
}

// Ряд кнопок: назад 15 с, пуск/пауза, вперёд 30 с и скорость.
export function createControls({ onBack, onToggle, onForward, onSpeed }) {
  const node = el('div', 'pcontrols');
  const toggle = iconButton('pplay', PLAY_ICON, 'player.play', onToggle);
  const speed = button('pspeed', onSpeed);
  bindAria(speed, 'player.speed');
  node.append(iconButton('pskip', BACK_ICON, 'player.back', onBack), toggle, iconButton('pskip', FORWARD_ICON, 'player.forward', onForward), speed);

  return {
    node,
    update(playing, speedValue) {
      toggle.innerHTML = playing ? PAUSE_ICON : PLAY_ICON;
      bindAria(toggle, playing ? 'player.pause' : 'player.play');
      speed.textContent = fmtSpeed(speedValue);
    },
  };
}
