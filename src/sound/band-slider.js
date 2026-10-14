import { el } from '../core/dom.js';
import { t } from '../i18n/i18n.js';
import { MAX_DB, MIN_DB } from './bands.js';

const SPAN = MAX_DB - MIN_DB;
const fmtDb = (db) => (db > 0 ? `+${db}` : String(db));

// Вертикальный ползунок одной полосы: ведём пальцем, шаг 1 дБ, ноль — посередине.
export function createBandSlider(label, onChange) {
  const node = el('div', 'band');
  const value = el('span', 'band-db');
  const track = el('div', 'band-track');
  const fill = el('div', 'band-fill');
  const thumb = el('div', 'band-thumb');
  track.append(fill, thumb);
  node.append(value, track, el('span', 'band-hz', label));
  Object.assign(track, { tabIndex: 0 });
  track.setAttribute('role', 'slider');
  track.setAttribute('aria-valuemin', MIN_DB);
  track.setAttribute('aria-valuemax', MAX_DB);

  let current = 0;
  const fromY = (y) => {
    const box = track.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, 1 - (y - box.top) / box.height));
    return Math.round(MIN_DB + ratio * SPAN);
  };
  track.addEventListener('pointerdown', (e) => {
    track.setPointerCapture(e.pointerId);
    onChange(fromY(e.clientY));
  });
  track.addEventListener('pointermove', (e) => track.hasPointerCapture(e.pointerId) && onChange(fromY(e.clientY)));
  track.addEventListener('keydown', (e) => {
    const step = { ArrowUp: 1, ArrowRight: 1, ArrowDown: -1, ArrowLeft: -1 }[e.key];
    if (step) onChange(Math.max(MIN_DB, Math.min(MAX_DB, current + step)));
  });

  return {
    node,
    set(db) {
      current = db;
      const ratio = (db - MIN_DB) / SPAN;
      thumb.style.bottom = `${ratio * 100}%`;
      fill.style.bottom = db >= 0 ? '50%' : `${ratio * 100}%`;
      fill.style.height = `${(Math.abs(db) / SPAN) * 100}%`;
      value.textContent = fmtDb(db);
      track.setAttribute('aria-valuenow', db);
      track.setAttribute('aria-label', t('sound.band', { f: label, db: fmtDb(db) }));
    },
  };
}
