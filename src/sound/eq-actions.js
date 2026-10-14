import { editingId } from './eq-scope.js';
import { clearTrackEq, effectiveEq, getGlobalEq, getTrackEq, setGlobalEq, setTrackEq } from './eq-store.js';
import { presetBands } from './presets.js';

const changed = () => document.dispatchEvent(new Event('eqchange'));

// То, что сейчас показано на экране: у трека — его настройка (или общая, пока своей нет).
export const shownEq = () => {
  const id = editingId();
  return id ? effectiveEq(id) : getGlobalEq();
};

export const hasOwnEq = () => Boolean(editingId() && getTrackEq(editingId()));

function write(eq) {
  const id = editingId();
  if (id) setTrackEq(id, eq);
  else setGlobalEq(eq);
  changed();
}

export function setBand(index, db) {
  const bands = [...shownEq().bands];
  if (bands[index] === db) return;
  bands[index] = db;
  write({ preset: 'custom', bands });
}

export const pickPreset = (id) => write({ preset: id, bands: [...presetBands(id)] });

export const resetBands = () => pickPreset('flat');

// У трека снова общая настройка.
export function useGlobalEq() {
  clearTrackEq(editingId());
  changed();
}
