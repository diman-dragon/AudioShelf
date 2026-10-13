import { registerPlugin } from '@capacitor/core';
import { mock } from './mock.js';
import { mockPlayer } from '../player/mock-player.js';

// Нативные плагины Android (Java, см. android/.../com/audioshelf/app).
// В браузере вместо них работает заглушка mock.js.
const web = () => mock;

export const Folder = registerPlugin('Folder', { web }); // pickFolder, scan, cancelScan, событие 'scan'
export const FileOps = registerPlugin('FileOps', { web }); // add, remove, rename, open
export const Cover = registerPlugin('Cover', { web }); // read
export const Player = registerPlugin('Player', { web: () => mockPlayer }); // load, play, pause, seek, setSpeed, state
