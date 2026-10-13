import { el } from '../core/dom.js';
import { bylineOf, titleOf } from '../library/file-info.js';
import { createControls } from './controls.js';
import { createCover } from './cover-box.js';
import { createPlayerEmpty } from './player-empty.js';
import { cycleSpeed, seekTo, skip, togglePlay } from './player-actions.js';
import { getStatus, getTrack, isLoaded } from './player-state.js';
import { loadPos, loadSpeed } from './progress-store.js';
import { createSeek } from './seek-bar.js';

export function renderPlayer() {
  const root = el('section', 'player');
  const body = el('div', 'pbody');
  const cover = createCover();
  const title = el('div', 'ptitle');
  const byline = el('div', 'psub');
  const seek = createSeek(seekTo);
  const controls = createControls({
    onBack: () => skip(false),
    onToggle: togglePlay,
    onForward: () => skip(true),
    onSpeed: cycleSpeed,
  });
  body.append(cover.node, title, byline, seek.node, controls.node);
  const empty = createPlayerEmpty();
  root.append(body, empty);

  let shown = null;
  const paint = () => {
    const track = getTrack();
    body.hidden = !track;
    empty.hidden = Boolean(track);
    if (!track) return;
    if (shown !== track.uri) {
      shown = track.uri;
      cover.show(track);
    }
    title.textContent = titleOf(track);
    byline.textContent = bylineOf(track);
    byline.hidden = !byline.textContent;
    cover.refresh(track);
    const s = getStatus();
    const loaded = isLoaded();
    seek.update(loaded ? s.position : loadPos(track.uri), loaded ? s.duration : 0);
    controls.update(loaded && s.playing, loaded ? s.speed : loadSpeed());
  };

  document.addEventListener('playerchange', paint);
  document.addEventListener('langchange', paint);
  document.addEventListener('librarychange', paint);
  paint();
  return root;
}
