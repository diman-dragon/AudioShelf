import { Player } from '../folder/plugins.js';
import { resumeAt } from './progress-store.js';
import { load } from './player-load.js';
import { applyStatus, getStatus, getTrack, isLoaded } from './player-state.js';

// Перейти к главе (номер с нуля). Если плеер ещё не загружен — загружаем и сразу играем.
export async function playChapter(index) {
  if (!isLoaded()) return load(index, 0, true);
  applyStatus(await Player.skipTo({ index }));
  if (!getStatus().playing) applyStatus(await Player.play());
}

export const stepChapter = (step) => playChapter(Math.max(0, Math.min(getTrack().files.length - 1, chapterIndex() + step)));

// Номер текущей главы: у загруженной — из плеера, иначе — где остановились.
export const chapterIndex = () => (isLoaded() ? getStatus().index : resumeAt(getTrack().id).i);
