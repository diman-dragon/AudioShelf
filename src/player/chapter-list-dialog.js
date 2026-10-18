import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { t } from '../i18n/i18n.js';
import { kindOf } from '../library/file-info.js';
import { closeModal, openModal } from '../ui/modal.js';
import { chapterIndex, playChapter } from './chapter-actions.js';
import { createChapterRow } from './chapter-row.js';
import { getDurations } from './durations.js';
import { getTrack } from './player-state.js';

export function openChapterList() {
  const book = getTrack();
  if (!book) return;
  const known = getDurations(book.id);
  const current = chapterIndex();
  const list = el('div', 'clist');
  list.append(...book.files.map((file, i) => createChapterRow(file, i, {
    current: i === current,
    duration: known[i],
    onPick: (index) => {
      closeModal();
      playChapter(index);
    },
  })));
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn ghost', closeModal), 'marks.close'));
  openModal(t(kindOf(book) === 'album' ? 'list.tracks' : 'list.chapters'), [list, actions]);
  list.querySelector('.cur')?.scrollIntoView({ block: 'center' });
}
