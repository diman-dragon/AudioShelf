import './styles/index.css';
import { initLanguage } from './i18n/language.js';
import { initTheme } from './theme/theme.js';
import { mountApp } from './app/app.js';
import { initBack } from './app/back.js';
import { initLibrarySync } from './library/library-sync.js';
import { initPlayer } from './player/player-state.js';

initLanguage();
initTheme();
mountApp(document.getElementById('app'));
initBack();
initLibrarySync();
initPlayer();
