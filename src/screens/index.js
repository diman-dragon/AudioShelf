import { renderLibrary } from './library.js';
import { renderPlayer } from './player.js';
import { renderSettings } from './settings.js';
import libraryIcon from '../assets/icons/library.webp';
import playerIcon from '../assets/icons/player.webp';
import settingsIcon from '../assets/icons/settings.webp';

// Единый реестр экранов: из него строятся вкладки, заголовок и акцентный цвет.
// Порядок здесь = порядок вкладок = порядок свайпов.
export const SCREENS = [
  { id: 'library', icon: libraryIcon, label: 'Библиотека', title: 'Библиотека', accent: '#f0a64b', render: renderLibrary },
  { id: 'player', icon: playerIcon, label: 'Плеер', title: 'Плеер', accent: '#f5c84c', render: renderPlayer },
  { id: 'settings', icon: settingsIcon, label: 'Настройки', title: 'Настройки', accent: '#e08a5c', render: renderSettings },
];
