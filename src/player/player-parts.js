import { el } from '../core/dom.js';
import { openMarksDialog } from './bookmark-dialog.js';
import { openChapterList } from './chapter-list-dialog.js';
import { stepChapter } from './chapter-actions.js';
import { createBookLine } from './book-line.js';
import { createControls } from './controls.js';
import { createCover } from './cover-box.js';
import { createExtras } from './extras-row.js';
import { cycleMode } from './mode-actions.js';
import { loadSpeed } from './last-book.js';
import { seekTo, setSpeed, skip, togglePlay } from './player-actions.js';
import { createPlayerEmpty } from './player-empty.js';
import { createSeek } from './seek-bar.js';
import { cycleSleep } from './sleep-actions.js';
import { nextSpeed } from './speeds.js';

export function createParts() {
  const root = el('section', 'player');
  const body = el('div', 'pbody');
  const parts = {
    root,
    body,
    cover: createCover(),
    title: el('div', 'ptitle'),
    byline: el('div', 'psub'),
    chapter: el('div', 'psub'),
    seek: createSeek(seekTo),
    line: createBookLine(),
    empty: createPlayerEmpty(),
  };
  parts.controls = createControls({
    onPrev: () => stepChapter(-1),
    onBack: () => skip(false),
    onToggle: togglePlay,
    onForward: () => skip(true),
    onNext: () => stepChapter(1),
  });
  parts.extras = createExtras({
    onMarks: openMarksDialog,
    onList: openChapterList,
    onMode: cycleMode,
    onSpeed: () => setSpeed(nextSpeed(loadSpeed())),
    onSleep: cycleSleep,
  });
  body.append(parts.cover.node, parts.title, parts.byline, parts.chapter, parts.seek.node, parts.line.node, parts.controls.node, parts.extras.node);
  root.append(body, parts.empty);
  return parts;
}
