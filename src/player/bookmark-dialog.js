import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { closeModal, openModal } from '../ui/modal.js';
import { jumpToMark, markHere } from './bookmark-actions.js';
import { createMarkRow } from './bookmark-row.js';
import { getMarks, removeMark } from './bookmark-store.js';
import { getTrack, isLoaded } from './player-state.js';

export function openMarksDialog() {
  const book = getTrack();
  if (!book) return;
  const list = el('div', 'mlist');
  const add = bindText(button('btn', () => {
    markHere();
    paint();
  }), 'marks.add');
  const hint = bindText(el('span', 'field-hint'), 'marks.addHint');
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn ghost', closeModal), 'marks.close'));

  function paint() {
    const marks = getMarks(book.id);
    add.disabled = !isLoaded();
    hint.hidden = isLoaded();
    list.replaceChildren(...(marks.length
      ? marks.map((m) => createMarkRow(book, m, {
          onJump: (mark) => {
            closeModal();
            jumpToMark(mark);
          },
          onRemove: (mark) => {
            removeMark(book.id, mark.t);
            document.dispatchEvent(new Event('markschange'));
            paint();
          },
        }))
      : [bindText(el('p', 'muted'), 'marks.empty')]));
  }
  paint();
  openModal(t('marks.title'), [add, hint, list, actions]);
}
