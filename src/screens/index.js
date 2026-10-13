import { renderLibrary } from './library.js';
import { renderPlayer } from './player.js';
import { renderSettings } from './settings.js';
import libraryIcon from '../assets/icons/library.webp';
import playerIcon from '../assets/icons/player.webp';
import settingsIcon from '../assets/icons/settings.webp';

// Единый реестр экранов: из него строятся вкладки и заголовок.
// key — ключ перевода названия. Порядок = порядок вкладок = порядок свайпов.
export const SCREENS = [
  { id: 'library', icon: libraryIcon, key: 'nav.library', render: renderLibrary },
  { id: 'player', icon: playerIcon, key: 'nav.player', render: renderPlayer },
  { id: 'settings', icon: settingsIcon, key: 'nav.settings', render: renderSettings },
];
