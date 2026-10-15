import { registerPlugin } from '@capacitor/core';
import { mock } from './mock.js';
import { mockEnergy, mockPlayer, mockResume, mockSleep } from '../player/mock-player.js';
import { mockBilling } from '../billing/mock-billing.js';
import { mockLoudness, mockSound } from '../sound/mock-sound.js';

// Нативные плагины Android (Java, см. android/.../com/audioshelf/app).
// В браузере вместо них работает заглушка mock.js.
const web = () => mock;

export const Folder = registerPlugin('Folder', { web }); // pickFolder, scan, cancelScan, событие 'scan'
export const FileOps = registerPlugin('FileOps', { web }); // add, remove, rename, open
export const Cover = registerPlugin('Cover', { web }); // read
export const Player = registerPlugin('Player', { web: () => mockPlayer }); // load, play, pause, seek, setSpeed, setProfile, next, previous, state
export const Sound = registerPlugin('Sound', { web: () => mockSound }); // setSound: эквалайзер и поправки громкости
export const Loudness = registerPlugin('Loudness', { web: () => mockLoudness }); // start, cancel, событие 'loudness'
export const Sleep = registerPlugin('Sleep', { web: () => mockSleep }); // setSleep: таймер сна
export const Billing = registerPlugin('Billing', { web: () => mockBilling }); // getState, buy, событие 'billing': Google Play Billing
export const Resume = registerPlugin('Resume', { web: () => mockResume }); // saved: место, которое запомнил нативный плеер
export const Energy = registerPlugin('Energy', { web: () => mockEnergy }); // setOffload: аппаратное воспроизведение
