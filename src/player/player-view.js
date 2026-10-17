import { onNavigate } from '../app/router.js';
import { createPainter } from './player-paint.js';
import { createParts } from './player-parts.js';

// Экран плеера: части собираются один раз и перерисовываются по событиям.
export function renderPlayer() {
  const parts = createParts();
  const { paint, tick, repaintIfDirty } = createPainter(parts);

  ['playerchange', 'langchange', 'librarychange', 'skipchange', 'modechange', 'markschange', 'speedchange', 'sleepchange'].forEach((name) =>
    document.addEventListener(name, paint),
  );
  document.addEventListener('playertick', tick);
  onNavigate((id) => id === 'player' && repaintIfDirty());
  paint();
  return parts.root;
}
