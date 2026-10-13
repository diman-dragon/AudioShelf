import { el, button } from '../core/dom.js';
import { bindText } from '../i18n/bind.js';
import { createSegmented } from '../ui/segmented.js';
import { closeModal, openModal } from '../ui/modal.js';
import { t } from '../i18n/i18n.js';
import { saveFile } from './save-file.js';
import { kindOf } from './file-info.js';
import { createField, textInput } from './fields.js';
import { getMeta } from './meta-store.js';
import { splitName } from './format.js';

const KINDS = [
  { id: 'book', labelKey: 'kind.book' },
  { id: 'album', labelKey: 'kind.album' },
];

// Окно «Изменить»: тип, теги и имя файла. У каждого поля подсказка, где оно будет видно.
export function openEditDialog(file) {
  const meta = getMeta(file.uri);
  const [base, ext] = splitName(file.name);
  let kind = kindOf(file);

  const title = textInput(meta.title, base);
  const author = textInput(meta.author);
  const narrator = textInput(meta.narrator);
  const year = textInput(meta.year);
  year.inputMode = 'numeric';
  year.maxLength = 4;
  const name = textInput(base);

  const type = createField('edit.type', 'edit.typeHint', createSegmented(KINDS, () => kind, (id) => {
    kind = id;
    sync();
  }));
  const fTitle = createField('edit.title', 'edit.titleHint', title);
  const fAuthor = createField('edit.authorBook', 'edit.authorHint', author);
  const fNarrator = createField('edit.narrator', 'edit.narratorHint', narrator);
  const fYear = createField('edit.year', 'edit.yearHint', year);
  const fFile = createField('edit.file', 'edit.fileHint', name, { ext });

  // Тип меняет подпись «Автор / Исполнитель» и показывает «Чтеца» только у книги.
  function sync() {
    bindText(fAuthor.label, kind === 'book' ? 'edit.authorBook' : 'edit.authorAlbum');
    fNarrator.node.hidden = kind !== 'book';
  }
  sync();

  const save = bindText(button('btn', () => {
    saveFile(file, { kind, title: title.value.trim(), author: author.value.trim(), narrator: kind === 'book' ? narrator.value.trim() : '', year: year.value.trim() }, name.value.trim());
    closeModal();
  }), 'edit.save');
  const cancel = bindText(button('btn ghost', closeModal), 'lib.cancel');
  const actions = el('div', 'modal-actions');
  actions.append(cancel, save);

  openModal(t('edit.heading'), [type.node, fTitle.node, fAuthor.node, fNarrator.node, fYear.node, fFile.node, bindText(el('p', 'note'), 'edit.note'), actions]);
}
