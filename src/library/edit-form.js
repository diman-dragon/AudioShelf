import { bindText } from '../i18n/bind.js';
import { createSegmented } from '../ui/segmented.js';
import { kindOf } from './file-info.js';
import { createField, textInput } from './fields.js';
import { getMeta } from './meta-store.js';
import { splitName } from './format.js';

const KINDS = [
  { id: 'book', labelKey: 'kind.book' },
  { id: 'album', labelKey: 'kind.album' },
];

// Поля окна «Изменить»; у каждого подсказка, где оно будет видно.
export function createEditForm(book) {
  const meta = getMeta(book.id);
  const [base, ext] = splitName(book.files[0].name);
  let kind = kindOf(book);

  const title = textInput(meta.title, book.name);
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
  const fAuthor = createField('edit.authorBook', 'edit.authorHint', author);
  const fNarrator = createField('edit.narrator', 'edit.narratorHint', narrator);

  // Тип меняет подпись «Автор / Исполнитель» и показывает «Чтеца» только у книги.
  function sync() {
    bindText(fAuthor.label, kind === 'book' ? 'edit.authorBook' : 'edit.authorAlbum');
    fNarrator.node.hidden = kind !== 'book';
  }
  sync();

  return {
    nodes: [
      type.node,
      createField('edit.title', 'edit.titleHint', title).node,
      fAuthor.node,
      fNarrator.node,
      createField('edit.year', 'edit.yearHint', year).node,
      ...(book.folder ? [] : [createField('edit.file', 'edit.fileHint', name, { ext }).node]), // папку книги не переименовываем
    ],
    meta: () => ({
      kind,
      title: title.value.trim(),
      author: author.value.trim(),
      narrator: kind === 'book' ? narrator.value.trim() : '',
      year: year.value.trim(),
    }),
    fileName: () => name.value.trim(),
  };
}
