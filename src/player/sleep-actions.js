import { Sleep } from '../folder/plugins.js';

const SLEEP_OPTIONS = ['off', 15, 30, 45, 60, 'chapter'];

let choice = 'off';
let armedAt = 0;
const GRACE = 1500;

export const getSleepChoice = () => choice;

const notify = () => document.dispatchEvent(new Event('sleepchange'));

function setSleepChoice(id) {
  choice = id;
  armedAt = Date.now();
  notify();
  return Sleep.setSleep({ minutes: typeof id === 'number' ? id : 0, chapter: id === 'chapter' }).catch(() => {});
}

export function syncSleep(status) {
  if (choice !== 'off' && Date.now() - armedAt > GRACE && !(status.sleepLeft > 0 || status.sleepEnd)) {
    choice = 'off';
    notify();
  }
}

export const cycleSleep = () => setSleepChoice(SLEEP_OPTIONS[(SLEEP_OPTIONS.indexOf(choice) + 1) % SLEEP_OPTIONS.length]);
