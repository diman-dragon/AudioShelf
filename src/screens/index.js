import { renderLibrary } from '../library/library-view.js';
import { renderPlayer } from '../player/player-view.js';
import { renderSettings } from './settings.js';
import { renderSound } from './sound.js';
import libraryEmoji from '../assets/emoji/1f4da.svg';
import playerEmoji from '../assets/emoji/1f3a7.svg';
import soundEmoji from '../assets/emoji/1f50a.svg';
import settingsEmoji from '../assets/emoji/2699.svg';

// Единый реестр экранов: из него строятся вкладки и заголовок.
// key — ключ перевода названия. Порядок = порядок вкладок = порядок свайпов.
export const SCREENS = [
  { id: 'library', emoji: libraryEmoji, key: 'nav.library', render: renderLibrary },
  { id: 'player', emoji: playerEmoji, key: 'nav.player', render: renderPlayer },
  { id: 'sound', emoji: soundEmoji, key: 'nav.sound', render: renderSound },
  { id: 'settings', emoji: settingsEmoji, key: 'nav.settings', render: renderSettings },
];
