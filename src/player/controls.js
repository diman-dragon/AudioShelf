import { el, button } from '../core/dom.js';
import { bindAria } from '../i18n/bind.js';
import { NEXT_ICON, PAUSE_ICON, PLAY_ICON, PREV_ICON, backIcon, forwardIcon } from './icons.js';

function iconButton(className, icon, ariaKey, onClick) {
  const btn = button(className, onClick);
  btn.innerHTML = icon;
  return bindAria(btn, ariaKey);
}

export function createControls({ onPrev, onBack, onToggle, onForward, onNext }) {
  const node = el('div', 'pcontrols');
  const prev = iconButton('pskip', PREV_ICON, 'player.prev', onPrev);
  const next = iconButton('pskip', NEXT_ICON, 'player.next', onNext);
  const back = iconButton('pskip', '', 'player.back', onBack);
  const forward = iconButton('pskip', '', 'player.forward', onForward);
  const toggle = iconButton('pplay', PLAY_ICON, 'player.play', onToggle);
  node.append(prev, back, toggle, forward, next);

  let shownSkip = '';
  let shownPlaying = null;
  return {
    node,
    update({ playing, multi, skip }) {
      if (playing !== shownPlaying) {
        shownPlaying = playing;
        toggle.innerHTML = playing ? PAUSE_ICON : PLAY_ICON;
        bindAria(toggle, playing ? 'player.pause' : 'player.play');
      }
      prev.hidden = false;
      next.hidden = false;
      next.disabled = !multi;
      const key = `${skip.back}/${skip.forward}`;
      if (key === shownSkip) return;
      shownSkip = key;
      back.innerHTML = backIcon(skip.back);
      forward.innerHTML = forwardIcon(skip.forward);
      bindAria(back, 'player.back', { s: skip.back });
      bindAria(forward, 'player.forward', { s: skip.forward });
    },
  };
}
