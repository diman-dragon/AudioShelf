import { Player } from '../folder/plugins.js';
import { authorOf, titleOf } from '../library/file-info.js';
import { splitName } from '../library/format.js';
import { applyProfile } from './apply-profile.js';
import { applySound } from '../sound/apply-sound.js';
import { loadSpeed } from './progress-store.js';
import { applyStatus, getTrack } from './player-state.js';

// Загружает книгу в нативный плеер: главу index с позиции position.
export async function load(index, position, autoplay) {
  const book = getTrack();
  const state = await Player.load({
    id: book.id,
    items: book.files.map((f) => ({ uri: f.uri, title: splitName(f.name)[0] })),
    index,
    position,
    autoplay,
    title: titleOf(book),
    artist: authorOf(book),
  });
  await applyProfile(book);
  await applySound(book);
  applyStatus(await Player.setSpeed({ speed: loadSpeed() }).catch(() => state));
}
