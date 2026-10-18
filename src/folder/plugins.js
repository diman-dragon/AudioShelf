import { registerPlugin } from '@capacitor/core';
import { mock } from './mock.js';
import { mockChapters, mockEnergy, mockPlayer, mockResume, mockSleep } from '../player/mock-player.js';
import { mockBilling } from '../billing/mock-billing.js';
import { mockLicense } from '../billing/mock-license.js';
import { mockLoudness, mockSound } from '../sound/mock-sound.js';

const web = () => mock;

export const Folder = registerPlugin('Folder', { web });
export const FileOps = registerPlugin('FileOps', { web });
export const Cover = registerPlugin('Cover', { web });
export const Player = registerPlugin('Player', { web: () => mockPlayer });
export const Sound = registerPlugin('Sound', { web: () => mockSound });
export const Loudness = registerPlugin('Loudness', { web: () => mockLoudness });
export const Sleep = registerPlugin('Sleep', { web: () => mockSleep });
export const Billing = registerPlugin('Billing', { web: () => mockBilling });
export const Resume = registerPlugin('Resume', { web: () => mockResume });
export const Energy = registerPlugin('Energy', { web: () => mockEnergy });
export const License = registerPlugin('License', { web: () => mockLicense });
export const Chapters = registerPlugin('Chapters', { web: () => mockChapters });
