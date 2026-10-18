import { el, button } from '../core/dom.js';
import { bindAria, bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { LIST_ICON, MARK_ICON, MODE_ICONS, MOON_ICON } from './mode-icons.js';
import { fmtSpeed } from './speeds.js';
import { fmtTime } from './time.js';

function extra(onClick) {
  const btn = button('pextra', onClick);
  const icon = el('span', 'pi');
  const label = el('span', 'pl');
  btn.append(icon, label);
  return { btn, icon, label };
}

function sleepLabel({ choice, left, end }) {
  if (end) return t('sleep.chapter');
  if (left > 0) return fmtTime(left);
  return choice === 'off' ? t('player.sleep') : t('sleep.min', { n: choice });
}

export function createExtras({ onMarks, onList, onMode, onSpeed, onSleep }) {
  const node = el('div', 'pextras');
  const marks = extra(onMarks);
  const list = extra(onList);
  const mode = extra(onMode);
  const speed = extra(onSpeed);
  const sleep = extra(onSleep);
  marks.icon.innerHTML = MARK_ICON;
  list.icon.innerHTML = LIST_ICON;
  sleep.icon.innerHTML = MOON_ICON;
  speed.icon.classList.add('pi-text');
  bindText(list.label, 'player.list');
  bindText(speed.label, 'player.speed');
  bindAria(speed.btn, 'player.speed');
  bindAria(sleep.btn, 'player.sleep');
  node.append(speed.btn, sleep.btn, list.btn, mode.btn, marks.btn);
  return {
    node,
    update({ markCount, mode: current, multi, speed: rate, sleep: sleepState }) {
      list.btn.disabled = !multi;
      bindText(marks.label, markCount ? 'player.marksN' : 'player.marks', { n: markCount });
      bindText(mode.label, `mode.short.${current}`);
      bindAria(mode.btn, `mode.${current}`);
      mode.icon.innerHTML = MODE_ICONS[current];
      speed.icon.textContent = fmtSpeed(rate);
      this.updateSleep(sleepState);
    },
    updateSleep(sleepState) {
      sleep.label.textContent = sleepLabel(sleepState);
      sleep.btn.classList.toggle('on', sleepState.choice !== 'off');
    },
  };
}
