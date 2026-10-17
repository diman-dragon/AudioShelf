import { getScreen } from '../app/router.js';
import { kindOf } from '../library/file-info.js';
import { bookProgress } from './book-progress.js';
import { getMarks } from './bookmark-store.js';
import { chapterIndex } from './chapter-actions.js';
import { headingOf } from './heading.js';
import { loadSpeed } from './last-book.js';
import { getMode } from './mode-pref.js';
import { getStatus, getTrack, isLoaded } from './player-state.js';
import { resumeAt } from './progress-store.js';
import { getSleepChoice } from './sleep-actions.js';
import { getSkip } from './skip-pref.js';

// Рисует экран плеера. paint — целиком, когда изменилось что-то заметное (глава, пауза/игра, закладки, язык...);
// tick — только время: бегунок, общий прогресс, остаток сна. Пока экран не открыт, не рисуем вовсе.
export function createPainter({ body, empty, cover, title, byline, chapter, seek, line, controls, extras }) {
  let shown = null;
  let dirty = true;
  const visible = () => getScreen() === 'player';

  const timePart = (book, index) => {
    const loaded = isLoaded();
    const s = getStatus();
    const position = loaded ? s.position : resumeAt(book.id).p;
    seek.update(position, loaded ? s.duration : 0);
    line.update(kindOf(book) === 'book' ? bookProgress(book.id, book.files.length, index, position) : null); // общий прогресс — только у книги
    extras.updateSleep({ choice: getSleepChoice(), left: s.sleepLeft, end: s.sleepEnd });
  };

  const paint = () => {
    if (!visible()) {
      dirty = true;
      return;
    }
    dirty = false;
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
    const s = getStatus();
    const index = chapterIndex();
    const head = headingOf(book, index);
    title.textContent = head.title;
    byline.textContent = head.sub;
    byline.hidden = !head.sub;
    chapter.textContent = head.label;
    chapter.hidden = !head.label;
    cover.refresh(book);
    timePart(book, index);
    controls.update({ playing: isLoaded() && s.playing, multi, skip: getSkip() });
    extras.update({
      markCount: getMarks(book.id).length,
      mode: getMode(kind),
      multi,
      speed: loadSpeed(),
      sleep: { choice: getSleepChoice(), left: s.sleepLeft, end: s.sleepEnd },
    });
  };

  const tick = () => {
    const book = getTrack();
    if (!book) return;
    if (!visible() || dirty) return paint(); // paint сам запомнит, что экран надо дорисовать

    timePart(book, chapterIndex());
  };

  return { paint, tick, repaintIfDirty: () => dirty && paint() };
}
