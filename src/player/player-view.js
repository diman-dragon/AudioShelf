import { el } from '../core/dom.js';
import { kindOf } from '../library/file-info.js';
import { bookProgress } from './book-progress.js';
import { createBookLine } from './book-line.js';
import { openMarksDialog } from './bookmark-dialog.js';
import { getMarks } from './bookmark-store.js';
import { openChapterList } from './chapter-list-dialog.js';
import { chapterIndex, stepChapter } from './chapter-actions.js';
import { createControls } from './controls.js';
import { createCover } from './cover-box.js';
import { createExtras } from './extras-row.js';
import { headingOf } from './heading.js';
import { cycleMode } from './mode-actions.js';
import { getMode } from './mode-pref.js';
import { seekTo, setSpeed, skip, togglePlay } from './player-actions.js';
import { createPlayerEmpty } from './player-empty.js';
import { getStatus, getTrack, isLoaded } from './player-state.js';
import { loadSpeed, resumeAt } from './progress-store.js';
import { createSeek } from './seek-bar.js';
import { cycleSleep, getSleepChoice } from './sleep-actions.js';
import { getSkip } from './skip-pref.js';
import { nextSpeed } from './speeds.js';

export function renderPlayer() {
  const root = el('section', 'player');
  const body = el('div', 'pbody');
  const cover = createCover();
  const title = el('div', 'ptitle');
  const byline = el('div', 'psub');
  const chapter = el('div', 'psub');
  const seek = createSeek(seekTo);
  const line = createBookLine();
  const controls = createControls({
    onPrev: () => stepChapter(-1),
    onBack: () => skip(false),
    onToggle: togglePlay,
    onForward: () => skip(true),
    onNext: () => stepChapter(1),
  });
  const extras = createExtras({
    onMarks: openMarksDialog,
    onList: openChapterList,
    onMode: cycleMode,
    onSpeed: () => setSpeed(nextSpeed(loadSpeed())),
    onSleep: cycleSleep,
  });
  body.append(cover.node, title, byline, chapter, seek.node, line.node, controls.node, extras.node);
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
    const kind = kindOf(book);
    const loaded = isLoaded();
    const s = getStatus();
    const index = chapterIndex();
    const head = headingOf(book, index);
    title.textContent = head.title;
    byline.textContent = head.sub;
    byline.hidden = !head.sub;
    chapter.textContent = head.label;
    chapter.hidden = !head.label;
    cover.refresh(book);
    const position = loaded ? s.position : resumeAt(book.id).p;
    seek.update(position, loaded ? s.duration : 0);
    line.update(kind === 'book' ? bookProgress(book.id, book.files.length, index, position) : null); // общий прогресс — только у книги
    controls.update({ playing: loaded && s.playing, multi, skip: getSkip() });
    extras.update({
      markCount: getMarks(book.id).length,
      mode: getMode(kind),
      multi,
      speed: loadSpeed(),
      sleep: { choice: getSleepChoice(), left: s.sleepLeft, end: s.sleepEnd },
    });
  };

  ['playerchange', 'langchange', 'librarychange', 'skipchange', 'modechange', 'markschange', 'speedchange', 'sleepchange'].forEach((name) =>
    document.addEventListener(name, paint),
  );
  paint();
  return root;
}
