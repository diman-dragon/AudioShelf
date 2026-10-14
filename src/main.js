import './styles/index.css';
import { initLanguage } from './i18n/language.js';
import { initTheme } from './theme/theme.js';
import { mountApp } from './app/app.js';
import { initBack } from './app/back.js';
import { initLibrarySync } from './library/library-sync.js';
import { initPlayer } from './player/player-state.js';
import { initSoundSync } from './sound/sound-sync.js';

initLanguage();
initTheme();
mountApp(document.getElementById('app'));
initBack();
initLibrarySync();
initPlayer();
initSoundSync();
