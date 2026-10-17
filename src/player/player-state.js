import { noteDuration } from './durations.js';
import { expandBook } from './expand.js';
import { loadLast } from './last-book.js';
import { remember } from './remember.js';
import { setAnchor } from './status-anchor.js';
import { diffKind } from './status-diff.js';
import { syncSleep } from './sleep-actions.js';

// Что играет (книга) и в каком состоянии. Состояние приходит от нативного плеера (опрос и события — status-poll.js).
const EMPTY = { loaded: false, uri: '', index: 0, count: 0, playing: false, loading: false, ended: false, position: 0, duration: 0, speed: 1 };

const saved = loadLast();
let track = expandBook(saved?.files ? saved : null);
let status = EMPTY;
const JUMP_PLAYING = 5000; // мс: при игре позиция меняется на ~1 с за шаг, больше — перемотка
const JUMP_PAUSED = 500;

const announce = (name) => document.dispatchEvent(new Event(name));

export const getTrack = () => track;
export const getStatus = () => status;

// Плеер держит именно эту книгу: тот же файл и то же число глав (главы внутри файла считаются отдельно).
export const isLoaded = () => Boolean(track) && status.loaded && status.count === track.files.length && track.files.some((f) => f.uri === status.uri);

export function setTrack(book) {
  track = expandBook(book);
  announce('trackchange');
  announce('playerchange');
}

// Новое состояние. 'playerchange' — изменилось что-то заметное, 'playertick' — только время.
// synthetic — состояние посчитано здесь по времени (status-ticker.js), а не пришло от плеера.
// Прогресс запоминаем только по событиям: остановка, перемотка, другая глава (во время игры место пишет сам нативный плеер).
export function applyStatus(next, synthetic = false) {
  const prev = status;
  const wasLoaded = isLoaded();
  status = next;
  if (!synthetic) setAnchor(next);
  syncSleep(next);
  if (wasLoaded && next.loaded && next.duration > 0) noteDuration(track.id, next.index, next.duration);
  const stopped = prev.playing && !next.playing;
  const jumped = Math.abs(next.position - prev.position) > (next.playing ? JUMP_PLAYING : JUMP_PAUSED); // подвинули ползунок
  const moved = prev.loaded && next.loaded && prev.index !== next.index;
  if (wasLoaded && (stopped || jumped || moved)) remember(track, prev, next);
  const kind = diffKind(prev, next);
  if (kind !== 'none') announce(kind === 'tick' ? 'playertick' : 'playerchange');
}
