import './styles/index.css';
import { initStorage } from './core/storage.js';

// Сначала база данных загружается в память (модули читают настройки уже при подключении), потом запускается приложение.
initStorage().finally(() => import('./start.js'));
