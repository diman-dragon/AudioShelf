import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { closeModal, openModal } from '../ui/modal.js';
import { splitName } from '../library/format.js';
import { t } from '../i18n/i18n.js';
import { chapterIndex, playChapter } from './chapter-actions.js';
import { getTrack } from './player-state.js';

// Список глав книги: текущая подсвечена, нажатие запускает главу.
export function openChaptersDialog() {
  const current = chapterIndex();
  const list = el('div', 'chapters');
  getTrack().files.forEach((file, i) => {
    const row = button(`chapter${i === current ? ' on' : ''}`, () => {
      closeModal();
      playChapter(i);
    });
    row.textContent = `${i + 1}. ${splitName(file.name)[0]}`;
    list.append(row);
  });
  const actions = el('div', 'modal-actions');
  actions.append(bindText(button('btn ghost', closeModal), 'lib.cancel'));
  openModal(t('player.chapters'), [list, actions]);
}
