import { el } from '../core/dom.js';
import { bylineOf, titleOf } from '../library/file-info.js';
import { chapterLabel } from './chapter-label.js';
import { createControls } from './controls.js';
import { createCover } from './cover-box.js';
import { createPlayerEmpty } from './player-empty.js';
import { chapterIndex, stepChapter } from './chapter-actions.js';
import { seekTo, skip, togglePlay } from './player-actions.js';
import { getStatus, getTrack, isLoaded } from './player-state.js';
import { resumeAt } from './progress-store.js';
import { createSeek } from './seek-bar.js';
import { getSkip } from './skip-pref.js';

export function renderPlayer() {
  const root = el('section', 'player');
  const body = el('div', 'pbody');
  const cover = createCover();
  const title = el('div', 'ptitle');
  const byline = el('div', 'psub');
  const chapter = el('div', 'psub');
  const seek = createSeek(seekTo);
  const controls = createControls({
    onPrev: () => stepChapter(-1),
    onBack: () => skip(false),
    onToggle: togglePlay,
    onForward: () => skip(true),
    onNext: () => stepChapter(1),
  });
  body.append(cover.node, title, byline, chapter, seek.node, controls.node);
  const empty = createPlayerEmpty();
  root.append(body, empty);

  let shown = null;
  const paint = () => {
    const book = getTrack();
    body.hidden = !book;
    empty.hidden = Boolean(book);
    if (!book) return;
    if (shown !== book.id) {
      shown = book.id;
      cover.show(book);
    }
    const multi = book.files.length > 1;
    const loaded = isLoaded();
    const s = getStatus();
    title.textContent = titleOf(book);
    byline.textContent = bylineOf(book);
    byline.hidden = !byline.textContent;
    const index = chapterIndex();
    chapter.textContent = multi ? chapterLabel(book, index) : '';
    chapter.hidden = !multi;
    cover.refresh(book);
    seek.update(loaded ? s.position : resumeAt(book.id).p, loaded ? s.duration : 0);
    controls.update({ playing: loaded && s.playing, multi, skip: getSkip() });
  };

  document.addEventListener('playerchange', paint);
  document.addEventListener('langchange', paint);
  document.addEventListener('librarychange', paint);
  document.addEventListener('skipchange', paint);
  paint();
  return root;
}
