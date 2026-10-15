import { el } from '../core/dom.js';
import { t } from '../i18n/i18n.js';
import { SLEEP_OPTIONS, getSleepChoice, setSleepChoice } from '../player/sleep-actions.js';
import { getStatus } from '../player/player-state.js';
import { fmtTime } from '../player/time.js';
import { createSegmented } from './segmented.js';

const items = SLEEP_OPTIONS.map((id) => (id === 'off' || id === 'chapter' ? { id, labelKey: `sleep.${id}` } : { id, label: String(id) }));

// Таймер сна: выбор и строка «осталось …».
export function createSleepSwitch() {
  const node = el('div', 'sleep');
  const status = el('div', 'note');
  const paint = () => {
    const { sleepLeft, sleepEnd } = getStatus();
    status.textContent = sleepEnd ? t('sleep.chapterEnd') : sleepLeft > 0 ? t('sleep.left', { t: fmtTime(sleepLeft) }) : '';
    status.hidden = !status.textContent;
  };
  node.append(createSegmented(items, getSleepChoice, setSleepChoice), status);
  ['playerchange', 'sleepchange', 'langchange'].forEach((name) => document.addEventListener(name, paint));
  paint();
  return node;
}
