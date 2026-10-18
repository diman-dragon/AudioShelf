import { applyStatus, getStatus } from './player-state.js';
import { getAnchor } from './status-anchor.js';
import { needsSync, project } from './status-project.js';

const STEP_MS = 1000;
const SYNC_EVERY = 10;

let timer = 0;
let steps = 0;

export function initTicker({ sync, isHeld }) {
  const step = () => {
    const { status, at } = getAnchor();
    if (isHeld() || !status) return;
    const projected = project(status, performance.now() - at);
    applyStatus(projected, true);
    if (needsSync(status, projected) || ++steps % SYNC_EVERY === 0) sync();
  };

  const update = () => {
    const run = getStatus().playing && !document.hidden;
    if (run && !timer) {
      steps = 0;
      timer = setInterval(step, STEP_MS);
    } else if (!run && timer) {
      clearInterval(timer);
      timer = 0;
    }
  };

  document.addEventListener('playerchange', update);
  document.addEventListener('visibilitychange', update);
  update();
}
