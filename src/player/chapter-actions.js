import { Player } from '../folder/plugins.js';
import { resumeAt } from './progress-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded } from './player-state.js';

export async function playChapter(index) {
  if (!isLoaded()) return load(index, 0, true);
  applyStatus(await Player.skipTo({ index }));
  if (!getStatus().playing) applyStatus(await Player.play());
}

export async function stepChapter(step) {
  if (!isLoaded()) return playChapter(Math.max(0, Math.min(getTrack().files.length - 1, chapterIndex() + step)));
  applyStatus(await (step > 0 ? Player.next() : Player.previous()));
  if (!getStatus().playing) applyStatus(await Player.play());
}

export const chapterIndex = () => (isLoaded() ? getStatus().index : resumeAt(getTrack().id).i);
